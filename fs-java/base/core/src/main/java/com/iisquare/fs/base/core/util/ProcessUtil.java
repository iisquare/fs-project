package com.iisquare.fs.base.core.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 外部进程调用工具，用于通过命令行向远程集群提交作业（flink run / spark-submit）。
 */
public class ProcessUtil {

    public static class Result {
        private final int exitCode;
        private final String output;
        private final boolean timeout;

        public Result(int exitCode, String output, boolean timeout) {
            this.exitCode = exitCode;
            this.output = output;
            this.timeout = timeout;
        }

        public int exitCode() {
            return exitCode;
        }

        public String output() {
            return output;
        }

        public boolean timeout() {
            return timeout;
        }

        public boolean succeed() {
            return !timeout && 0 == exitCode;
        }

        @Override
        public String toString() {
            return output;
        }
    }

    public static Result execute(List<String> command, File workDir, long timeoutSeconds, String charset) {
        ProcessBuilder builder = new ProcessBuilder(new ArrayList<>(command));
        if (null != workDir) builder.directory(workDir);
        builder.redirectErrorStream(true);
        Charset cs = DPUtil.empty(charset) ? Charset.defaultCharset() : Charset.forName(charset);
        Process process = null;
        StringBuilder output = new StringBuilder();
        try {
            process = builder.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), cs))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return new Result(-1, output.toString(), true);
            }
            return new Result(process.exitValue(), output.toString(), false);
        } catch (Exception e) {
            return new Result(-1, output + e.getClass().getSimpleName() + "：" + e.getMessage(), false);
        } finally {
            if (null != process && process.isAlive()) process.destroyForcibly();
        }
    }

}
