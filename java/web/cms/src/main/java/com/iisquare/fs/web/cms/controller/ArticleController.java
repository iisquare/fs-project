package com.iisquare.fs.web.cms.controller;

import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.web.cms.entity.Article;
import com.iisquare.fs.web.cms.entity.Catalog;
import com.iisquare.fs.web.cms.mvc.Configuration;
import com.iisquare.fs.web.cms.service.ArticleService;
import com.iisquare.fs.web.cms.service.CatalogService;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.core.rbac.Permission;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/article")
public class ArticleController extends PermitControllerBase {

    @Autowired
    private DefaultRbacService rbacService;
    @Autowired
    private ArticleService articleService;
    @Autowired
    private CatalogService catalogService;
    @Autowired
    private Configuration configuration;

    @RequestMapping("/info")
    @Permission("")
    public String infoAction(@RequestBody Map<?, ?> param) {
        Integer id = ValidateUtil.filterInteger(param.get("id"), true, 1, null, 0);
        Article info = articleService.info(id);
        if (null != info) {
            rbacService.fillUserInfo(Arrays.asList(info), "createdUid", "updatedUid");
        }
        return ApiUtil.echoResult(null == info ? 404 : 0, null, info);
    }

    @RequestMapping("/list")
    @Permission("")
    public String listAction(@RequestBody Map<?, ?> param) {
        Map<?, ?> result = articleService.search(param, DPUtil.buildMap(
            "withUserInfo", true, "withStatusText", true, "withCatalogInfo", true
        ));
        return ApiUtil.echoResult(0, null, result);
    }

    @RequestMapping("/save")
    @Permission({"add", "modify"})
    public String saveAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        Integer id = ValidateUtil.filterInteger(param.get("id"), true, 1, null, 0);
        String title = DPUtil.trim(DPUtil.parseString(param.get("title")));
        if(DPUtil.empty(title)) return ApiUtil.echoResult(1001, "标题异常", title);
        int status = DPUtil.parseInt(param.get("status"));
        if(!articleService.status("default").containsKey(status)) return ApiUtil.echoResult(1002, "状态异常", status);
        Catalog catalog = catalogService.info(DPUtil.parseInt(param.get("catalogId")));
        if(null == catalog || !catalogService.status("default").containsKey(catalog.getStatus())) {
            return ApiUtil.echoResult(1004, "所属栏目不存在或已删除", param);
        }
        Article info;
        if(id > 0) {
            if(!rbacService.hasPermit(request, "modify")) return ApiUtil.echoResult(9403, null, null);
            info = articleService.info(id);
            if(null == info) return ApiUtil.echoResult(404, null, id);
        } else {
            if(!rbacService.hasPermit(request, "add")) return ApiUtil.echoResult(9403, null, null);
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
        info = articleService.save(info, rbacService.uid(request));
        if (null != info) {
            rbacService.fillUserInfo(Arrays.asList(info), "createdUid", "updatedUid");
        }
        return ApiUtil.echoResult(null == info ? 500 : 0, null, info);
    }

    @RequestMapping("/delete")
    @Permission
    public String deleteAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        List<Integer> ids = null;
        if(param.get("ids") instanceof List) {
            ids = DPUtil.parseIntList((List<?>) param.get("ids"));
        } else {
            ids = Arrays.asList(DPUtil.parseInt(param.get("ids")));
        }
        boolean result = articleService.delete(ids, rbacService.uid(request));
        return ApiUtil.echoResult(result ? 0 : 500, null, result);
    }

    @RequestMapping("/config")
    @Permission("")
    public String configAction(ModelMap model) {
        model.put("status", articleService.status("default"));
        model.put("label", articleService.label());
        model.put("format", articleService.format());
        model.put("catalog", catalogService.tree(DPUtil.buildMap("status", 1), DPUtil.buildMap()));
        return ApiUtil.echoResult(0, null, model);
    }

}
