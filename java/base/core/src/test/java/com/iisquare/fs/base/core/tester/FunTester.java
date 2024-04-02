package com.iisquare.fs.base.core.tester;

import com.iisquare.fs.base.core.FinalClose;
import org.junit.Test;

import java.io.Closeable;
import java.io.IOException;

public class FunTester implements Closeable {

    @Test
    public void closeTest() {
        FinalClose.instance().register(FunTester.class, f -> this);
    }

    @Override
    public void close() throws IOException {
        System.out.println("FuncTester.close()");
    }
}
