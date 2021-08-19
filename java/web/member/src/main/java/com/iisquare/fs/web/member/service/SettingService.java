package com.iisquare.fs.web.member.service;

import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.jpa.helper.SQLHelper;
import com.iisquare.fs.base.jpa.util.JPAUtil;
import com.iisquare.fs.base.web.mvc.ServiceBase;
import com.iisquare.fs.web.member.dao.SettingDao;
import com.iisquare.fs.web.member.entity.Setting;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.*;

@Service
public class SettingService extends ServiceBase {

    @Autowired
    private SettingDao settingDao;
    @Autowired
    private UserService userService;
    @Autowired
    private EntityManager entityManager;
    @Value("${spring.datasource.member.table-prefix}")
    private String tablePrefix;

    public boolean set(String type, String key, String value) {
        Setting info = settingDao.findFirstByTypeAndName(type, key);
        if(null == info) return false;
        info.setContent(value);
        return null != save(info, 0);
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
        return SQLHelper.build(entityManager, tablePrefix + "setting").batchInsert(list, true, "content").intValue();
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
            return cb.and(predicates.toArray(new Predicate[predicates.size()]));
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
        return info.isPresent() ? info.get() : null;
    }

    public Setting save(Setting info, int uid) {
        long time = System.currentTimeMillis();
        info.setUpdatedTime(time);
        info.setUpdatedUid(uid);
        return settingDao.save(info);
    }

    public Map<?, ?> search(Map<?, ?> param, Map<?, ?> args) {
        Map<String, Object> result = new LinkedHashMap<>();
        int page = ValidateUtil.filterInteger(param.get("page"), true, 1, null, 1);
        int pageSize = ValidateUtil.filterInteger(param.get("pageSize"), true, 1, 500, 15);
        Sort sort = JPAUtil.sort(DPUtil.parseString(param.get("sort")), Arrays.asList("id", "sort"));
        if (null == sort) sort = Sort.by(Sort.Order.desc("sort"));
        Page<?> data = settingDao.findAll(new Specification() {
            @Override
            public Predicate toPredicate(Root root, CriteriaQuery query, CriteriaBuilder cb) {
                List<Predicate> predicates = new ArrayList<>();
                int id = DPUtil.parseInt(param.get("id"));
                if(id > 0) predicates.add(cb.equal(root.get("id"), id));
                String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
                if(!DPUtil.empty(name)) {
                    predicates.add(cb.like(root.get("name"), "%" + name + "%"));
                }
                String type = DPUtil.trim(DPUtil.parseString(param.get("type")));
                if(!DPUtil.empty(type)) {
                    predicates.add(cb.equal(root.get("type"), type));
                }
                String content = DPUtil.trim(DPUtil.parseString(param.get("content")));
                if(!DPUtil.empty(content)) {
                    predicates.add(cb.like(root.get("content"), "%" + content + "%"));
                }
                return cb.and(predicates.toArray(new Predicate[predicates.size()]));
            }
        }, PageRequest.of(page - 1, pageSize, sort));
        List<?> rows = data.getContent();
        if(!DPUtil.empty(args.get("withUserInfo"))) {
            userService.fillInfo(rows, "createdUid", "updatedUid");
        }
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("total", data.getTotalElements());
        result.put("rows", rows);
        return result;
    }

    public boolean delete(List<Integer> ids) {
        if(null == ids || ids.size() < 1) return false;
        settingDao.deleteInBatch(settingDao.findAllById(ids));
        return true;
    }

}
