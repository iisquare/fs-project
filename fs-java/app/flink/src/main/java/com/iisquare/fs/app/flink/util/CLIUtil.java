package com.iisquare.fs.app.flink.util;

public class CLIUtil {

    public static String path () {
        String path = CLIUtil.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        int index = path.indexOf("/java/app/flink/");
        if (-1 == index) throw new RuntimeException("path error:" + path);
        path = path.substring(0, index) + "/java/app/flink/";
        return path;
    }

}
