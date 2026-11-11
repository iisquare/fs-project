package com.iisquare.fs.base.core;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandlerFactory;

public class JarClassLoader extends URLClassLoader {

    public static JarClassLoader fromUrl(URL url, ClassLoader parent) {
        return new JarClassLoader(new URL[]{url}, parent);
    }

    public static JarClassLoader fromUrl(String url, ClassLoader parent) throws MalformedURLException {
        return new JarClassLoader(new URL[]{new URL(url)}, parent);
    }
    
    public JarClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    public JarClassLoader(URL[] urls) {
        super(urls);
    }

    public JarClassLoader(URL[] urls, ClassLoader parent, URLStreamHandlerFactory factory) {
        super(urls, parent, factory);
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        // 标准双亲委派：框架的 DAGNode/DAGType 必须与宿主使用同一 Class 实例，
        // 插件 JAR 即使携带完整依赖，也不能重新定义这些基础类。
        return super.loadClass(name);
    }

}
