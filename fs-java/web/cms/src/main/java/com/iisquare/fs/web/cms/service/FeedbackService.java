package com.iisquare.fs.web.cms.service;

import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.jpa.helper.SpecificationHelper;
import com.iisquare.fs.base.jpa.util.JPAUtil;
import com.iisquare.fs.base.web.mvc.ServiceBase;
import com.iisquare.fs.web.cms.dao.ArticleDao;
import com.iisquare.fs.web.cms.dao.FeedbackDao;
import com.iisquare.fs.web.cms.entity.Article;
import com.iisquare.fs.web.cms.entity.Feedback;
import com.iisquare.fs.web.cms.mvc.Configuration;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class FeedbackService extends ServiceBase {

    @Autowired
    private FeedbackDao feedbackDao;
    @Autowired
    private ArticleDao articleDao;
    @Autowired
    private DefaultRbacService rbacService;
    @Autowired
    private Configuration configuration;

    public Map<?, ?> status(String level) {
        Map<Integer, String> status = new LinkedHashMap<>();
        status.put(1, "待处理");
        status.put(2, "已处理");
        switch (level) {
            case "default":
                break;
            case "full":
                status.put(-1, "已删除");
                break;
            default:
                return null;
        }
        return status;
    }

    public Feedback info(Integer id) {
        if(null == id || id < 1) return null;
        Optional<Feedback> info = feedbackDao.findById(id);
        return info.isPresent() ? info.get() : null;
    }

    public Feedback add(Feedback info, int uid) {
        if (null != info.getId()) return null;
        long time = System.currentTimeMillis();
        info.setStatus(1);
        info.setPublishTime(time);
        info.setPublishUid(uid);
        return feedbackDao.save(info);
    }

    public Map<String, Object> audit(Map param, int uid) {
        Feedback info = info(DPUtil.parseInt(param.get("id")));
        if (null == info || -1 == info.getStatus()) return ApiUtil.result(1401, "信息不存在", param);
        int status = DPUtil.parseInt(param.get("status"));
        if(!status("default").containsKey(status)) return ApiUtil.result(1002, "状态异常", param);
        info.setStatus(status);
        List<String> auditTag = DPUtil.parseStringList(param.get("auditTag"));
        info.setAuditTag(DPUtil.implode(",", DPUtil.toArray(String.class, auditTag)));
        String auditReason = DPUtil.trim(DPUtil.parseString(param.get("auditReason")));
        if (DPUtil.empty(auditReason)) return ApiUtil.result(1003, "请填写审核意见", param);
        info.setAuditReason(auditReason);
        info.setAuditTime(System.currentTimeMillis());
        info.setAuditUid(uid);
        info = feedbackDao.save(info);
        return ApiUtil.result(0, null, info);
    }

    public List<Feedback> fillRefer(List<Feedback> list) {
        if(null == list || list.size() < 1) return list;
        Map<String, Set<String>> map = new LinkedHashMap<>();
        for (Feedback feedback : list) {
            String type = feedback.getReferType();
            Set<String> ids = map.get(type);
            if (null == ids) {
                map.put(type, ids = new HashSet<>());
            }
            String id = feedback.getReferId();
            if (!DPUtil.empty(id)) ids.add(id);
        }
        Map<Integer, Article> articles = new LinkedHashMap<>();
        for (Map.Entry<String, Set<String>> entry : map.entrySet()) {
            Set<String> ids = entry.getValue();
            if (ids.size() < 1) continue;
            switch (entry.getKey()) {
                case "article":
                    articles = DPUtil.list2map(articleDao.findAllById(DPUtil.parseIntList(ids)), Integer.class, "id");
                    break;
            }
        }
        for (Feedback feedback : list) {
            switch (feedback.getReferType()) {
                case "article":
                    Article article = articles.get(DPUtil.parseInt(feedback.getReferId()));
                    if (null == article) break;
                    feedback.setReferIdDigest(article.getTitle());
                    break;
            }
        }
        return list;
    }

    public Map<?, ?> search(Map<?, ?> param, Map<?, ?> args) {
        Map<String, Object> result = new LinkedHashMap<>();
        int page = ValidateUtil.filterInteger(param.get("page"), true, 1, null, 1);
        int pageSize = ValidateUtil.filterInteger(param.get("pageSize"), true, 1, 500, 15);
        Sort sort = JPAUtil.sort(DPUtil.parseString(param.get("sort")), Arrays.asList("id", "publishTime", "auditTime"));
        if (null == sort) sort = Sort.by(Sort.Order.desc("publishTime"));
        Page<Feedback> data = feedbackDao.findAll((Specification<Feedback>) (root, query, cb) -> {
            SpecificationHelper<Feedback> helper = SpecificationHelper.newInstance(root, cb, param);
            helper.dateFormat(configuration.getFormatDate());
            helper.equal("id").equal("referType").equal("referId").like("url").like("ua").equal("ip");
            helper.equalWithIntElseNot("status", -1).betweenWithDate("publishTime").equal("publishUid");
            helper.functionFindInSet("auditTag").like("auditReason").equal("auditUid").betweenWithDate("auditTime");
            return cb.and(helper.predicates());
        }, PageRequest.of(page - 1, pageSize, sort));
        List<Feedback> rows = data.getContent();
        if(!DPUtil.empty(args.get("withUserInfo"))) {
            rbacService.fillUserInfo(rows, "publishUid", "auditUid");
        }
        if(!DPUtil.empty(args.get("withStatusText"))) {
            DPUtil.fillValues(rows, new String[]{"status"}, new String[]{"statusText"}, status("full"));
        }
        if(!DPUtil.empty(args.get("withReferInfo"))) fillRefer(rows);
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("total", data.getTotalElements());
        result.put("rows", rows);
        return result;
    }

    public boolean remove(List<Integer> ids) {
        if(null == ids || ids.size() < 1) return false;
        feedbackDao.deleteInBatch(feedbackDao.findAllById(ids));
        return true;
    }

    public boolean delete(List<Integer> ids, int uid) {
        if(null == ids || ids.size() < 1) return false;
        List<Feedback> list = feedbackDao.findAllById(ids);
        long time = System.currentTimeMillis();
        for (Feedback item : list) {
            item.setStatus(-1);
            item.setAuditTime(time);
            item.setAuditUid(uid);
        }
        feedbackDao.saveAll(list);
        return true;
    }

}
