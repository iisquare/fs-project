package com.iisquare.fs.web.oa.service;

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
        helper.equal("bpmInstance").equal("bpmStatus").equal("bpmTask");
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
        List<String> fields = Arrays.asList(MongoCore.FIELD_ID, "frameId",
                "bpmInstance", "bpmStatus", "bpmTask", "bpmIdentity",
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

    public Document save(Document info, int uid) {
        long time = System.currentTimeMillis();
        info.append("updatedTime", time).append("updatedUid", uid);
        if(!info.containsKey(MongoCore.FIELD_ID)) {
            info.append("createdTime", time).append("createdUid", uid);
        }
        return formDataMongo.save(info);
    }

    public long delete(List<String> ids, int uid) {
        if(null == ids || ids.size() < 1) return 0;
        return formDataMongo.delete(ids.toArray(new String[ids.size()]));
    }

}
