package com.iisquare.fs.base.calcite;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.calcite.core.CalciteSession;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.FileUtil;

import java.util.Arrays;
import java.util.List;

public class Application {

    public static void main(String[] args) throws Exception {
        List<String> sqlList = Arrays.asList(
                "select * from d1.fs_member_user limit 2",
                "select * from d2.t limit 2",
                "select * from d1.fs_member_role limit 2",
                "select * from d1.fs_member_user u join d2.t t on u.id=t.id limit 2"
        );
        CalciteSession session = new CalciteSession();
        session.mysql("d1", mysql());
        session.mysql("d2", mysql2());
        for (String sql : sqlList) {
            long time = System.currentTimeMillis();
            System.out.println("sql: " + sql);
            System.out.println("data: " + session.query(sql));
            System.out.println("coast: " + (System.currentTimeMillis() - time));
        }
        FileUtil.close(session);
    }

    public static JsonNode mysql() throws Exception {
        ObjectNode config = DPUtil.objectNode();
        config.put("url", "jdbc:mysql://127.0.0.1");
        config.put("username", "root");
        config.put("password", "admin888");
        config.put("database", "fs_project");
        return config;
    }

    public static JsonNode mysql2() throws Exception {
        ObjectNode config = DPUtil.objectNode();
        config.put("url", "jdbc:mysql://127.0.0.1");
        config.put("username", "root");
        config.put("password", "admin888");
        config.put("database", "fs_test");
        return config;
    }

}
