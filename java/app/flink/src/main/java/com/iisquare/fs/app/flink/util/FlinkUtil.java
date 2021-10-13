package com.iisquare.fs.app.flink.util;

import com.iisquare.fs.base.dag.core.DAGNode;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.streaming.api.datastream.DataStream;

import java.util.Iterator;
import java.util.Set;

public class FlinkUtil {

    public static <T> T union(Class<T> classType, Set<DAGNode> nodes) {
        if (null == nodes || 0 == nodes.size()) return null;
        Iterator<DAGNode> iterator = nodes.iterator();
        T result = iterator.next().result(classType);
        while (iterator.hasNext()) {
            if (result instanceof DataSet) {
                result = (T) ((DataSet) result).union(iterator.next().result(DataSet.class));
                continue;
            }
            if (result instanceof DataStream) {
                result = (T) ((DataStream) result).union(iterator.next().result(DataStream.class));
                continue;
            }
            return null;
        }
        return result;
    }

}
