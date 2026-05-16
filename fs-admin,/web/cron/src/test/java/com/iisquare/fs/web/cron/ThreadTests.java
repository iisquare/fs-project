package com.iisquare.fs.web.cron;

import com.iisquare.fs.web.cron.stage.CommandTaskStage;
import org.junit.Test;

import java.io.IOException;
import java.util.Date;

public class ThreadTests {

    @Test
    public void interruptTest() {
        Thread thread = new Thread() {
            @Override
            public void run() {
                while (true) {
                    System.out.println(new Date() + "->" + isInterrupted());
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e) {
                        System.out.println("interrupt...");
                        e.printStackTrace();
                        break; // 等待线程接收到中断信号之后，状态会重置，需要主动退出
                    }
                }
            }
        };
        thread.start();
        System.out.println("before isAlive:" + thread.isAlive());
        System.out.println("before isDaemon:" + thread.isDaemon());
        System.out.println("before isInterrupted:" + thread.isInterrupted());
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            System.out.println("before...");
            e.printStackTrace();
        }
        thread.interrupt(); // 发送中断信号
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            System.out.println("after...");
            e.printStackTrace();
        }
        System.out.println("after isAlive:" + thread.isAlive());
        System.out.println("after isDaemon:" + thread.isDaemon());
        System.out.println("after isInterrupted:" + thread.isInterrupted());
    }

    @Test
    public void cmdTest() throws IOException, InterruptedException {
        String charset = "gbk";
        String command = "help";
        Process process = Runtime.getRuntime().exec(command);
        StringBuilder sb = new StringBuilder();
        CommandTaskStage.read(sb, process.getInputStream(), charset);
        CommandTaskStage.read(sb, process.getErrorStream(), charset);
        sb.append("\nexit: ").append(process.exitValue());
        System.out.println(sb.toString());
    }

}
