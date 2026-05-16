package com.iisquare.fs.web.lm.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.jpa.helper.SpecificationHelper;
import com.iisquare.fs.base.jpa.mvc.JPAServiceBase;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.lm.core.TrieNode;
import com.iisquare.fs.web.lm.dao.SensitiveDao;
import com.iisquare.fs.web.lm.entity.Sensitive;
import com.iisquare.fs.web.lm.mvc.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

@Service
public class SensitiveService extends JPAServiceBase {

    @Autowired
    private SensitiveDao sensitiveDao;
    @Autowired
    DefaultRbacService rbacService;
    @Autowired
    Configuration configuration;

    private TrieNode trie = new TrieNode(); // 敏感词的前缀字典树

    private int trieLevel = 0; // 树的最大层级，可用于截断迭代输出的监测窗口

    private final Semaphore semaphore = new Semaphore(1);

    public boolean rebuild() {
        if (!semaphore.tryAcquire()) return false;
        int level = 0;
        TrieNode root = new TrieNode(); // 根为空节点
        List<Sensitive> list = sensitiveDao.findAll((Specification<Sensitive>) (root1, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root1.get("status"), 3)); // 仅处理拦截状态的关键词
            return cb.and(predicates.toArray(new Predicate[0]));
        });
        for (Sensitive sensitive : list) {
            String[] keyword = DPUtil.explode("", sensitive.getContent());
            if (keyword.length == 0) continue;
            rebuild(root, keyword);
            level = Math.max(level, keyword.length);
        }
        this.trie = root;
        this.trieLevel = level;
        semaphore.release();
        return true;
    }
    
    private void rebuild(TrieNode parent, String[] keyword) {
        if (parent.level >= keyword.length) {
            return;
        }
        String world = keyword[parent.level];
        TrieNode node = parent.children.get(world);
        if (null == node) {
            node = new TrieNode();
            node.world = world;
            node.level = parent.level + 1;
            node.composable = node.level >= keyword.length;
            node.parent = parent;
            parent.children.put(world, node);
        } else {
            node.composable = node.composable || node.level >= keyword.length;
        }
        rebuild(node, keyword);
    }

    public int window() {
        return trieLevel;
    }

    /**
     * 推荐采用树深度+新生成内容长度作为实际输出内容的检测窗口大小
     */
    public String window(String sentence, int size) {
        return DPUtil.substring(sentence, -size);
    }

    public List<String> check(String sentence) {
        String[] keyword = DPUtil.explode("", sentence);
        List<String> result = new ArrayList<>();
        for (int i = 0; i < keyword.length; i++) {
            sentence = "";
            TrieNode node = this.trie;
            for (int j = i; j < keyword.length; j++) {
                String word = keyword[j];
                sentence += word;
                node = node.children.get(word);
                if (null == node) break;
                if (node.composable) result.add(sentence);
            }
        }
        return result;
    }

    public Map<?, ?> status() {
        Map<Integer, String> status = new LinkedHashMap<>();
        status.put(1, "监测");
        status.put(2, "预警");
        status.put(3, "拦截");
        return status;
    }

    public JsonNode risk() {
        return rbacService.dictionary("ai-risk");
    }

    public Sensitive info(Integer id) {
        return info(sensitiveDao, id);
    }

    public Map<String, Object> save(Map<?, ?> param, HttpServletRequest request) {
        Integer id = ValidateUtil.filterInteger(param.get("id"), true, 1, null, 0);
        String content = DPUtil.trim(DPUtil.parseString(param.get("content")));
        if(DPUtil.empty(content)) return ApiUtil.result(1001, "关键词内容异常", content);
        int status = DPUtil.parseInt(param.get("status"));
        if(!status().containsKey(status)) return ApiUtil.result(1002, "状态异常", status);
        Sensitive info = null;
        if(id > 0) {
            if(!rbacService.hasPermit(request, "modify")) return ApiUtil.result(9403, null, null);
            info = info(id);
            if(null == info) return ApiUtil.result(404, null, id);
        } else {
            if(!rbacService.hasPermit(request, "add")) return ApiUtil.result(9403, null, null);
            info = new Sensitive();
        }
        info.setContent(content);
        info.setRisk(DPUtil.implode(",", DPUtil.parseIntList(param.get("risk"))));
        info.setSort(DPUtil.parseInt(param.get("sort")));
        info.setStatus(status);
        info.setDescription(DPUtil.parseString(param.get("description")));
        info = save(sensitiveDao, info, rbacService.uid(request));
        return ApiUtil.result(0, null, info);
    }

    public ObjectNode search(Map<String, Object> param, Map<?, ?> args) {
        ObjectNode result = search(sensitiveDao, param, (root, query, cb) -> {
            SpecificationHelper<Sensitive> helper = SpecificationHelper.newInstance(root, cb, param);
            helper.dateFormat(configuration.getFormatDate()).equalWithIntGTZero("id");
            helper.equalWithIntNotEmpty("status").like("content").functionFindInSet("risk");
            return cb.and(helper.predicates());
        }, Sort.by(Sort.Order.desc("sort")), "id", "status", "sort");
        JsonNode rows = format(ApiUtil.rows(result));
        if(!DPUtil.empty(args.get("withUserInfo"))) {
            rbacService.fillUserInfo(rows, "createdUid", "updatedUid");
        }
        if(!DPUtil.empty(args.get("withStatusText"))) {
            fillStatus(rows, status());
        }
        return result;
    }

    public JsonNode format(JsonNode rows) {
        for (JsonNode row : rows) {
            ObjectNode node = (ObjectNode) row;
            String[] risk = DPUtil.explode(",", node.at("/risk").asText());
            node.replace("risk", DPUtil.toJSON(risk));
        }
        return rows;
    }

    public boolean remove(List<Integer> ids) {
        return remove(sensitiveDao, ids);
    }

}
