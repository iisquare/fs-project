package com.iisquare.fs.web.lucene.service;

import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.jpa.helper.SpecificationHelper;
import com.iisquare.fs.base.jpa.util.JPAUtil;
import com.iisquare.fs.base.web.mvc.ServiceBase;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.lucene.dao.DictionaryDao;
import com.iisquare.fs.web.lucene.entity.Dictionary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.Predicate;
import java.util.*;

@Service
public class DictionaryService extends ServiceBase {

    @Autowired
    private DictionaryDao dictionaryDao;
    @Autowired
    private DefaultRbacService rbacService;

    public Map<?, ?> type() {
        Map<String, String> type = new LinkedHashMap<>();
        type.put("word", "分词");
        type.put("synonym", "同义词");
        type.put("stopword", "停用词");
        type.put("quantifier", "量词");
        return type;
    }

    public Map<?, ?> sort() {
        Map<String, String> sort = new LinkedHashMap<>();
        sort.put("content.asc", "词条正序");
        sort.put("content.desc", "词条逆序");
        sort.put("createdTime.asc", "创建时间正序");
        sort.put("createdTime.desc", "创建时间逆序");
        sort.put("updatedTime.asc", "修改时间正序");
        sort.put("updatedTime.desc", "修改时间逆序");
        return sort;
    }

    public String plain(Map<String, Object> param) {
        List<Dictionary> list = dictionaryDao.findAll((Specification<Dictionary>) (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("catalogue"), DPUtil.parseString(param.get("catalogue"))));
            predicates.add(cb.equal(root.get("type"), DPUtil.parseString(param.get("type"))));
            return cb.and(predicates.toArray(new Predicate[0]));
        });
        StringBuilder builder = new StringBuilder();
        for (Dictionary dictionary : list) {
            builder.append(dictionary.getContent()).append("\n");
        }
        return builder.toString();
    }

    public Integer unique(String catalogue, String type) {
        return dictionaryDao.unique(catalogue, type);
    }

    public Dictionary info(Integer id) {
        if(null == id || id < 1) return null;
        Optional<Dictionary> info = dictionaryDao.findById(id);
        return info.isPresent() ? info.get() : null;
    }

    public List<Dictionary> saveAll(Dictionary info, int uid) {
        if (null == info || DPUtil.empty(info.getContent())) return null;
        if (!DPUtil.empty(info.getId())) {
            return Arrays.asList(save(info, uid));
        }
        long time = System.currentTimeMillis();
        List<Dictionary> list = new ArrayList<>();
        for (String content : DPUtil.explode(info.getContent(), "\n")) {
            content = DPUtil.trim(content);
            if (DPUtil.empty(content)) continue;
            list.add(Dictionary.builder()
                    .catalogue(info.getCatalogue()).type(info.getType()).source(info.getSource()).content(content)
                    .createdTime(time).createdUid(uid).updatedTime(time).updatedUid(uid)
                    .build());
        }
        if (list.size() < 1) return null;
        return dictionaryDao.saveAll(list);
    }

    public Dictionary save(Dictionary info, int uid) {
        long time = System.currentTimeMillis();
        info.setUpdatedTime(time);
        info.setUpdatedUid(uid);
        if(null == info.getId()) {
            info.setCreatedTime(time);
            info.setCreatedUid(uid);
        }
        return dictionaryDao.save(info);
    }

    public Map<?, ?> search(Map<?, ?> param, Map<?, ?> config) {
        Map<String, Object> result = new LinkedHashMap<>();
        int page = ValidateUtil.filterInteger(param.get("page"), true, 1, null, 1);
        int pageSize = ValidateUtil.filterInteger(param.get("pageSize"), true, 1, 500, 15);
        Sort sort = JPAUtil.sort(DPUtil.parseString(param.get("sort")), Arrays.asList("content", "createdTime", "updatedTime"));
        if (null == sort) sort = Sort.by(Sort.Order.desc("updatedTime"));
        Page<Dictionary> data = dictionaryDao.findAll((Specification<Dictionary>) (root, query, cb) -> {
            SpecificationHelper<Dictionary> helper = SpecificationHelper.newInstance(root, cb, param);
            helper.equal("catalogue").equal("type").like("content").in("source");
            return cb.and(helper.predicates());
        }, PageRequest.of(page - 1, pageSize, sort));
        List<?> rows = data.getContent();
        if(!DPUtil.empty(config.get("withUserInfo"))) {
            rbacService.fillUserInfo(rows, "createdUid", "updatedUid");
        }
        if(!DPUtil.empty(config.get("withTypeText"))) {
            DPUtil.fillValues(rows, new String[]{"type"}, new String[]{"typeText"}, type());
        }
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("total", data.getTotalElements());
        result.put("rows", rows);
        return result;
    }

    public boolean delete(List<Integer> ids) {
        if(null == ids || ids.size() < 1) return false;
        dictionaryDao.deleteInBatch(dictionaryDao.findAllById(ids));
        return true;
    }

}
