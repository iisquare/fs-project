package com.iisquare.fs.web.demo.service;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.mongodb.MongoCore;
import com.iisquare.fs.base.mongodb.helper.FilterHelper;
import com.iisquare.fs.base.mongodb.util.FindUtil;
import com.iisquare.fs.base.web.mvc.ServiceBase;
import com.iisquare.fs.web.demo.core.Configuration;
import com.iisquare.fs.web.demo.mongodb.DemoTestMongo;
import org.bson.conversions.Bson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Map;

@Service
public class MongoService extends ServiceBase {

    @Autowired
    DemoTestMongo testMongo;
    @Autowired
    Configuration configuration;

    public ObjectNode search(Map<?, ?> param, Map<?, ?> config) {
        FilterHelper helper = FilterHelper.newInstance(param).dateFormat(configuration.getFormatDate());
        helper.equalWithObjectId(MongoCore.FIELD_ID).equalWithIntNotEmpty("frameId");
        helper.betweenWithDate("createdTime").betweenWithDate("updatedTime");
        helper.equalWithIntNotEmpty("createdUid").equalWithIntNotEmpty("updatedUid");
        Bson filter = helper.filter();
        ObjectNode result = FindUtil.search(testMongo, (Map<String, Object>) param, filter,
                MongoCore.FIELD_ID + ".desc", Arrays.asList(MongoCore.FIELD_ID, "createdTime", "updatedTime"));
        return result;
    }

}
