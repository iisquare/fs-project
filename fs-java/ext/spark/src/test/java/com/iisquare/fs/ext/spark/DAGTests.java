package com.iisquare.fs.ext.spark;

import com.iisquare.fs.app.spark.SparkApplication;
import org.junit.Test;

public class DAGTests {

    @Test
    public void diagramTest() throws Exception {
        String url = "http://127.0.0.1:7815/dag/diagram?id=2";
        SparkApplication.main(url);
    }

}
