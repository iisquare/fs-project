package com.iisquare.fs.base.core;

import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.PrintStream;

public class SystemTests {

    @Test
    public void outTest() throws FileNotFoundException {
        System.setOut(new PrintStream("sys.log"));
        System.out.println("System:");
        System.out.printf("%s: %d", "a", 1);
        String line = String.format("%s: %d", "b", 2);
        System.out.println(line);
    }

}
