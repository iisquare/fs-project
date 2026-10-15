package com.iisquare.fs.app.spark.script;

import com.iisquare.fs.base.dag.core.DAGTransform;
import com.iisquare.fs.base.dag.core.DAGType;

/**
 * 脚本插件示例：输出类型由插件实现决定
 */
public class SQLScript extends DAGTransform<Object> {

    @Override
    public DAGType<Object> outputType() {
        return DAGType.OBJECT;
    }

    @Override
    public Object process() throws Exception {
        return null;
    }
}
