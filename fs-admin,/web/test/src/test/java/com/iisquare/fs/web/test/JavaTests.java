package com.iisquare.fs.web.test;

import org.junit.Test;

public class JavaTests {

    @Test
    public void switchTest() {
        String value = null;
        switch (value) { // will throw:java.lang.NullPointerException
            case "":
                System.out.println("value empty");
                break;
            default:
                System.out.println("value:" + value);
        }
    }

}
