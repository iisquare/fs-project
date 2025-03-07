package com.iisquare.fs.web.cms.service;

import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.jpa.helper.SpecificationHelper;
import com.iisquare.fs.base.jpa.util.JPAUtil;
import com.iisquare.fs.base.web.mvc.ServiceBase;
import com.iisquare.fs.web.cms.dao.ArticleDao;
import com.iisquare.fs.web.cms.entity.Article;
import com.iisquare.fs.web.cms.entity.Catalog;
import com.iisquare.fs.web.cms.mvc.Configuration;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

@Service
public class ArticleService extends ServiceBase {

    @Autowired
    private ArticleDao articleDao;
    @Autowired
    private CatalogService catalogService;
    @Autowired
    private DefaultRbacService rbacService;
    @Autowired
    private Configuration configuration;

    public Map<?, ?> status(String level) {
        Map<Integer, String> status = new LinkedHashMap<>();
        status.put(1, "启用");
        status.put(2, "关闭");
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

    public Map<String, String> label() {
        Map<String, String> label = new LinkedHashMap<>();
        label.put("commentEnabled", "启用评论");
        label.put("original", "原创");
        label.put("repost", "转载");
        label.put("translate", "翻译");
        return label;
    }

    public Map<String, String> format() {
        Map<String, String> format = new LinkedHashMap<>();
        format.put("html", "富文本");
        format.put("text", "纯文本");
        format.put("markdown", "Markdown");
        return format;
    }

    public Article info(Integer id) {
        if(null == id || id < 1) return null;
        Optional<Article> info = articleDao.findById(id);
        return info.isPresent() ? info.get() : null;
    }

    public Map<String, Object> save(Map<?, ?> param, HttpServletRequest request) {
        Integer id = ValidateUtil.filterInteger(param.get("id"), true, 1, null, 0);
        String title = DPUtil.trim(DPUtil.parseString(param.get("title")));
        if(DPUtil.empty(title)) return ApiUtil.result(1001, "标题异常", title);
        int status = DPUtil.parseInt(param.get("status"));
        if(!status("default").containsKey(status)) return ApiUtil.result(1002, "状态异常", status);
        Catalog catalog = catalogService.info(DPUtil.parseInt(param.get("catalogId")));
        if(null == catalog || !catalogService.status("default").containsKey(catalog.getStatus())) {
            return ApiUtil.result(1004, "所属栏目不存在或已删除", param);
        }
        Article info;
        if(id > 0) {
            if(!rbacService.hasPermit(request, "modify")) return ApiUtil.result(9403, null, null);
            info = info(id);
            if(null == info) return ApiUtil.result(404, null, id);
        } else {
            if(!rbacService.hasPermit(request, "add")) return ApiUtil.result(9403, null, null);
            info = new Article();
        }
        info.setTitle(title);
        info.setCatalogId(catalog.getId());
        info.setCover(DPUtil.trim(DPUtil.parseString(param.get("cover"))));
        info.setKeyword(DPUtil.trim(DPUtil.parseString(param.get("keyword"))));
        info.setLabel(DPUtil.trim(DPUtil.parseString(param.get("label"))));
        info.setTag(DPUtil.trim(DPUtil.parseString(param.get("tag"))));
        info.setCiteName(DPUtil.trim(DPUtil.parseString(param.get("citeName"))));
        info.setCiteAuthor(DPUtil.trim(DPUtil.parseString(param.get("citeAuthor"))));
        info.setCiteUrl(DPUtil.trim(DPUtil.parseString(param.get("citeUrl"))));
        info.setPassword(DPUtil.trim(DPUtil.parseString(param.get("password"))));
        info.setFormat(DPUtil.trim(DPUtil.parseString(param.get("format"))));
        info.setContent(DPUtil.trim(DPUtil.parseString(param.get("content"))));
        info.setPublishTime(DPUtil.dateTime2millis(param.get("publishTime"), configuration.getFormatDate()));
        info.setSort(DPUtil.parseInt(param.get("sort")));
        info.setStatus(status);
        info.setDescription(DPUtil.parseString(param.get("description")));
        int uid = rbacService.uid(request);
        long time = System.currentTimeMillis();
        info.setUpdatedTime(time);
        info.setUpdatedUid(uid);
        if(null == info.getId()) {
            info.setCountComment(0);
            info.setCountApprove(0);
            info.setCountOppose(0);
            info.setCountView(0);
            info.setCreatedTime(time);
            info.setCreatedUid(uid);
        }
        if (info.getPublishTime() < 1) {
            info.setPublishTime(info.getCreatedTime());
        }
        info = articleDao.save(info);
        rbacService.fillUserInfo(Arrays.asList(info), "createdUid", "updatedUid");
        return ApiUtil.result(0, null, info);
    }

    public Map<?, ?> search(Map<?, ?> param, Map<?, ?> args) {
        Map<String, Object> result = new LinkedHashMap<>();
        int page = ValidateUtil.filterInteger(param.get("page"), true, 1, null, 1);
        int pageSize = ValidateUtil.filterInteger(param.get("pageSize"), true, 1, 500, 15);
        Sort sort = JPAUtil.sort(DPUtil.parseString(param.get("sort")),
                Arrays.asList("id", "sort", "publishTime", "createdTime", "updatedTime", "countView", "countApprove", "countOppose", "countComment"));
        if (null == sort) sort = Sort.by(Sort.Order.desc("updatedTime"));
        Page<?> data = articleDao.findAll((Specification<Article>) (root, query, cb) -> {
            SpecificationHelper<Article> helper = SpecificationHelper.newInstance(root, cb, param);
            helper.dateFormat(configuration.getFormatDate());
            helper.equal("id").like("title").in("catalogId").functionFindInSet("label").functionFindInSet("tag")
                    .equal("citeName").equal("citeAuthor").equal("format").between("countView").between("countLike").between("countComment");
            helper.equalWithIntElseNot("status", -1).betweenWithDate("publishTime").betweenWithDate("createdTime").betweenWithDate("updatedTime");
            return cb.and(helper.predicates());
        }, PageRequest.of(page - 1, pageSize, sort));
        List<?> rows = data.getContent();
        if(!DPUtil.empty(args.get("withUserInfo"))) {
            rbacService.fillUserInfo(rows, "createdUid", "updatedUid");
        }
        if(!DPUtil.empty(args.get("withStatusText"))) {
            DPUtil.fillValues(rows, new String[]{"status"}, new String[]{"statusText"}, status("full"));
        }
        if(!DPUtil.empty(args.get("withCatalogInfo"))) {
            catalogService.fillInfo(rows, "catalogId");
        }
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("total", data.getTotalElements());
        result.put("rows", rows);
        return result;
    }

    public boolean remove(List<Integer> ids) {
        if(null == ids || ids.size() < 1) return false;
        articleDao.deleteInBatch(articleDao.findAllById(ids));
        return true;
    }

    public boolean delete(List<Integer> ids, int uid) {
        if(null == ids || ids.size() < 1) return false;
        List<Article> list = articleDao.findAllById(ids);
        long time = System.currentTimeMillis();
        for (Article item : list) {
            item.setStatus(-1);
            item.setUpdatedTime(time);
            item.setUpdatedUid(uid);
        }
        articleDao.saveAll(list);
        return true;
    }

}
