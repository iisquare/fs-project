package com.iisquare.fs.ext.flink.util;

import com.iisquare.fs.ext.flink.job.CDCJob;

public class CLIUtil {

    public static String path () {
        String path = CDCJob.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        int index = path.indexOf("/java/ext/flink/");
        if (-1 == index) throw new RuntimeException("path error:" + path);
        path = path.substring(0, index) + "/java/ext/flink/";
        return path;
    }

}
