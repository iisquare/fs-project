package com.iisquare.fs.web.core.mvc;

import feign.hystrix.FallbackFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public abstract class FallbackFactoryBase extends FallbackBase implements FallbackFactory {

    @Override
    public Object create(Throwable cause) {
        Object that = this;
        Class<? extends FallbackFactoryBase> cls = getClass();
        return Proxy.newProxyInstance(cls.getClassLoader(), cls.getInterfaces(), (proxy, method, args) -> {
            String name = method.getName();
            Class<?>[] names = method.getParameterTypes();
            Class<?>[] targets = new Class[names.length + 1];
            for (int i = 0; i < names.length; i++) { targets[i] = names[i]; }
            targets[names.length] = Throwable.class;
            Method target;
            try {
                target = cls.getMethod(name, targets);
                Object[] objs = new Object[args.length + 1];
                for (int i = 0; i < args.length; i++) { objs[i] = args[i]; }
                objs[args.length] = cause;
                args = objs;
            } catch (NoSuchMethodException e) {
                target = cls.getMethod(name, names);
            }
            System.out.println(name + "->" + cause.getMessage());
            return target.invoke(that, args);
        });
    }

}
