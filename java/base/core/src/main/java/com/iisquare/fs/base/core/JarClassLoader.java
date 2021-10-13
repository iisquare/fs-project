package com.iisquare.fs.base.core;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandlerFactory;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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
        synchronized (getClassLoadingLock(name)) {
            ZipInputStream in = null;
            for (URL url : getURLs()) {
                try {
                    in = new ZipInputStream(url.openStream());
                    ZipEntry ze;
                    while ((ze = in.getNextEntry()) != null) {
                        String classname = ze.getName().replaceFirst("\\.class", "").replaceAll("/", ".");
                        if(!classname.equals(name)) continue;
                        byte[] data = IOUtils.toByteArray(in);
                        return defineClass(name, data, 0, data.length);
                    }
                } catch (IOException e) {
                    throw new ClassNotFoundException(e.getMessage(), e.getCause());
                } finally {
                    IOUtils.closeQuietly(in);
                }
            }
        }
        return super.loadClass(name);
    }

}
