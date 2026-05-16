package com.iisquare.fs.app.crawler;

import com.iisquare.fs.base.core.util.FileUtil;
import com.iisquare.fs.app.crawler.helper.XlabHelper;
import com.iisquare.fs.app.crawler.tool.AnjukeTTFTool;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

public class ToolsTests {

    @Test
    public void mmTest() {
        System.out.println(2^1);
    }

    @Test
    public void ttfTest() {
        XlabHelper.server = "http://127.0.0.1:8714";
        String base64 = FileUtil.getContent(getClass().getClassLoader().getResource("ttf-base64.txt"), false, StandardCharsets.UTF_8);
        AnjukeTTFTool tool = new AnjukeTTFTool();
        tool.loadBase64(base64);
        System.out.println(tool.dict());
        System.out.println(tool.parse("齤驋麣麣万"));
    }

}
