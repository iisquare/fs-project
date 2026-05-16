package com.iisquare.fs.app.nlp;

public class NLPCore {

    public static final String SQL_POLICY = "select base_id, title, content, publich_time from dwd_policy_info where classify_primary='D4F6C59F-2C9F-4A7E-8F57-1BD696F5C010' and doc_no!=''";
    public static final String SQL_POLICY_TEST = "select base_id, title, content from dwd_policy_info where base_id in ('0003aeac-3fbb-4758-8b49-fc379526cf07')";
    public static final String SQL_POLICY_SAMPLE = "select base_id, title, content from t_policy";

}
