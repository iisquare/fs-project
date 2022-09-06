package com.iisquare.fs.web.govern.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.jpa.util.JPAUtil;
import com.iisquare.fs.base.web.mvc.ServiceBase;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.govern.dao.QualityLogicDao;
import com.iisquare.fs.web.govern.entity.Model;
import com.iisquare.fs.web.govern.entity.QualityLogic;
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
public class QualityLogicService extends ServiceBase {

    @Autowired
    private QualityLogicDao qualityLogicDao;
    @Autowired
    private DefaultRbacService rbacService;

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
        Page<QualityLogic> data = qualityLogicDao.findAll((Specification<QualityLogic>) (root, query, cb) -> {
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
            String mold = DPUtil.trim(DPUtil.parseString(param.get("mold")));
            if(!DPUtil.empty(mold)) {
                predicates.add(cb.equal(root.get("mold"), mold));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        }, PageRequest.of(page - 1, pageSize, sort));
        List<QualityLogic> rows = data.getContent();
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

    public QualityLogic info(String catalog, String code) {
        Optional<QualityLogic> info = qualityLogicDao.findById(QualityLogic.IdClass.builder().catalog(catalog).code(code).build());
        return info.isPresent() ? info.get() : null;
    }

    public Map<String, Object> info(Map<?, ?> param) {
        String catalog = DPUtil.trim(DPUtil.parseString(param.get("catalog")));
        if (DPUtil.empty(catalog)) return ApiUtil.result(1001, "所属目录不能为空", catalog);
        String code = DPUtil.trim(DPUtil.parseString(param.get("code")));
        if (DPUtil.empty(code)) return ApiUtil.result(1002, "规则类型编码不能为空", code);
        QualityLogic info = info(catalog, code);
        if (null == info) return ApiUtil.result(1404, "规则类型信息不存在", null);
        ObjectNode result = DPUtil.toJSON(info, ObjectNode.class);
        if ("catalog".equals(info.getMold())) {
            return ApiUtil.result(0, null, result);
        }
        JsonNode content = DPUtil.parseJSON(info.getContent());
        if (null == content || !content.isObject()) content = DPUtil.objectNode();
        result.replace("content", content);
        JsonNode arg = DPUtil.parseJSON(info.getArg());
        if (null == arg || !arg.isObject()) arg = DPUtil.objectNode();
        result.replace("arg", arg);
        return ApiUtil.result(0, null, result);
    }

    public Map<String, Object> save(Map<?, ?> param, HttpServletRequest request) {
        String catalog = DPUtil.trim(DPUtil.parseString(param.get("catalog")));
        if (DPUtil.empty(catalog)) return ApiUtil.result(1001, "所属目录不能为空", catalog);
        if (!Model.safeCatalog(catalog)) return ApiUtil.result(1002, "目录格式异常", catalog);
        String code = DPUtil.trim(DPUtil.parseString(param.get("code")));
        if (DPUtil.empty(code)) return ApiUtil.result(1003, "规则类型编码不能为空", code);
        if (!Model.safeCode(code)) return ApiUtil.result(1004, "规则类型编码格式异常", code);
        String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
        if (DPUtil.empty(name)) return ApiUtil.result(1005, "规则类型名称不能为空", name);
        String mold = DPUtil.trim(DPUtil.parseString(param.get("mold")));
        boolean isNew = true;
        int uid = rbacService.uid(request);
        long time = System.currentTimeMillis();
        QualityLogic info = info(catalog, code);
        if (null == info) {
            if(!rbacService.hasPermit(request, "add")) return ApiUtil.result(9403, null, null);
            info = QualityLogic.builder().catalog(catalog).code(code).mold(mold)
                    .createdUid(uid).updatedUid(uid).createdTime(time).updatedTime(time).build();
            info.setPath(catalog + code + ("catalog".equals(mold) ? "/" : ""));
        } else {
            if(!rbacService.hasPermit(request, "modify")) return ApiUtil.result(9403, null, null);
            isNew = false;
            info.setUpdatedUid(uid);
            info.setUpdatedTime(time);
            if (!mold.equals(info.getMold())) return ApiUtil.result(1003, "规则类型不允许修改", mold);
        }
        if (isNew || param.containsKey("name")) info.setName(name);
        info.setContent(DPUtil.stringify(param.get("content")));
        info.setArg(DPUtil.stringify(param.get("arg")));
        info.setSuggest(DPUtil.parseString(param.get("suggest")));
        if (isNew || param.containsKey("status")) {
            int status = DPUtil.parseInt(param.get("status"));
            if(!status().containsKey(status)) return ApiUtil.result(1014, "状态参数异常", status);
            info.setStatus(status);
        }
        if (isNew || param.containsKey("sort")) info.setSort(DPUtil.parseInt(param.get("sort")));
        if (isNew || param.containsKey("description")) info.setDescription(DPUtil.parseString(param.get("description")));
        qualityLogicDao.save(info);
        return info(DPUtil.buildMap("catalog", catalog, "code", code));
    }

    public Map<String, Object> delete(Map<?, ?> param, HttpServletRequest request) {
        String catalog = DPUtil.trim(DPUtil.parseString(param.get("catalog")));
        String code = DPUtil.trim(DPUtil.parseString(param.get("code")));
        if (DPUtil.empty(code)) return ApiUtil.result(1001, "规则类型编码不能为空", code);
        if(!rbacService.hasPermit(request, "delete")) return ApiUtil.result(9403, null, null);
        QualityLogic info = info(catalog, code);
        if (null == info) return ApiUtil.result(0, null, null);
        Integer result = 1;
        if ("catalog".equals(info.getMold())) {
            String path = info.getPath() + "%";
            result += qualityLogicDao.deleteByCatalog(path);
        }
        qualityLogicDao.delete(info);
        return ApiUtil.result(0, null, result);
    }

}
