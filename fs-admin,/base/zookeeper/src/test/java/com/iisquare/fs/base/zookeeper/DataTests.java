package com.iisquare.fs.base.zookeeper;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class DataTests {

    @Test
    public void pathTest() {
        String filename = "/fs/test/1";
        List<String> paths = Arrays.asList("/fs/test", "/fs/test/");
        for (String path : paths) {
            System.out.println(path);
            String name = filename.substring(path.length());
            System.out.println(name);
            System.out.println(name.indexOf("/"));
            System.out.println(name.indexOf("/") < 1);
        }
    }

}
