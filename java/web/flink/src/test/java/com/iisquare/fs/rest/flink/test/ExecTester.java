package com.iisquare.fs.rest.flink.test;

import com.iisquare.fs.base.core.util.DPUtil;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class ExecTester {

    public String read(InputStream fis) throws IOException {
        StringBuilder sb = new StringBuilder();
        InputStreamReader isr = new InputStreamReader(fis);
        BufferedReader br = new BufferedReader(isr);
        String line = null;
        while ((line = br.readLine()) != null) {
            sb.append(line);
            sb.append("\n");
        }
        return sb.toString();
    }

    @Test
    public void commandTest() throws IOException {
        List<String> command = new ArrayList<>();
        command.add("jps");
        Process process = Runtime.getRuntime().exec(command.toArray(new String[command.size()]));
        System.out.println("error:");
        System.out.println(read(process.getErrorStream()));
        System.out.println("echo:");
        System.out.println(read(process.getInputStream()));
    }

}
