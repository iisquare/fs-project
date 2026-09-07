package com.iisquare.fs.web.bi.core;

public class RedisKey {

    public static String trinoCatalogReloadLock() {
        return "fs:bi:trino:catalog:reload";
    }

    public static String datasetReloadLock() {
        return "fs:bi:dataset:reload";
    }
}
