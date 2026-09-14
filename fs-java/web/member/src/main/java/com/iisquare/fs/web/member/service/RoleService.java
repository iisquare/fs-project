package com.iisquare.fs.web.member.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.jpa.mvc.JPAServiceBase;
import com.iisquare.fs.web.member.dao.ApplicationDao;
import com.iisquare.fs.web.member.dao.DataPermitDao;
import com.iisquare.fs.web.member.dao.MenuDao;
import com.iisquare.fs.web.member.dao.ResourceDao;
import com.iisquare.fs.web.member.dao.RoleApplicationDao;
import com.iisquare.fs.web.member.dao.RoleDao;
import com.iisquare.fs.web.member.dao.RoleMenuDao;
import com.iisquare.fs.web.member.dao.RoleResourceDao;
import com.iisquare.fs.web.member.dao.UserRoleDao;
import com.iisquare.fs.web.member.entity.Menu;
import com.iisquare.fs.web.member.entity.Resource;
import com.iisquare.fs.web.member.entity.Role;
import com.iisquare.fs.web.member.entity.RoleApplication;
import com.iisquare.fs.web.member.entity.RoleMenu;
import com.iisquare.fs.web.member.entity.RoleResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;

@Service
public class RoleService extends JPAServiceBase {

    @Autowired
    RoleDao roleDao;
    @Autowired
    UserService userService;
    @Autowired
    RbacService rbacService;
    @Autowired
    ApplicationDao applicationDao;
    @Autowired
    MenuDao menuDao;
    @Autowired
    ResourceDao resourceDao;
    @Autowired
    UserRoleDao userRoleDao;
    @Autowired
    DataPermitDao dataPermitDao;
    @Autowired
    RoleApplicationDao roleApplicationDao;
    @Autowired
    RoleMenuDao roleMenuDao;
    @Autowired
    RoleResourceDao roleResourceDao;

    /**
     * 授权类型 -> 保存参数中的授权字段名，授权关系均维护在独立关联表中
     */
    public static final Map<String, String> PERMIT_TYPES = new LinkedHashMap<>();
    static {
        PERMIT_TYPES.put("application", "applicationIds");
        PERMIT_TYPES.put("menu", "menuIds");
        PERMIT_TYPES.put("resource", "resourceIds");
    }

    @Override
    public Map<String, String> sorts() {
        Map<String, String> sorts = new LinkedHashMap<>();
        sorts.put("id", "desc");
        sorts.put("status", "asc");
        sorts.put("sort", "desc");
        return sorts;
    }

    public Map<?, ?> status() {
        Map<Integer, String> status = new LinkedHashMap<>();
        status.put(1, "正常");
        status.put(2, "禁用");
        return status;
    }

    public ObjectNode infos(List<Integer> ids) {
        ObjectNode nodes = infoByIds(roleDao, ids);
        return (ObjectNode) filter(nodes);
    }

    @Override
    public JsonNode filter(JsonNode json) {
        for (JsonNode node : json) {
            ObjectNode item = (ObjectNode) node;
            item.retain("id", "name", "status");
        }
        return json;
    }

    public Role info(Integer id) {
        return info(roleDao, id);
    }

    public Map<String, Object> info(Map<?, ?> param) {
        int id = DPUtil.parseInt(param.get("id"));
        Role info = info(id);
        if (null == info) {
            return ApiUtil.result(0, null, DPUtil.objectNode());
        }
        return ApiUtil.result(0, null, info);
    }

    /**
     * 角色已授权的全部标识，直接读取关联表
     */
    public Set<Integer> permitIds(Integer roleId, String type) {
        return new TreeSet<>(permitIdList(roleId, type));
    }

    /**
     * 角色已授权的标识，菜单、资源仅返回指定应用范围内的授权
     */
    public Set<Integer> permitIds(Integer roleId, String type, Integer applicationId) {
        Set<Integer> result = permitIds(roleId, type);
        if (!"application".equals(type)) result.retainAll(idsInApplication(type, applicationId));
        return result;
    }

    private List<Integer> permitIdList(Integer roleId, String type) {
        if (null == roleId) return Collections.emptyList();
        switch (type) {
            case "application":
                return roleApplicationDao.findApplicationIdsByRoleId(roleId);
            case "menu":
                return roleMenuDao.findMenuIdsByRoleId(roleId);
            case "resource":
                return roleResourceDao.findResourceIdsByRoleId(roleId);
            default:
                return Collections.emptyList();
        }
    }

    /**
     * 指定应用下的菜单或资源标识，作为该应用的授权范围
     * 取值范围与权限分配树保持一致（该应用下全部节点，不按状态过滤），避免出现界面上不可见却被解除的授权
     */
    public Set<Integer> idsInApplication(String type, Integer applicationId) {
        if (DPUtil.parseInt(applicationId) < 1) return new TreeSet<>();
        switch (type) {
            case "menu":
                return DPUtil.values(menuDao.findAll((Specification<Menu>) (root, query, cb) ->
                        cb.equal(root.get("applicationId"), applicationId)), Integer.class, "id");
            case "resource":
                return DPUtil.values(resourceDao.findAll((Specification<Resource>) (root, query, cb) ->
                        cb.equal(root.get("applicationId"), applicationId)), Integer.class, "id");
            default:
                return new TreeSet<>();
        }
    }

    /**
     * 读取或更新角色授权
     * 菜单、资源不限状态均可授权，运行时鉴权仍只识别启用状态的节点
     *
     * @param bids 为空时表示读取当前授权；非空时表示将当前应用范围内的授权更新为该集合
     */
    @Transactional
    public Set<Integer> permit(Integer roleId, String type, Set<Integer> bids, Integer applicationId, int uid) {
        if (!PERMIT_TYPES.containsKey(type)) return new TreeSet<>();
        if (null == bids) return permitIds(roleId, type, applicationId);
        Role role = roleDao.findByIdForUpdate(roleId).orElse(null); // 悲观锁，串行化同一角色的授权变更
        if (null == role) return new TreeSet<>(); // 角色不存在
        Set<Integer> current = permitIds(roleId, type);
        // 应用授权为全量替换；菜单、资源按应用隔离范围，避免误删其它应用的授权
        Set<Integer> scope = "application".equals(type) ? null : idsInApplication(type, applicationId);
        Set<Integer> target = new TreeSet<>(bids);
        if (null != scope) target.retainAll(scope);
        updatePermit(roleId, type, current, target, scope, uid);
        rbacService.evictRolePermit(roleId); // 授权变更后清理角色资源缓存
        return permitIds(roleId, type, applicationId);
    }

    /**
     * 增量更新授权，仅写入新增与解除的部分，未改动的授权保持原样
     *
     * @param scope 为空表示当前授权全部参与比对，不为空时仅比对该范围内的授权
     */
    private void updatePermit(Integer roleId, String type, Set<Integer> current,
                              Set<Integer> target, Set<Integer> scope, int uid) {
        Set<Integer> currentScope = new TreeSet<>(current);
        if (null != scope) currentScope.retainAll(scope);
        Set<Integer> added = new TreeSet<>(target);
        added.removeAll(currentScope);
        Set<Integer> removed = new TreeSet<>(currentScope);
        removed.removeAll(target);
        if (added.isEmpty() && removed.isEmpty()) return; // 无变更时不做任何写入
        long time = System.currentTimeMillis();
        switch (type) {
            case "application": {
                if (!removed.isEmpty()) roleApplicationDao.deleteByRoleIdAndApplicationIdIn(roleId, removed);
                if (added.isEmpty()) break;
                List<RoleApplication> list = new ArrayList<>(added.size());
                for (Integer id : added) {
                    list.add(RoleApplication.builder()
                            .roleId(roleId).applicationId(id).createdTime(time).createdUid(uid).build());
                }
                roleApplicationDao.saveAll(list);
                break;
            }
            case "menu": {
                if (!removed.isEmpty()) roleMenuDao.deleteByRoleIdAndMenuIdIn(roleId, removed);
                if (added.isEmpty()) break;
                List<RoleMenu> list = new ArrayList<>(added.size());
                for (Integer id : added) {
                    list.add(RoleMenu.builder()
                            .roleId(roleId).menuId(id).createdTime(time).createdUid(uid).build());
                }
                roleMenuDao.saveAll(list);
                break;
            }
            case "resource": {
                if (!removed.isEmpty()) roleResourceDao.deleteByRoleIdAndResourceIdIn(roleId, removed);
                if (added.isEmpty()) break;
                List<RoleResource> list = new ArrayList<>(added.size());
                for (Integer id : added) {
                    list.add(RoleResource.builder()
                            .roleId(roleId).resourceId(id).createdTime(time).createdUid(uid).build());
                }
                roleResourceDao.saveAll(list);
                break;
            }
            default:
                break;
        }
    }

    /**
     * 按类型更新授权：应用授权为全量替换，菜单、资源按应用分组增量更新，
     * 仅比对各应用自身范围内的授权，避免误删其它应用的授权
     */
    private void updatePermits(Integer roleId, String type, Set<Integer> target, int uid) {
        Set<Integer> current = permitIds(roleId, type);
        if ("application".equals(type)) {
            updatePermit(roleId, type, current, target, null, uid);
            return;
        }
        for (Map.Entry<Integer, Set<Integer>> entry : groupIdsByApplication(type, target).entrySet()) {
            updatePermit(roleId, type, current, entry.getValue(), idsInApplication(type, entry.getKey()), uid);
        }
    }

    /**
     * 校验授权标识是否存在
     */
    private boolean existIds(String type, Set<Integer> ids) {
        long count = switch (type) {
            case "application" -> applicationDao.findAllById(ids).size();
            case "menu" -> menuDao.findAllById(ids).size();
            case "resource" -> resourceDao.findAllById(ids).size();
            default -> 0;
        };
        return count == ids.size();
    }

    /**
     * 按应用标识分组菜单或资源授权，供按应用范围增量更新
     */
    private Map<Integer, Set<Integer>> groupIdsByApplication(String type, Set<Integer> ids) {
        Map<Integer, Set<Integer>> result = new LinkedHashMap<>();
        if (ids.isEmpty()) return result;
        if ("menu".equals(type)) {
            for (Menu menu : menuDao.findAllById(ids)) {
                result.computeIfAbsent(menu.getApplicationId(), key -> new TreeSet<>()).add(menu.getId());
            }
        } else if ("resource".equals(type)) {
            for (Resource resource : resourceDao.findAllById(ids)) {
                result.computeIfAbsent(resource.getApplicationId(), key -> new TreeSet<>()).add(resource.getId());
            }
        }
        return result;
    }

    @Transactional
    public Map<String, Object> save(Map<?, ?> param, HttpServletRequest request) {
        int id = ValidateUtil.filterInteger(param.get("id"), 1, null, 0);
        String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
        if(DPUtil.empty(name)) return ApiUtil.result(1001, "名称异常", name);
        int status = DPUtil.parseInt(param.get("status"));
        if(!status().containsKey(status)) return ApiUtil.result(1002, "状态异常", status);
        Role info = null;
        if(id > 0) {
            if(!rbacService.hasPermit(request, "modify")) return ApiUtil.result(9403, null, null);
            info = info(id);
            if(null == info) return ApiUtil.result(404, null, id);
        } else {
            if(!rbacService.hasPermit(request, "add")) return ApiUtil.result(9403, null, null);
            info = new Role();
        }
        // 授权关系维护在独立关联表中，变更前需校验对应的操作权限
        if(null != info.getId()) roleDao.findByIdForUpdate(info.getId()); // 悲观锁，串行化同一角色的授权变更
        Map<String, Set<Integer>> permits = new LinkedHashMap<>();
        boolean denied = false;
        for (Map.Entry<String, String> entry : PERMIT_TYPES.entrySet()) {
            String type = entry.getKey();
            if(!param.containsKey(entry.getValue())) continue;
            Set<Integer> value = new TreeSet<>(DPUtil.parseIntList(param.get(entry.getValue())));
            if(null != info.getId() && value.equals(permitIds(info.getId(), type))) continue;
            if(!rbacService.hasPermit(request, type)) {
                denied = true;
                continue;
            }
            if(!value.isEmpty() && !existIds(type, value)) {
                return ApiUtil.result(1003, "授权标识不存在或已删除", entry.getValue());
            }
            permits.put(type, value);
        }
        info.setName(name);
        info.setSort(DPUtil.parseInt(param.get("sort")));
        info.setStatus(status);
        info.setDescription(DPUtil.parseString(param.get("description")));
        info = save(roleDao, info, rbacService.uid(request));
        for (Map.Entry<String, Set<Integer>> entry : permits.entrySet()) {
            updatePermits(info.getId(), entry.getKey(), entry.getValue(), rbacService.uid(request));
        }
        rbacService.evictRolePermit(info.getId()); // 角色变更后同步清理资源缓存
        if(denied) return ApiUtil.result(0, "角色保存成功，无授权操作权限", info);
        return ApiUtil.result(0, null, info);
    }

    public ObjectNode search(Map<String, Object> param, Map<?, ?> args) {
        ObjectNode result = search(roleDao, param, (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            Set<Integer> ids = new TreeSet<>(DPUtil.parseIntList(param.get("id"))); // 支持下拉选择器批量回显
            ids.removeIf(item -> item < 1);
            if (!ids.isEmpty()) predicates.add(root.get("id").in(ids));
            int status = DPUtil.parseInt(param.get("status"));
            if (!"".equals(DPUtil.parseString(param.get("status")))) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
            if (!DPUtil.empty(name)) {
                predicates.add(cb.like(root.get("name"), "%" + name + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        }, Sort.by(Sort.Order.desc("sort"), Sort.Order.desc("id")), sorts().keySet());
        JsonNode rows = ApiUtil.rows(result);
        if(!DPUtil.empty(args.get("withUserInfo"))) {
            userService.fillInfo(rows, "createdUid", "updatedUid");
        }
        if(!DPUtil.empty(args.get("withStatusText"))) {
            fillStatus(rows, status());
        }
        if(!DPUtil.empty(args.get("withApplications")) && !rows.isEmpty()) {
            fillApplicationIds(rows);
            fillInfos(applicationDao, rows, "applicationIds");
        }
        return result;
    }

    /**
     * 读取关联表，为角色列表补充授权应用标识
     */
    public JsonNode fillApplicationIds(JsonNode rows) {
        Set<Integer> ids = DPUtil.values(rows, Integer.class, "id");
        Map<Integer, Set<Integer>> map = new HashMap<>();
        if(!ids.isEmpty()) {
            for (RoleApplication item : roleApplicationDao.findAllByRoleIdIn(ids)) {
                map.computeIfAbsent(item.getRoleId(), key -> new TreeSet<>()).add(item.getApplicationId());
            }
        }
        for (JsonNode row : rows) {
            ObjectNode item = (ObjectNode) row;
            item.replace("applicationIds", DPUtil.toJSON(
                    map.getOrDefault(item.at("/id").asInt(), Collections.<Integer>emptySet())));
        }
        return rows;
    }

    @Transactional
    public boolean remove(List<Integer> ids) {
        if(null == ids || ids.isEmpty()) return false;
        for (Integer id : ids) {
            rbacService.evictRolePermit(id); // 角色变更后同步清理角色资源缓存
        }
        roleApplicationDao.deleteByRoleIdIn(ids);
        roleMenuDao.deleteByRoleIdIn(ids);
        roleResourceDao.deleteByRoleIdIn(ids);
        userRoleDao.deleteByRoleIdIn(ids);
        dataPermitDao.deleteByRoleIds(ids); // 清理该角色的数据行权限
        return remove(roleDao, ids);
    }

    public JsonNode fillInfo(JsonNode rows, String ...properties) {
        return fillInfo(roleDao, rows, properties);
    }

}
