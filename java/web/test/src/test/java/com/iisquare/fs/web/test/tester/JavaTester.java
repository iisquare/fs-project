package com.iisquare.fs.web.test.tester;

import org.junit.Test;

public class JavaTester {

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
