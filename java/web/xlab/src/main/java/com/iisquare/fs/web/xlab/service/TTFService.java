package com.iisquare.fs.web.xlab.service;

import com.iisquare.fs.base.core.util.FileUtil;
import com.iisquare.fs.base.web.mvc.ServiceBase;
import org.apache.commons.codec.binary.Base64;
import org.apache.fontbox.ttf.CmapSubtable;
import org.apache.fontbox.ttf.TTFParser;
import org.apache.fontbox.ttf.TrueTypeFont;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Service
public class TTFService extends ServiceBase {

    protected final static Logger logger = LoggerFactory.getLogger(TTFService.class);

    /**
     * 字体在线编辑工具@see(http://fontstore.baidu.com/static/editor/index.html)
     */
    public Map<String, Integer> anjukeBase64(String base64) {
        base64 = base64.replaceFirst("^.*?,", "");
        InputStream in = new ByteArrayInputStream(Base64.decodeBase64(base64));
        TTFParser parser = new TTFParser();
        Map<String, Integer> dict = new HashMap<>();
        try {
            TrueTypeFont ttf = parser.parse(in);
            if (ttf != null && ttf.getCmap() != null && ttf.getCmap().getCmaps() != null && ttf.getCmap().getCmaps().length > 0) {
                CmapSubtable[] tables = ttf.getCmap().getCmaps();
                CmapSubtable table = tables[0];// No matter what
                for (int i = 1; i <= 10; i++) {
                    dict.put(String.valueOf((char) (int) (table.getCharacterCode(i))), i - 1);
                }
            }
            return dict;
        } catch (IOException e) {
            logger.warn("parse ttf failed", e);
            return null;
        } finally {
            FileUtil.close(in);
        }
    }

}
