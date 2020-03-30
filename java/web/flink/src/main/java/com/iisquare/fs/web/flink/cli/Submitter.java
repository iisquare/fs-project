package com.iisquare.fs.web.flink.cli;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.core.util.DPUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;

public class Submitter {

    public static void read(StringBuilder sb, InputStream fis) throws IOException {
        InputStreamReader isr = new InputStreamReader(fis);
        BufferedReader br = new BufferedReader(isr);
        String line = null;
        while ((line = br.readLine()) != null) {
            sb.append(line);
            sb.append("\n");
        }
    }

    public static Object run(JsonNode config) throws Exception {
        List<String> params = new ArrayList<>();
        params.add("run");
        if(config.has("yarn")) {
            params.add("-m");
            params.add("yarn-cluster");
            JsonNode yarn = config.get("yarn");
            if(yarn.has("yarndetached")) { // 单独设置yarn运行方式
                if(yarn.get("yarndetached").asBoolean()) params.add("--yarndetached");
            } else { // 若未设置默认与detached一致
                if(config.has("detached") && config.get("detached").asBoolean()) params.add("--yarndetached");
            }
            if(!yarn.has("yarnstreaming") || yarn.get("yarnstreaming").asBoolean()) {
                params.add("--yarnstreaming"); // Start Flink in streaming mode as default
            }
            if(yarn.has("yarnapplicationId")) {
                params.add("--yarnapplicationId");
                params.add(yarn.get("yarnapplicationId").asText());
            }
            params.add("--yarncontainer");
            params.add(yarn.has("yarncontainer") ? yarn.get("yarncontainer").asText() : (
                config.has("parallelism") ? config.get("parallelism").asText() : "1"
            ));
            if(yarn.has("yarnslots")) {
                params.add("--yarnslots");
                params.add(yarn.get("yarnslots").asText());
            }
            if(yarn.has("yarnjobManagerMemory")) {
                params.add("--yarnjobManagerMemory");
                params.add(yarn.get("yarnjobManagerMemory").asText());
            }
            if(yarn.has("yarntaskManagerMemory")) {
                params.add("--yarntaskManagerMemory");
                params.add(yarn.get("yarntaskManagerMemory").asText());
            }
            if(yarn.has("yarnqueue")) {
                params.add("--yarnqueue");
                params.add(yarn.get("yarnqueue").asText());
            }
            if(yarn.has("taskmanagerMemoryFraction")) {
                params.add("-yD taskmanager.memory.fraction=" + yarn.get("taskmanagerMemoryFraction").asText());
            }
            params.add("--yarnname");
            params.add("flow-" + config.get("appname").asText());
        }
        if(config.has("detached") && config.get("detached").asBoolean()) params.add("-d");
        if(config.has("parallelism")) {
            params.add("-p");
            params.add(config.get("parallelism").asText());
        }
        Iterator<JsonNode> iterator = config.get("jars").elements();
        while (iterator.hasNext()) { // jar文件必须对Flink节点可访问，文件会上传到集群中
            JsonNode item = iterator.next();
            params.add("-C");
            params.add(item.asText());
        }
        params.add("-c");
        params.add(config.get("entry").asText());
        params.add(config.get("jar").asText());
        String command = System.getenv("FLINK_ROOT_DIR");
        if(null == command) {
            iterator = config.get("args").elements();
            while (iterator.hasNext()) {
                JsonNode item = iterator.next();
                params.add(item.asText());
            }
            if(config.get("debug").asBoolean()) return DPUtil.implode(" ", params.toArray(new String[params.size()]));
            // Flink的JobGraph在Cli端进行解析，然后将任务提交给集群
            return Frontend.main(params.toArray(new String[params.size()]));
        } else {
            iterator = config.get("args").elements();
            Base64.Encoder encoder = Base64.getEncoder();
            while (iterator.hasNext()) {
                JsonNode item = iterator.next();
                params.add("'" + encoder.encodeToString(item.asText().getBytes()) + "'");
            }
            command += "/bin/flink";
            List<String> list = new ArrayList<>();
            list.add("su");
            list.add("flink");
            list.add("-c");
            list.add(command + " " + DPUtil.implode(" ", params.toArray(new String[params.size()])));
            params = list;
            if(config.get("debug").asBoolean()) return DPUtil.implode(" ", params.toArray(new String[params.size()]));
            Process process = Runtime.getRuntime().exec(params.toArray(new String[params.size()]));
            StringBuilder sb = new StringBuilder();
            read(sb, process.getErrorStream());
            read(sb, process.getInputStream());
            return sb.toString();
        }
    }

}
