package com.iisquare.fs.base.jsoup.util;

import com.iisquare.fs.base.core.util.DPUtil;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class JsoupUtil {

    public static String title(Document doc) {
        String title = doc.title();
        if (DPUtil.empty(title)) {
            Elements meta = doc.select("meta[property=og:title]");
            if (!meta.isEmpty()) {
                title = meta.attr("content");
            }
        }
        return title;
    }

    public static String description(Document doc) {
        String description = "";
        Elements meta = doc.select("meta[name=description]");
        if (!meta.isEmpty()) {
            description = meta.attr("content");
        }
        if (!DPUtil.empty(description)) return description;
        meta = doc.select("meta[name=Description]");
        if (!meta.isEmpty()) {
            description = meta.attr("content");
        }
        if (!DPUtil.empty(description)) return description;
        meta = doc.select("meta[property=og:description]");
        if (!meta.isEmpty()) {
            description = meta.attr("content");
        }
        return description;
    }

    public static String keywords(Document doc) {
        String keywords = "";
        Elements meta = doc.select("meta[name=keywords]");
        if (!meta.isEmpty()) {
            keywords = meta.attr("content");
        }
        if (!DPUtil.empty(keywords)) return keywords;
        meta = doc.select("meta[name=Keywords]");
        if (!meta.isEmpty()) {
            keywords = meta.attr("content");
        }
        if (!DPUtil.empty(keywords)) return keywords;
        meta = doc.select("meta[property=og:keywords]");
        if (!meta.isEmpty()) {
            keywords = meta.attr("content");
        }
        return keywords;
    }

    public static String charset(Document doc) {
        // 方法1：从charset属性获取
        Element charsetMeta = doc.select("meta[charset]").last();
        if (charsetMeta != null) {
            String charset = charsetMeta.attr("charset");
            if (!DPUtil.empty(charset)) return charset;
        }
        // 方法2：从http-equiv获取
        Element httpEquivMeta = doc.select("meta[http-equiv=Content-Type]").first();
        if (httpEquivMeta != null) {
            String content = httpEquivMeta.attr("content");
            if (content.contains("charset=")) {
                String charset = content.split("charset=")[1].trim();
                if (!DPUtil.empty(charset)) return charset;
            }
        }
        // 方法3：检查所有可能的meta标签
        String[] possibleAttrs = {"charset", "charset", "encoding"};
        String[] possibleHttpEquivs = {"Content-Type", "content-type"};
        for (Element meta : doc.select("meta")) {
            // 检查charset属性
            for (String attr : possibleAttrs) {
                if (meta.hasAttr(attr)) {
                    String charset = meta.attr(attr);
                    if (!DPUtil.empty(charset)) return charset;
                }
            }
            // 检查http-equiv
            String httpEquiv = meta.attr("http-equiv");
            if (!httpEquiv.isEmpty()) {
                for (String equiv : possibleHttpEquivs) {
                    if (httpEquiv.equalsIgnoreCase(equiv)) {
                        String content = meta.attr("content");
                        if (content.contains("charset=")) {
                            String charset = content.split("charset=")[1].split(";")[0].trim();
                            if (!DPUtil.empty(charset)) return charset;
                        }
                    }
                }
            }
        }
        return "";
    }

}
