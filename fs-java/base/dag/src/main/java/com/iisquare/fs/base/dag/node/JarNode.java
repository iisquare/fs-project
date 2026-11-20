package com.iisquare.fs.base.dag.node;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.core.JarClassLoader;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.dag.core.DAGConfig;
import com.iisquare.fs.base.dag.core.DAGException;
import com.iisquare.fs.base.dag.core.DAGNode;
import com.iisquare.fs.base.dag.core.DAGSink;
import com.iisquare.fs.base.dag.core.DAGSource;
import com.iisquare.fs.base.dag.core.DAGTransform;
import com.iisquare.fs.base.dag.core.DAGType;

/**
 * JAR 作业节点：加载外部 DAGSource、DAGTransform 或 DAGSink 并执行。
 *
 * 作业输出类型在运行期才能确定，故本节点声明为 OBJECT，由下游节点自行做类型校验。
 */
public class JarNode extends DAGTransform<Object> {

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
        if (DPUtil.empty(pkgClass)) {
            throw new DAGException(this, "缺少作业类名（pkgClass）");
        }
        Class<?> cls;
        try {
            if (DPUtil.empty(jarURI)) {
                cls = loadLocalClass(pkgClass);
            } else {
                cls = JarClassLoader.fromUrl(jarURI, getClass().getClassLoader()).loadClass(pkgClass);
            }
        } catch (Exception e) {
            throw new DAGException(this, "加载执行类（" + pkgClass + "）失败", e);
        }
        if (!DAGTransform.class.isAssignableFrom(cls)) {
            if (DAGConfig.class.isAssignableFrom(cls)) {
                throw new DAGException(this, "JarNode 不支持 DAGConfig：" + pkgClass);
            }
            if (!DAGSource.class.isAssignableFrom(cls) && !DAGSink.class.isAssignableFrom(cls)) {
                throw new DAGException(this, "不支持外部节点类型：" + pkgClass);
            }
        }
        DAGNode<?> instance = (DAGNode<?>) cls.getDeclaredConstructor().newInstance();
        instance.setId(getId());
        instance.setName(getName());
        instance.setParallelism(getParallelism());
        instance.setRunner(getRunner());
        instance.setOptions(getOptions());
        instance.getSources().addAll(getSources());
        instance.getTargets().addAll(getTargets());
        if (instance instanceof DAGSource<?> source) {
            source.setKvConfigPrefix(getKvConfigPrefix());
        } else if (instance instanceof DAGTransform<?> transform) {
            transform.setKvConfigPrefix(getKvConfigPrefix());
        } else if (instance instanceof DAGSink<?> sink) {
            sink.setKvConfigPrefix(getKvConfigPrefix());
        }
        if (!instance.configure(configs)) {
            throw new DAGException(this, "外部节点配置失败：" + pkgClass);
        }
        return instance.process();
    }

    /**
     * 本地调试时优先使用线程上下文类加载器，以兼容 IDE 测试和 Spark user classloader。
     */
    private Class<?> loadLocalClass(String pkgClass) throws ClassNotFoundException {
        ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
        ClassLoader nodeLoader = getClass().getClassLoader();
        if (null != contextLoader) {
            try {
                return Class.forName(pkgClass, true, contextLoader);
            } catch (ClassNotFoundException ignored) {
                // 回退到节点框架自身的类加载器
            }
        }
        return Class.forName(pkgClass, true, nodeLoader);
    }
}
