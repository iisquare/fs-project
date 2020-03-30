package com.iisquare.fs.rest.flink.test;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.web.flink.cli.Submitter;
import com.iisquare.fs.base.core.util.DPUtil;

import java.io.File;

public class SubmitTester {

    public static void main(String[] args) throws Exception {
        String nfs = "http://127.0.0.1/fs-project/web/flink/plugins";
        ObjectNode config = DPUtil.objectNode();
        ArrayNode jars = DPUtil.arrayNode();
        File pluginsDir = new File("cloud-rest/flink/plugins");
        if(pluginsDir.isDirectory()) {
            for (File file : pluginsDir.listFiles()) {
                if(!file.isDirectory()) continue;
                for (File jar : file.listFiles()) {
                    if(!jar.getName().endsWith(".jar")) continue;
//                    jars.add("file://" + jar.getAbsolutePath());
                    jars.add(DPUtil.implode("/", new String[]{nfs, file.getName(), jar.getName()}));
                }
            }
        }
        config.replace("jars", jars);
        config.put("entry", "com.iisquare.fs.flink.FlowApplication");
        config.put("jar", "cloud-rest/flink/plugins/app.jar");
        String url = "http://10.207.9.203:7096/flow/plain/?flowId=4";
        config.replace("args", DPUtil.arrayNode().add(url));
        config.put("debug", false);
        Submitter.run(config);
    }

}
