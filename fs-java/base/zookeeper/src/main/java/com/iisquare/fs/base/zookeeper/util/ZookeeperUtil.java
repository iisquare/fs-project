package com.iisquare.fs.base.zookeeper.util;

import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.CuratorCache;

import java.util.List;

public class ZookeeperUtil {

    public static List<ChildData> children(CuratorCache cache, String path, boolean deep) {
        return cache.stream().filter(child -> {
            if (!child.getPath().startsWith(path)) return false;
            if (deep) return true;
            return child.getPath().substring(path.length()).indexOf('/') < 1;
        }).toList();
    }

}
