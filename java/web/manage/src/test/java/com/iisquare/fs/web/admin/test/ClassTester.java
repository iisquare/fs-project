package com.iisquare.fs.web.admin.test;

import org.junit.Test;

import java.util.*;

public class ClassTester {

    @Test
    public void nameTest() {
        System.out.println(ClassTester.class.getName());
        System.out.println(ClassTester.class.getSimpleName());
    }

    @Test
    public void arrayTest() {
        System.out.println(Arrays.asList(new String[]{"124", "fsafasf"}));
    }

    @Test
    public void spliteTest() {
        System.out.println("asfasf$$asfafs$$asf".split("\\$\\$")[0]);
    }
}
