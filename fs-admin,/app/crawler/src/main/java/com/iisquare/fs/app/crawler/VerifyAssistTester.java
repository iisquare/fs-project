package com.iisquare.fs.app.crawler;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.app.crawler.assist.Assist;
import com.iisquare.fs.app.crawler.assist.VerifyAssist;
import com.iisquare.fs.app.crawler.helper.XlabHelper;

import java.io.IOException;

public class VerifyAssistTester {

    public static void main(String[] args) {
        System.setProperty("webdriver.chrome.driver", args[0]);
        XlabHelper.server = "http://127.0.0.1:8714";
        VerifyAssist.headless = true;
        ObjectNode config = DPUtil.objectNode();
        config.put("url", "https://www.anjuke.com/captcha-verify/?callback=shield&from=antispam&serialID=4c61a8b6748bf9cae6b7f8fd8447af8f_c132dadca844491c94636e30d406694e&history=aHR0cHM6Ly9zaGFuZ2hhaS5hbmp1a2UuY29tL3NhbGUv");
        Assist assist = Assist.assist("verify", config);
        try {
            if (assist.open()) {
                try {
                    System.out.println("result:" + assist.run());
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    assist.close();
                }
            } else {
                System.out.println("尝试打开失败");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("done");
    }

}
