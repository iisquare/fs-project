package com.iisquare.fs.web.oa.mongodb;

import com.iisquare.fs.base.mongodb.helper.FiltrationHelper;
import com.iisquare.fs.base.mongodb.mvc.MongoBase;
import org.bson.Document;
import org.springframework.stereotype.Component;

@Component
public class FormDataMongo extends MongoBase {

    public FormDataMongo() {
        this.database = "fs_project";
        this.table = "fs.oa.form.data";
    }

    @Override
    public Document filtration(Document document) {
        FiltrationHelper helper = FiltrationHelper.newInstance(document).id();
        helper.useInteger("frameId", 0); // 所属表单
        helper.useString("bpmWorkflowId", ""); // 流程模型
        helper.useString("bpmInstanceId", ""); // 流程实例
        helper.useString("bpmStartUserId", ""); // 流程发起人
        helper.useLong("createdTime", 0L);
        helper.useInteger("createdUid", 0);
        helper.useLong("updatedTime", 0L);
        helper.useInteger("updatedUid", 0);
        document.putAll(helper.result());
        return document;
    }

}
