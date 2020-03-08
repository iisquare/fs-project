package com.iisquare.fs.web.core.mvc;

import feign.hystrix.FallbackFactory;

public abstract class FallbackFactoryBase<T extends RpcBase> extends FallbackBase implements FallbackFactory<T> {

    @Override
    public T create(Throwable cause) {
        System.out.println(cause.getMessage());
        return (T) this;
    }

}
