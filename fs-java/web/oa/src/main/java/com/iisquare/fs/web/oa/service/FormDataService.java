package com.iisquare.fs.web.oa.service;

import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.mongodb.MongoCore;
import com.iisquare.fs.base.mongodb.helper.FilterHelper;
import com.iisquare.fs.base.mongodb.util.MongoUtil;
import com.iisquare.fs.base.web.mvc.ServiceBase;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.oa.mongodb.FormDataMongo;
import com.iisquare.fs.web.oa.mvc.Configuration;
import com.mongodb.client.model.Sorts;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

@Service
public class FormDataService extends ServiceBase {

    @Autowired
    private Configuration configuration;
    @Autowired
    private FormDataMongo formDataMongo;
    @Autowired
    private DefaultRbacService rbacService;
    @Autowired
    private FormFrameService formFrameService;

    public Map<?, ?> sort() {
        Map<String, String> sort = new LinkedHashMap<>();
        sort.put(MongoCore.FIELD_ID + ".asc", "数据记录正序");
        sort.put(MongoCore.FIELD_ID + ".desc", "数据记录逆序");
        sort.put("createdTime.asc", "创建时间正序");
        sort.put("createdTime.desc", "创建时间逆序");
        sort.put("updatedTime.asc", "修改时间正序");
        sort.put("updatedTime.desc", "修改时间逆序");
        return sort;
    }

    public Map<?, ?> search(Map<?, ?> param, Map<?, ?> config) {
        Map<String, Object> result = new LinkedHashMap<>();
        int page = ValidateUtil.filterInteger(param.get("page"), true, 1, null, 1);
        int pageSize = ValidateUtil.filterInteger(param.get("pageSize"), true, 1, 500, 15);
        Bson sort = MongoUtil.sort(DPUtil.parseString(param.get("sort")),
                Arrays.asList(MongoCore.FIELD_ID, "createdTime", "updatedTime"));
        if (null == sort) sort = Sorts.descending(MongoCore.FIELD_ID);
        FilterHelper helper = FilterHelper.newInstance(param).dateFormat(configuration.getFormatDate());
        helper.equalWithObjectId(MongoCore.FIELD_ID).equalWithIntNotEmpty("frameId");
        helper.equal("bpmWorkflowId").equal("bpmInstanceId").equal("bpmStartUserId");
        helper.betweenWithDate("createdTime").betweenWithDate("updatedTime");
        helper.equalWithIntNotEmpty("createdUid").equalWithIntNotEmpty("updatedUid");
        Bson filter = helper.filter();
        long total = formDataMongo.count(filter);
        List<Document> rows = format(total > 0 ? formDataMongo.all(filter, sort, page, pageSize) : Arrays.asList());
        if(!DPUtil.empty(config.get("withUserInfo"))) {
            rbacService.fillUserInfo(rows, "createdUid", "updatedUid");
        }
        if(!DPUtil.empty(config.get("withFormFrameInfo"))) {
            formFrameService.fillInfo(rows, "frameId");
        }
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("total", total);
        result.put("rows", rows);
        return result;
    }

    public List<Document> format(List<Document> rows) {
        List<String> fields = Arrays.asList(
                MongoCore.FIELD_ID, "frameId",
                "bpmWorkflowId", "bpmInstanceId", "bpmStartUserId",
                "createdTime", "createdUid", "updatedTime", "updatedUid");
        List<Document> result = new ArrayList<>();
        for (Document row : rows) {
            Document document = new Document();
            for (String field : fields) {
                document.put(field, row.get(field));
            }
            document.put("content", row);
            result.add(document);
        }
        return result;
    }

    public Document info(String id) {
        if(DPUtil.empty(id)) return null;
        return formDataMongo.one(id);
    }

    public Map<String, Object> save(Map<?, ?> param, HttpServletRequest request) {
        String id = DPUtil.trim(DPUtil.parseString(param.get(MongoCore.FIELD_ID)));
        Integer frameId = ValidateUtil.filterInteger(param.get("frameId"), true, 1, null, 0);
        if(frameId < 1) return ApiUtil.result(1001, "所属表单参数异常", frameId);
        String bpmWorkflowId = DPUtil.trim(DPUtil.parseString(param.get("bpmWorkflowId")));
        String bpmInstanceId = DPUtil.trim(DPUtil.parseString(param.get("bpmInstanceId")));
        String bpmStartUserId = DPUtil.trim(DPUtil.parseString(param.get("bpmStartUserId")));
        Document document = Document.parse(DPUtil.stringify(param.get("content")));
        document.append("frameId", frameId).append("bpmWorkflowId", bpmWorkflowId);
        document.append("bpmInstanceId", bpmInstanceId).append("bpmStartUserId", bpmStartUserId);
        if(DPUtil.empty(id)) {
            if(!rbacService.hasPermit(request, "add")) return ApiUtil.result(9403, null, null);
            document.remove(MongoCore.FIELD_ID);
        } else {
            if(!rbacService.hasPermit(request, "modify")) return ApiUtil.result(9403, null, null);
            document.put(MongoCore.FIELD_ID, id);
        }
        document = save(document, rbacService.uid(request));
        return ApiUtil.result(null == document ? 500 : 0, null, document);

    }

    public Document save(Document document, int uid) {
        long time = System.currentTimeMillis();
        document.append("updatedTime", time).append("updatedUid", uid);
        if(!document.containsKey(MongoCore.FIELD_ID)) {
            document.append("createdTime", time).append("createdUid", uid);
        }
        return formDataMongo.save(document);
    }

    public long delete(List<String> ids, int uid) {
        if(null == ids || ids.size() < 1) return 0;
        return formDataMongo.delete(ids.toArray(new String[0]));
    }

}
