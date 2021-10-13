package com.iisquare.fs.app.spark.tester;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.app.spark.SparkApplication;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.dag.util.DAGUtil;
import org.junit.Test;

import java.util.Locale;

public class DAGTester {

    @Test
    public void dateTest() {
        Locale locale = Locale.ENGLISH;
        System.out.println("getLanguage:" + locale.getLanguage());
        System.out.println("getScript:" + locale.getScript());
        System.out.println("toLanguageTag:" + locale.toLanguageTag());
        System.out.println("toString:" + locale.toString());
        System.out.println("getSimpleName:" + locale.getClass().getSimpleName());
    }

    @Test
    public void diagramTest() throws Exception {
        String url = "http://127.0.0.1:7815/dag/diagram?id=3";
        SparkApplication.main(url);
    }

    @Test
    public void optionsTest() throws Exception {
        ObjectNode json = DPUtil.objectNode();
        ArrayNode array = json.putObject("a").putArray("b");
        array.addObject().put("c", 1);
        array.addObject().put("d", "xxx");
        array.addObject().putObject("e").put("f", 3);
        ObjectNode options = DPUtil.objectNode();
        options.put("a", "fs_demo_{}");
        options.put("b", "fs_demo_{a.b.1.d}");
        options.put("c", "fs_demo_{a.b.2.e.f}");
        options.put("d", "{a.b.1.d}{a.b.2.e.f}");
        System.out.println(DAGUtil.formatOptions(options, json));
    }

}
