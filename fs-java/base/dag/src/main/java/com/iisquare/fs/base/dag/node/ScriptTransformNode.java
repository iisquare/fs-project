package com.iisquare.fs.base.dag.node;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.core.JarClassLoader;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.dag.core.DAGException;
import com.iisquare.fs.base.dag.core.DAGTransform;
import com.iisquare.fs.base.dag.core.DAGType;
import com.iisquare.fs.base.dag.transform.AbstractScriptTransform;

/**
 * 脚本转换节点：加载外部插件（DAGTransform 子类）并执行。
 *
 * 插件输出类型在运行期才能确定，故本节点声明为 OBJECT，由下游节点自行做类型校验。
 */
public class ScriptTransformNode extends AbstractScriptTransform<Object> {

    private JsonNode[] configs;

    @Override
    public DAGType<Object> outputType() {
        return DAGType.OBJECT;
    }

    @Override
    public boolean configure(JsonNode... configs) {
        if (!super.configure(configs)) return false;
        this.configs = configs;
        return true;
    }

    @Override
    public Object process() throws Exception {
        String jarURI = options.at("/jarURI").asText();
        String pkgClass = options.at("/pkgClass").asText();
        Class<?> cls;
        if (DPUtil.empty(jarURI)) {
            cls = Class.forName(pkgClass);
        } else {
            cls = JarClassLoader.fromUrl(jarURI, getClass().getClassLoader()).loadClass(pkgClass);
        }
        if (!DAGTransform.class.isAssignableFrom(cls)) {
            throw new DAGException(this, String.format("class %s is not instanceof DAGTransform!", pkgClass));
        }
        DAGTransform<?> instance = (DAGTransform<?>) cls.getDeclaredConstructor().newInstance();
        instance.setId(getId());
        instance.setName(getName());
        instance.setParallelism(getParallelism());
        instance.setRunner(getRunner());
        instance.setOptions(getOptions());
        instance.getSources().addAll(getSources());
        instance.getTargets().addAll(getTargets());
        instance.configure(configs);
        return instance.process();
    }
}
