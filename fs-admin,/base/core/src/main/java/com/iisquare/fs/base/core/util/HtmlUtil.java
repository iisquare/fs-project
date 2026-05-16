package com.iisquare.fs.base.core.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HtmlUtil {

    public static final String regexScript = "<[\\s]*?script[^>]*?>[\\s\\S]*?<[\\s]*?\\/[\\s]*?script[\\s]*?>";
    public static final String regexStyle = "<[\\s]*?style[^>]*?>[\\s\\S]*?<[\\s]*?\\/[\\s]*?style[\\s]*?>";
    public static final String regexTag = "<[^>]+>";
    public static final String regexSpace = " |\t";
    public static final String regexLine = "<br\\/?>";

    public static String strip(String content, String... regex) {
        if (null == content || content.length() < 1) return content;
        for (String item : regex) {
            content = replace(item, content, "");
        }
        return content;
    }

    public static String replace(String regex, String content, String replace) {
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher matcher= pattern.matcher(content);
        return matcher.replaceAll(replace);
    }

    /**
     * 全量去除
     */
    public static String format(String content) {
        return strip(content, regexScript, regexStyle, regexTag, regexSpace);
    }

    /**
     * 全量去除，保留换行
     */
    public static String beauty(String content) {
        content = replace(regexLine, content, "\n");
        return strip(content, regexScript, regexStyle, regexTag);
    }

    /**
     * 仅去除脚本
     */
    public static String safe(String content) {
        return strip(content, regexScript);
    }

}
