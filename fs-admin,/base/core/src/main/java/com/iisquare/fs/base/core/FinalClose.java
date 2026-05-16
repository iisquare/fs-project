package com.iisquare.fs.base.core;

import com.iisquare.fs.base.core.util.FileUtil;

import java.io.Closeable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public class FinalClose {

    private static volatile FinalClose self;

    private final Map<Closeable, AtomicInteger> instances = new  ConcurrentHashMap<>();

    private FinalClose() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
    }

    public void shutdown() {
        FileUtil.close(instances.keySet().toArray(new Closeable[0]));
    }

    public <T extends Closeable> T register(T instance) {
        instances.computeIfAbsent(instance, k -> new AtomicInteger()).incrementAndGet();
        return instance;
    }

    public int unregister(Closeable instance) {
        AtomicInteger integer = instances.remove(instance);
        FileUtil.close(instance);
        return null == integer ? 0 : integer.get();
    }

    public synchronized <T extends Closeable> T register(Class<T> cls, Function<? super FinalClose, ? extends T> function) {
        for (Map.Entry<Closeable, AtomicInteger> entry : instances.entrySet()) {
            Closeable instance = entry.getKey();
            if (instance.getClass().equals(cls)) {
                return (T) instance;
            }
        }
        if (null == function) return null;
        T instance = function.apply(this);
        if (null == instance) return null;
        instances.put(instance, new AtomicInteger(1));
        return instance;
    }

    public int count(Closeable instance) {
        AtomicInteger integer = instances.get(instance);
        return null == integer ? 0 : integer.get();
    }

    public static FinalClose instance() {
        if (null == self) {
            synchronized (FinalClose.class) {
                if (null == self) {
                    self = new FinalClose();
                }
            }
        }
        return self;
    }

}
