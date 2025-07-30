package com.iisquare.fs.web.site.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.FileUtil;
import com.iisquare.fs.web.site.mvc.SiteConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Iterator;
import java.util.Map;

@Component
public class Assets implements InitializingBean {

    @Autowired
    protected SiteConfiguration siteConfiguration;

    private String fileUrl;
    private String staticUrl;
    private ObjectNode assets;
    protected final static Logger logger = LoggerFactory.getLogger(Assets.class);

    @Override
    public void afterPropertiesSet() throws Exception {
        fileUrl = siteConfiguration.getUrls().get("file");
        staticUrl = siteConfiguration.getUrls().get("static");
        try {
            File file = ResourceUtils.getFile(ResourceUtils.CLASSPATH_URL_PREFIX + "assets.json");
            JsonNode json = DPUtil.parseJSON(FileUtil.getContent(file));
            ObjectNode assets = DPUtil.objectNode();
            Iterator<Map.Entry<String, JsonNode>> iterator = json.fields();
            while (iterator.hasNext()) {
                Map.Entry<String, JsonNode> entry = iterator.next();
                Iterator<Map.Entry<String, JsonNode>> fields = entry.getValue().fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> item = fields.next();
                    assets.put("/" + entry.getKey() + "." + item.getKey(), "/dest/" + item.getValue().asText());
                }
            }
            this.assets = assets;
        } catch (FileNotFoundException e) {
            logger.error("load assets.json failed!", e);
        }
    }

    public Map<String, String> urls() {
        return siteConfiguration.getUrls();
    }

    public String asset(String uri) {
        String asset = uri, search = "";
        int index = uri.indexOf("?");
        if (-1 != index) {
            asset = uri.substring(0, index);
            search = uri.substring(index);
        }
        if (!assets.has(asset)) return uri;
        return assets.get(asset).asText() + search;
    }

    /**
     * 获取编译打包的CSS路径
     */
    public String css(String uri) {
        return staticUrl + asset(uri);
    }

    /**
     * 获取编译打包的JS路径
     */
    public String js(String uri) {
        return staticUrl + asset(uri);
    }

    /**
     * 获取图床图片路径
     */
    public String img(String uri) {
        return fileUrl + uri;
    }

    /**
     * 获取图床图片路径
     */
    public String img(String id, int width, int height, String clip) {
        if (DPUtil.empty(clip)) {
            clip = "";
        } else if ("cc".equals(clip)) {
            clip = ",c_";
        } else {
            clip = ",c_" + clip;
        }
        return img(String.format("/image/%d-w_%d,h_%d%s.jpg", id, width, height, clip));
    }

    /**
     * 获取存储文件路径
     */
    public String file(String uri) {
        return fileUrl + uri;
    }

    /**
     * 获取静态资源（非编译的CSS、JS、图片等）
     */
    public String res(String uri) {
        return staticUrl + uri;
    }

}
