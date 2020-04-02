package com.iisquare.fs.flink.test;

import com.iisquare.fs.flink.FlowApplication;

public class FlowTester {

    public static void main(String[] args) throws Exception {
        String url = "http://127.0.0.1:7096/flow/plain/?flowId=1";
        FlowApplication.main(new String[]{url});
    }

}
