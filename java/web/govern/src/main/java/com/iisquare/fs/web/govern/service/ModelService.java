package com.iisquare.fs.web.govern.service;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.jpa.util.JPAUtil;
import com.iisquare.fs.base.web.mvc.ServiceBase;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.govern.dao.ModelColumnDao;
import com.iisquare.fs.web.govern.dao.ModelDao;
import com.iisquare.fs.web.govern.dao.ModelRelationDao;
import com.iisquare.fs.web.govern.entity.Model;
import com.iisquare.fs.web.govern.entity.ModelColumn;
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
public class ModelService extends ServiceBase {

    @Autowired
    private ModelDao modelDao;
    @Autowired
    private ModelColumnDao columnDao;
    @Autowired
    private ModelRelationDao relationDao;
    @Autowired
    private DefaultRbacService rbacService;
    @Autowired
    private ModelColumnService columnService;

    public Map<?, ?> types() {
        Map<String, String> result = new LinkedHashMap<>();
        result.put("catalog", "包"); // 目录结构，非catalog外为实体
        result.put("table", "类"); // 支持多种非包类型
        return result;
    }

    public Map<String, Object> search(Map<?, ?> param, Map<?, ?> config) {
        Map<String, Object> result = new LinkedHashMap<>();
        int page = ValidateUtil.filterInteger(param.get("page"), true, 1, null, 1);
        int pageSize = ValidateUtil.filterInteger(param.get("pageSize"), true, 1, 500, 15);
        Sort sort = JPAUtil.sort(DPUtil.parseString(param.get("sort")), Arrays.asList("catalog", "code", "type", "sort"));
        if (null == sort) sort = Sort.by(Sort.Order.asc("type"), Sort.Order.desc("sort"));
        Page<Model> data = modelDao.findAll((Specification<Model>) (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            String catalog = DPUtil.trim(DPUtil.parseString(param.get("catalog")));
            if(!DPUtil.empty(catalog)) {
                predicates.add(cb.equal(root.get("catalog"), catalog));
            }
            String code = DPUtil.trim(DPUtil.parseString(param.get("code")));
            if(!DPUtil.empty(code)) {
                predicates.add(cb.like(root.get("code"), code));
            }
            String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
            if(!DPUtil.empty(name)) {
                predicates.add(cb.like(root.get("name"), name));
            }
            String type = DPUtil.trim(DPUtil.parseString(param.get("type")));
            if(!DPUtil.empty(type)) {
                predicates.add(cb.equal(root.get("type"), type));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        }, PageRequest.of(page - 1, pageSize, sort));
        List<Model> rows = data.getContent();
        if(!DPUtil.empty(config.get("withUserInfo"))) {
            rbacService.fillUserInfo(rows, "createdUid", "updatedUid");
        }
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("total", data.getTotalElements());
        result.put("rows", rows);
        return result;
    }

    public Model info(String catalog, String code) {
        Optional<Model> info = modelDao.findById(Model.IdClass.builder().catalog(catalog).code(code).build());
        return info.isPresent() ? info.get() : null;
    }

    public Map<String, Object> info(Map<?, ?> param) {
        String catalog = DPUtil.trim(DPUtil.parseString(param.get("catalog")));
        if (DPUtil.empty(catalog)) return ApiUtil.result(1001, "所属包不能为空", catalog);
        String code = DPUtil.trim(DPUtil.parseString(param.get("code")));
        if (DPUtil.empty(code)) return ApiUtil.result(1002, "元模型编码不能为空", code);
        Model info = info(catalog, code);
        if (null == info) return ApiUtil.result(1404, "元模型信息不存在", null);
        ObjectNode result = DPUtil.toJSON(info, ObjectNode.class);
        if ("catalog".equals(info.getType())) {
            return ApiUtil.result(0, null, result);
        }
        result.replace("pk", DPUtil.toJSON(DPUtil.explode(",", info.getPk())));
        List<ModelColumn> columns = columnDao.findAllByCatalogAndModel(
                info.getCatalog(), info.getCode(), Sort.by(Sort.Order.asc("sort")));
        result.replace("columns", columnService.format(columns));
        return ApiUtil.result(0, null, result);
    }

    public Map<String, Object> save(Map<?, ?> param, HttpServletRequest request) {
        String catalog = DPUtil.trim(DPUtil.parseString(param.get("catalog")));
        if (DPUtil.empty(catalog)) return ApiUtil.result(1001, "所属包不能为空", catalog);
        if (!Model.safeCatalog(catalog)) return ApiUtil.result(1002, "元模型包格式异常", catalog);
        String code = DPUtil.trim(DPUtil.parseString(param.get("code")));
        if (DPUtil.empty(code)) return ApiUtil.result(1003, "元模型编码不能为空", code);
        if (!Model.safeCode(code)) return ApiUtil.result(1004, "元模型编码格式异常", code);
        String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
        if (DPUtil.empty(name)) return ApiUtil.result(1005, "元模型名称不能为空", name);
        String type = DPUtil.trim(DPUtil.parseString(param.get("type")));
        if (!types().containsKey(type)) {
            return ApiUtil.result(1006, "元模型类型暂不支持", type);
        }
        boolean isNew = true;
        int uid = rbacService.uid(request);
        long time = System.currentTimeMillis();
        Model info = info(catalog, code);
        if (null == info) {
            if(!rbacService.hasPermit(request, "add")) return ApiUtil.result(9403, null, null);
            info = Model.builder().catalog(catalog).code(code).type(type)
                    .createdUid(uid).updatedUid(uid).createdTime(time).updatedTime(time).build();
        } else {
            if(!rbacService.hasPermit(request, "modify")) return ApiUtil.result(9403, null, null);
            isNew = false;
            info.setUpdatedUid(uid);
            info.setUpdatedTime(time);
            if (!type.equals(info.getType())) return ApiUtil.result(1003, "元模型类型不允许修改", type);
        }
        if (isNew || param.containsKey("name")) info.setName(name);
        if (isNew || param.containsKey("pk")) {
            info.setPk(DPUtil.implode(",", DPUtil.parseStringList(param.get("pk")).toArray(new String[0])));
        }
        if (isNew || param.containsKey("sort")) info.setSort(DPUtil.parseInt(param.get("sort")));
        if (isNew || param.containsKey("description")) info.setDescription(DPUtil.parseString(param.get("description")));
        modelDao.save(info);
        if (!"catalog".equals(info.getType())) {
            Map<String, Object> result = columnService.save(
                    info.getCatalog(), info.getCode(), DPUtil.toJSON(param.get("columns")));
            if (ApiUtil.failed(result)) return result;
        }
        return info(DPUtil.buildMap("catalog", catalog, "code", code));
    }

    public Map<String, Object> delete(Map<?, ?> param, HttpServletRequest request) {
        String catalog = DPUtil.trim(DPUtil.parseString(param.get("catalog")));
        String code = DPUtil.trim(DPUtil.parseString(param.get("code")));
        if (DPUtil.empty(code)) return ApiUtil.result(1001, "元模型编码不能为空", code);
        if(!rbacService.hasPermit(request, "delete")) return ApiUtil.result(9403, null, null);
        Model info = info(catalog, code);
        if (null == info) return ApiUtil.result(0, null, null);
        Integer result = 0;
        if ("catalog".equals(info.getType())) {
            String path = info.path() + "%";
            result += modelDao.deleteByCatalog(path);
            result += columnDao.deleteByCatalog(path);
            result += relationDao.deleteByCatalog(path);
        } else {
            result += columnDao.deleteByCatalogAndModel(catalog, code);
            result += relationDao.deleteByCatalogAndModel(catalog, code);
        }
        modelDao.delete(info);
        return ApiUtil.result(0, null, result);
    }

}
