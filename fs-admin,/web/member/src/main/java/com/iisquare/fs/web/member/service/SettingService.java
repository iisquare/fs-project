package com.iisquare.fs.web.member.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.jpa.helper.SQLHelper;
import com.iisquare.fs.base.jpa.mvc.JPAServiceBase;
import com.iisquare.fs.web.member.dao.SettingDao;
import com.iisquare.fs.web.member.entity.Setting;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;

@Service
public class SettingService extends JPAServiceBase {

    @Autowired
    private SettingDao settingDao;
    @Autowired
    private UserService userService;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private RbacService rbacService;

    public boolean set(String type, String key, String value) {
        Setting info = settingDao.findFirstByTypeAndName(type, key);
        if(null == info) return false;
        info.setContent(value);
        info.setUpdatedTime(System.currentTimeMillis());
        info.setUpdatedUid(0);
        settingDao.save(info);
        return true;
    }

    @Transactional
    public int set(String type, Map<String, String> data) {
        if (data.size() < 1) return 0;
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map.Entry<String, String> entry : data.entrySet()) {
            list.add(new LinkedHashMap<String, Object>(){{
                put("type", type);
                put("name", entry.getKey());
                put("content", entry.getValue());
                put("description", "系统更新");
            }});
        }
        return SQLHelper.build(entityManager, Setting.class).batchInsert(list, "content").intValue();
    }

    public Map<String, String> get(String type, List<String> include, List<String> exclude) {
        List<Setting> list = settingDao.findAll((Specification<Setting>) (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("type"), type));
            if (null != include && include.size() > 0) {
                predicates.add(root.get("name").in(DPUtil.toArray(String.class, include)));
            }
            if (null != exclude && exclude.size() > 0) {
                predicates.add(cb.not(root.get("name").in(DPUtil.toArray(String.class, exclude))));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        });
        Map<String, String> result = new LinkedHashMap<>();
        for (Setting setting : list) {
            result.put(setting.getName(), setting.getContent());
        }
        return result;
    }

    public String get(String type, String key) {
        Setting info = settingDao.findFirstByTypeAndName(type, key);
        if(null == info) return "";
        String value = info.getContent();
        if(null == value) return "";
        return value;
    }

    public Setting info(Integer id) {
        if(null == id || id < 1) return null;
        Optional<Setting> info = settingDao.findById(id);
        return info(settingDao, id);
    }

    public Map<String, Object> save(Map<?, ?> param, HttpServletRequest request) {
        Integer id = ValidateUtil.filterInteger(param.get("id"), true, 1, null, 0);
        String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
        if(DPUtil.empty(name)) return ApiUtil.result(1001, "名称异常", name);
        String type = DPUtil.trim(DPUtil.parseString(param.get("type")));
        Setting info = null;
        if(id > 0) {
            if(!rbacService.hasPermit(request, "modify")) return ApiUtil.result(9403, null, null);
            info = info(id);
            if(null == info) return ApiUtil.result(404, null, id);
        } else {
            if(!rbacService.hasPermit(request, "add")) return ApiUtil.result(9403, null, null);
            info = new Setting();
        }
        info.setName(name);
        info.setType(type);
        info.setContent(DPUtil.parseString(param.get("content")));
        info.setSort(DPUtil.parseInt(param.get("sort")));
        info.setDescription(DPUtil.parseString(param.get("description")));
        info.setUpdatedTime(System.currentTimeMillis());
        info.setUpdatedUid(rbacService.uid(request));
        info = settingDao.save(info);
        return ApiUtil.result(0, null, info);
    }

    public ObjectNode search(Map<String, Object> param, Map<?, ?> args) {
        ObjectNode result = search(settingDao, param, (Specification<Setting>) (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            int id = DPUtil.parseInt(param.get("id"));
            if (id > 0) predicates.add(cb.equal(root.get("id"), id));
            String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
            if (!DPUtil.empty(name)) {
                predicates.add(cb.like(root.get("name"), "%" + name + "%"));
            }
            String type = DPUtil.trim(DPUtil.parseString(param.get("type")));
            if (!DPUtil.empty(type)) {
                predicates.add(cb.equal(root.get("type"), type));
            }
            String content = DPUtil.trim(DPUtil.parseString(param.get("content")));
            if (!DPUtil.empty(content)) {
                predicates.add(cb.like(root.get("content"), "%" + content + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        }, Sort.by(Sort.Order.desc("sort")), "id", "sort");
        JsonNode rows = ApiUtil.rows(result);
        if(!DPUtil.empty(args.get("withUserInfo"))) {
            userService.fillInfo(rows, "createdUid", "updatedUid");
        }
        return result;
    }

    public boolean remove(List<Integer> ids) {
        return remove(settingDao, ids);
    }

}
