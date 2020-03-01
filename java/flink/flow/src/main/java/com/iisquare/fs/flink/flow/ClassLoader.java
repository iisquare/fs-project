package com.iisquare.fs.flink.flow;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandlerFactory;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ClassLoader extends URLClassLoader {
    public ClassLoader(URL[] urls, java.lang.ClassLoader parent) {
        super(urls, parent);
    }

    public ClassLoader(URL[] urls) {
        super(urls);
    }

    public ClassLoader(URL[] urls, java.lang.ClassLoader parent, URLStreamHandlerFactory factory) {
        super(urls, parent, factory);
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            ZipInputStream zipin = null;
            for (URL url : getURLs()) {
                try {
                    zipin = new ZipInputStream(url.openStream());
                    ZipEntry ze = null;
                    while ((ze = zipin.getNextEntry()) != null) {
                        String classname = ze.getName().replaceFirst("\\.class", "").replaceAll("/", ".");
                        if(!classname.equals(name)) continue;
                        byte[] data = IOUtils.toByteArray(zipin);
                        return defineClass(name, data, 0, data.length);
                    }
                } catch (IOException e) {
                    throw new ClassNotFoundException(e.getMessage(), e.getCause());
                } finally {
                    IOUtils.closeQuietly(zipin);
                }
            }
        }
        return super.loadClass(name);
    }

}
