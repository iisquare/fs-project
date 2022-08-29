package com.iisquare.fs.web.govern.service;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.jpa.util.JPAUtil;
import com.iisquare.fs.base.web.mvc.ServiceBase;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.govern.dao.StandardDao;
import com.iisquare.fs.web.govern.entity.Model;
import com.iisquare.fs.web.govern.entity.Standard;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.Predicate;
import javax.servlet.http.HttpServletRequest;
import java.util.*;

@Service
public class StandardService extends ServiceBase {

    @Autowired
    private StandardDao standardDao;
    @Autowired
    private DefaultRbacService rbacService;

    public Map<?, ?> molds() {
        Map<String, String> result = new LinkedHashMap<>();
        result.put("catalog", "目录"); // 目录结构
        result.put("column", "字段");
        return result;
    }

    public Map<?, ?> flags() {
        Map<String, String> result = new LinkedHashMap<>();
        result.put("unify", "统一编码");
        return result;
    }

    public Map<?, ?> levels() {
        Map<String, String> result = new LinkedHashMap<>();
        result.put("force", "强制");
        result.put("advise", "建议");
        result.put("ignore", "忽略");
        return result;
    }

    public Map<?, ?> status() {
        Map<Integer, String> status = new LinkedHashMap<>();
        status.put(1, "启用");
        status.put(2, "禁用");
        return status;
    }

    public Map<String, Object> search(Map<?, ?> param, Map<?, ?> config) {
        Map<String, Object> result = new LinkedHashMap<>();
        int page = ValidateUtil.filterInteger(param.get("page"), true, 1, null, 1);
        int pageSize = ValidateUtil.filterInteger(param.get("pageSize"), true, 1, 500, 15);
        Sort sort = JPAUtil.sort(DPUtil.parseString(param.get("sort")), Arrays.asList("catalog", "code", "mold", "type", "sort"));
        if (null == sort) sort = Sort.by(Sort.Order.asc("mold"), Sort.Order.desc("sort"));
        Page<Standard> data = standardDao.findAll((Specification<Standard>) (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            String catalog = DPUtil.trim(DPUtil.parseString(param.get("catalog")));
            if(!DPUtil.empty(catalog)) {
                predicates.add(cb.equal(root.get("catalog"), catalog));
            }
            String code = DPUtil.trim(DPUtil.parseString(param.get("code")));
            if(!DPUtil.empty(code)) {
                predicates.add(cb.like(root.get("code"), code));
            }
            String another = DPUtil.trim(DPUtil.parseString(param.get("another")));
            if(!DPUtil.empty(another)) {
                predicates.add(cb.gt(
                        cb.function("FIND_IN_SET", Integer.class, cb.literal(another), root.get("another")), 0));
            }
            String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
            if(!DPUtil.empty(name)) {
                predicates.add(cb.like(root.get("name"), name));
            }
            String mold = DPUtil.trim(DPUtil.parseString(param.get("mold")));
            if(!DPUtil.empty(mold)) {
                predicates.add(cb.equal(root.get("mold"), mold));
            }
            String type = DPUtil.trim(DPUtil.parseString(param.get("type")));
            if(!DPUtil.empty(type)) {
                predicates.add(cb.equal(root.get("type"), type));
            }
            String level = DPUtil.trim(DPUtil.parseString(param.get("level")));
            if(!DPUtil.empty(level)) {
                predicates.add(cb.equal(root.get("level"), level));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        }, PageRequest.of(page - 1, pageSize, sort));
        List<Standard> rows = data.getContent();
        if(!DPUtil.empty(config.get("withUserInfo"))) {
            rbacService.fillUserInfo(rows, "createdUid", "updatedUid");
        }
        if(!DPUtil.empty(config.get("withStatusText"))) {
            DPUtil.fillValues(rows, new String[]{"status"}, new String[]{"statusText"}, status());
        }
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("total", data.getTotalElements());
        result.put("rows", rows);
        return result;
    }

    public Standard info(String catalog, String code) {
        Optional<Standard> info = standardDao.findById(Standard.IdClass.builder().catalog(catalog).code(code).build());
        return info.isPresent() ? info.get() : null;
    }

    public Map<String, Object> info(Map<?, ?> param) {
        String catalog = DPUtil.trim(DPUtil.parseString(param.get("catalog")));
        if (DPUtil.empty(catalog)) return ApiUtil.result(1001, "所属目录不能为空", catalog);
        String code = DPUtil.trim(DPUtil.parseString(param.get("code")));
        if (DPUtil.empty(code)) return ApiUtil.result(1002, "标准编码不能为空", code);
        Standard info = info(catalog, code);
        if (null == info) return ApiUtil.result(1404, "标准信息不存在", null);
        ObjectNode result = DPUtil.toJSON(info, ObjectNode.class);
        if ("catalog".equals(info.getMold())) {
            return ApiUtil.result(0, null, result);
        }
        result.put("nullable", DPUtil.parseBoolean(info.getNullable()));
        result.replace("flag", DPUtil.toJSON(DPUtil.explode(",", info.getFlag())));
        result.replace("another", DPUtil.toJSON(DPUtil.explode(",", info.getAnother())));
        return ApiUtil.result(0, null, result);
    }

    public Map<String, Object> save(Map<?, ?> param, HttpServletRequest request) {
        String catalog = DPUtil.trim(DPUtil.parseString(param.get("catalog")));
        if (DPUtil.empty(catalog)) return ApiUtil.result(1001, "所属目录不能为空", catalog);
        if (!Model.safeCatalog(catalog)) return ApiUtil.result(1002, "目录格式异常", catalog);
        String code = DPUtil.trim(DPUtil.parseString(param.get("code")));
        if (DPUtil.empty(code)) return ApiUtil.result(1003, "标准编码不能为空", code);
        if (!Model.safeCode(code)) return ApiUtil.result(1004, "标准编码格式异常", code);
        String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
        if (DPUtil.empty(name)) return ApiUtil.result(1005, "标准名称不能为空", name);
        String mold = DPUtil.trim(DPUtil.parseString(param.get("mold")));
        if (!molds().containsKey(mold)) {
            return ApiUtil.result(1006, "标准类型暂不支持", mold);
        }
        boolean isNew = true;
        int uid = rbacService.uid(request);
        long time = System.currentTimeMillis();
        Standard info = info(catalog, code);
        if (null == info) {
            if(!rbacService.hasPermit(request, "add")) return ApiUtil.result(9403, null, null);
            info = Standard.builder().catalog(catalog).code(code).mold(mold)
                    .createdUid(uid).updatedUid(uid).createdTime(time).updatedTime(time).build();
            info.setPath(catalog + code + ("catalog".equals(mold) ? "/" : ""));
        } else {
            if(!rbacService.hasPermit(request, "modify")) return ApiUtil.result(9403, null, null);
            isNew = false;
            info.setUpdatedUid(uid);
            info.setUpdatedTime(time);
            if (!mold.equals(info.getMold())) return ApiUtil.result(1003, "标准类型不允许修改", mold);
        }
        if (isNew || param.containsKey("name")) info.setName(name);
        if ("catalog".equals(info.getMold())) {
            if (isNew) info.setAnother("");
        } else {
            if (isNew || param.containsKey("level")) {
                String level = DPUtil.trim(DPUtil.parseString(param.get("level")));
                if (!levels().containsKey(level)) {
                    return ApiUtil.result(1009, "标准等级暂不支持", level);
                }
                info.setLevel(level);
            }
            if (isNew || param.containsKey("flag")) {
                info.setFlag(DPUtil.implode(",", DPUtil.parseStringList(param.get("flag"))));
            }
            if (isNew || param.containsKey("another")) {
                info.setAnother(DPUtil.implode(",", DPUtil.parseStringList(param.get("another"))));
            }
            if (isNew || param.containsKey("type")) {
                String type = DPUtil.trim(DPUtil.parseString(param.get("type")));
                if (DPUtil.empty(type)) {
                    return ApiUtil.result(1010, "标准字段类型不能为空", type);
                }
                info.setType(type);
            }
            if (isNew || param.containsKey("size")) info.setSize(DPUtil.parseInt(param.get("size")));
            if (isNew || param.containsKey("digit")) info.setDigit(DPUtil.parseInt(param.get("digit")));
            if (isNew || param.containsKey("nullable")) info.setNullable(DPUtil.parseBoolean(param.get("nullable")) ? 1 : 0);
        }

        if (isNew || param.containsKey("status")) {
            int status = DPUtil.parseInt(param.get("status"));
            if(!status().containsKey(status)) return ApiUtil.result(1014, "状态参数异常", status);
            info.setStatus(status);
        }
        if (isNew || param.containsKey("sort")) info.setSort(DPUtil.parseInt(param.get("sort")));
        if (isNew || param.containsKey("description")) info.setDescription(DPUtil.parseString(param.get("description")));
        standardDao.save(info);
        return info(DPUtil.buildMap("catalog", catalog, "code", code));
    }

    public Map<String, Object> delete(Map<?, ?> param, HttpServletRequest request) {
        String catalog = DPUtil.trim(DPUtil.parseString(param.get("catalog")));
        String code = DPUtil.trim(DPUtil.parseString(param.get("code")));
        if (DPUtil.empty(code)) return ApiUtil.result(1001, "标准编码不能为空", code);
        if(!rbacService.hasPermit(request, "delete")) return ApiUtil.result(9403, null, null);
        Standard info = info(catalog, code);
        if (null == info) return ApiUtil.result(0, null, null);
        Integer result = 1;
        if ("catalog".equals(info.getMold())) {
            String path = info.getPath() + "%";
            result += standardDao.deleteByCatalog(path);
        }
        standardDao.delete(info);
        return ApiUtil.result(0, null, result);
    }

    public Map<String, Standard> fromPath(List<String> codes, String... paths) {
        Map<String, Standard> result = new LinkedHashMap<>();
        if (paths.length < 1) return result;
        List<Standard> list = standardDao.findAll((Specification<Standard>) (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            for (String path : paths) {
                predicates.add(cb.like(root.get("path"), path + "%"));
            }
            return cb.or(predicates.toArray(new Predicate[0]));
        }, Sort.by(Sort.Order.asc("path")));
        if (list.size() < 1) return result;
        List<Standard> data = new ArrayList<>();
        String prefix = null;
        for (Standard standard : list) {
            if (null != prefix) {
                if (standard.getPath().startsWith(prefix)) {
                    continue; // 上级禁用
                } else {
                    prefix = null; // 深度遍历完成
                }
            }
            if (1 == standard.getStatus()) {
                data.add(standard);
            } else {
                prefix = standard.getPath();
            }
        }
        for (Standard standard : data) {
            result.put(standard.getCode(), standard);
            if (null != codes) codes.add(standard.getPath());
            for (String item : DPUtil.explode(",", standard.getAnother())) {
                result.put(item, standard);
                if (null != codes) codes.add(standard.getCatalog() + item);
            }
        }
        return result;
    }

}
