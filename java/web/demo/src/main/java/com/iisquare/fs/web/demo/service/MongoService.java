package com.iisquare.fs.web.demo.service;

import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.mongodb.MongoCore;
import com.iisquare.fs.base.mongodb.helper.FilterHelper;
import com.iisquare.fs.base.mongodb.util.MongoUtil;
import com.iisquare.fs.base.web.mvc.ServiceBase;
import com.iisquare.fs.web.demo.core.Configuration;
import com.iisquare.fs.web.demo.mongodb.DemoTestMongo;
import com.mongodb.client.model.Sorts;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class MongoService extends ServiceBase {

    @Autowired
    private DemoTestMongo testMongo;
    @Autowired
    private Configuration configuration;

    public Map<?, ?> search(Map<?, ?> param, Map<?, ?> config) {
        Map<String, Object> result = new LinkedHashMap<>();
        int page = ValidateUtil.filterInteger(param.get("page"), true, 1, null, 1);
        int pageSize = ValidateUtil.filterInteger(param.get("pageSize"), true, 1, 500, 15);
        Bson sort = MongoUtil.sort(DPUtil.parseString(param.get("sort")),
                Arrays.asList(MongoCore.FIELD_ID, "createdTime", "updatedTime"));
        if (null == sort) sort = Sorts.descending(MongoCore.FIELD_ID);
        FilterHelper helper = FilterHelper.newInstance(param).dateFormat(configuration.getFormatDate());
        helper.equalWithObjectId(MongoCore.FIELD_ID).equalWithIntNotEmpty("frameId");
        helper.betweenWithDate("createdTime").betweenWithDate("updatedTime");
        helper.equalWithIntNotEmpty("createdUid").equalWithIntNotEmpty("updatedUid");
        Bson filter = helper.filter();
        long total = testMongo.count(filter);
        List<Document> rows = total > 0 ? testMongo.all(filter, sort, page, pageSize) : Arrays.asList();
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("total", total);
        result.put("rows", rows);
        return result;
    }

}
