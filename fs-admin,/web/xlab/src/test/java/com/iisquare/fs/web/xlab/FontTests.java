package com.iisquare.fs.web.xlab;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.FileUtil;
import com.iisquare.fs.web.xlab.service.FontService;
import org.apache.fontbox.ttf.TrueTypeFont;
import org.junit.Test;

import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class FontTests {

    public String woffBase64(String uri) {
        URL url = getClass().getClassLoader().getResource(uri);
        String content = FileUtil.getContent(url.getFile());
        JsonNode json = DPUtil.parseJSON(content);
        String woffFontBody = json.get("data").get("fontInfo").get("woffFontBody").asText();
        return woffFontBody;
    }

    public JsonNode woffText(String uri) {
        URL url = getClass().getClassLoader().getResource(uri);
        String content = FileUtil.getContent(url.getFile());
        JsonNode json = DPUtil.parseJSON(content);
        return json;
    }

    @Test
    public void woffTest() throws Exception {
        FontService fontService = new FontService();
        TrueTypeFont ttf = fontService.fromWoffBase64(woffBase64("woff-font.json"), "refer-wof.ttf");
        Map<String, BufferedImage> refer = fontService.image(ttf, null, "refer-%d.jpg");
        refer = fontService.reflect(refer, woffText("woff-text.json"));
        TrueTypeFont font = fontService.fromWoffBase64(woffBase64("woff-test.json"), "follow-wof.ttf");
        List<String> intersect = Arrays.asList("f4323", "f4429");
        Map<String, BufferedImage> follow = fontService.image(font, intersect, "follow-%d.jpg");
        Map<String, String> result = fontService.compare(refer, follow, 0.9f);
        System.out.println(DPUtil.stringify(result));
    }

}
