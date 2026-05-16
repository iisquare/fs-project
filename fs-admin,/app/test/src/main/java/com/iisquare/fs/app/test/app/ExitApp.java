package com.iisquare.fs.app.test.app;

public class ExitApp {
    public static void main(String[] args) {
        try {
            System.out.println("a");
            System.exit(0);
            System.out.println("e");
        } catch (Exception e) {
            System.out.println("b");
        } finally {
            System.out.println("c");
        }
    }
}
