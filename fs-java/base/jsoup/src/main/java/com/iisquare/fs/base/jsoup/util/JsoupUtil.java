package com.iisquare.fs.base.jsoup.util;

import com.iisquare.fs.base.core.util.DPUtil;
import org.jsoup.nodes.Document;
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

}
