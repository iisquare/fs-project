package com.iisquare.fs.app.flink;

import org.junit.Test;

public class DAGTests {

    @Test
    public void diagramTest() throws Exception {
        FlinkApplication.main("http://127.0.0.1:7815/dag/diagram?id=7&execution=true");
    }

    @Test
    public void mysqlCDCTest() throws Exception {
        FlinkApplication.main("http://127.0.0.1:7815/dag/diagram?id=9&execution=true");
    }

}
