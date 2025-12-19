package com.iisquare.fs.base.core;

import org.junit.Test;

import java.io.Closeable;
import java.io.IOException;

public class FunTests implements Closeable {

    @Test
    public void closeTest() {
        FinalClose.instance().register(FunTests.class, f -> this);
    }

    @Override
    public void close() throws IOException {
        System.out.println("FuncTester.close()");
    }
}
