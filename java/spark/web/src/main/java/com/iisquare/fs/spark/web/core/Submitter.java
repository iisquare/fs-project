package com.iisquare.fs.spark.web.core;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.spark.deploy.SparkSubmit;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
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

    public static String exec(String... args) throws IOException {
        Process process = Runtime.getRuntime().exec(args);
        StringBuilder sb = new StringBuilder();
        read(sb, process.getErrorStream());
        read(sb, process.getInputStream());
        return sb.toString();
    }

    public static void kubernetes(JsonNode config) {
        List<String> params = new ArrayList<>();
        params.add("--master ");
        params.add("k8s://" + config.at("/master").asText("https://127.0.0.1:6443"));
        params.add("--deploy-mode cluster");
        params.add("--class " + config.at("/entry").asText());
        params.add("--conf spark.default.parallelism=");
        params.add(String.valueOf(config.at("/parallelism").asInt(1)));
        params.add("--conf spark.kubernetes.submission.waitAppCompletion=");
        params.add(config.at("/detached").asBoolean(true) ? "false" : "true");
        params.add("--conf spark.kubernetes.namespace=spark");
        params.add("--conf spark.kubernetes.authenticate.driver.serviceAccountName=spark");
        params.add("--conf spark.kubernetes.container.image=harbor.iisquare.com/library/spark:2.4.7");
        params.add("--conf spark.kubernetes.driver.volumes.hostPath.data.mount.path=/data");
        params.add("--conf spark.kubernetes.driver.volumes.hostPath.data.mount.readOnly=false");
        params.add("--conf spark.kubernetes.driver.volumes.hostPath.data.options.path=/data/nfs/spark");
        params.add("--conf spark.kubernetes.executor.volumes.hostPath.data.mount.path=/data");
        params.add("--conf spark.kubernetes.executor.volumes.hostPath.data.mount.readOnly=false");
        params.add("--conf spark.kubernetes.executor.volumes.hostPath.data.options.path=/data/nfs/spark");
        if (config.has("jars")) {
            params.add("--jars ");
            params.add(config.get("jars").asText());
        }
        params.add(config.at("/jar").asText());
        Iterator<JsonNode> iterator = config.get("args").elements();
        while (iterator.hasNext()) params.add(iterator.next().asText());
        SparkSubmit.main(params.toArray(new String[params.size()]));
    }

}
