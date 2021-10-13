package com.iisquare.fs.base.dag.node;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.core.JarClassLoader;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.dag.core.DAGTransform;
import com.iisquare.fs.base.dag.transform.AbstractScriptTransform;

public class ScriptTransformNode extends AbstractScriptTransform {

    JsonNode[] configs;

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
            throw new RuntimeException(String.format("class %s is not instanceof DAGTransform!", pkgClass));
        }
        DAGTransform instance = (DAGTransform) cls.newInstance();
        instance.setId(getId());
        instance.setRunner(getRunner());
        instance.setOptions(getOptions());
        instance.setSources(getSources());
        instance.setTargets(getTargets());
        instance.configure(configs);
        instance.setConfigured(true);
        Object result = instance.process();
        instance.setResult(result);
        instance.setProcessed(true);
        return result;
    }
}
