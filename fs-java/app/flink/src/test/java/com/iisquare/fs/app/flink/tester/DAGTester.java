package com.iisquare.fs.app.flink.tester;

import com.iisquare.fs.app.flink.FlinkApplication;
import org.junit.Test;

public class DAGTester {

    @Test
    public void diagramTest() throws Exception {
        String url = "http://127.0.0.1:7815/dag/diagram?id=7";
        FlinkApplication.main(url);
    }

    @Test
    public void mysqlCDCTest() throws Exception {
        String url = "http://127.0.0.1:7815/dag/diagram?id=9";
        FlinkApplication.main(url);
    }

}
