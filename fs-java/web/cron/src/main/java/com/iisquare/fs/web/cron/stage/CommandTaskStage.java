package com.iisquare.fs.web.cron.stage;

import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.dag.util.DAGUtil;
import com.iisquare.fs.web.cron.core.Stage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

public class CommandTaskStage extends Stage {

    public static void read(StringBuilder sb, InputStream fis, String charset) throws IOException {
        InputStreamReader isr = DPUtil.empty(charset) ? new InputStreamReader(fis) : new InputStreamReader(fis, charset);
        BufferedReader br = new BufferedReader(isr);
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line);
            sb.append("\n");
        }
    }

    @Override
    public Map<String, Object> call() throws Exception {
        options = DAGUtil.formatOptions(options, config);
        String charset = options.at("/charset").asText();
        String command = options.at("/command").asText();
        if (DPUtil.empty(command)) {
            return ApiUtil.result(1001, "指令不能为空", "command empty!");
        }
        Process process = Runtime.getRuntime().exec(command);
        StringBuilder sb = new StringBuilder();
        read(sb, process.getInputStream(), charset);
        read(sb, process.getErrorStream(), charset);
        sb.append("\nexit: ").append(process.exitValue());
        return ApiUtil.result(0, null, sb.toString());
    }
}
