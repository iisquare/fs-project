package com.iisquare.fs.ext.flink;

import com.iisquare.fs.app.flink.FlinkApplication;
import org.junit.Test;

public class DAGTests {

    @Test
    public void diagramTest() throws Exception {
        String url = "http://127.0.0.1:7815/dag/diagram?id=2";
        FlinkApplication.main(url);
    }

}
