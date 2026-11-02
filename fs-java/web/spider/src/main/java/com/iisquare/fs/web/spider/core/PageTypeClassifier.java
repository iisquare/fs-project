package com.iisquare.fs.web.spider.core;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 基于静态 DOM 的页面类型判定。
 *
 * 不依赖 URL 模板，优先使用重复链接块、分页控件、正文段落和文章元数据等结构信号。
 */
public final class PageTypeClassifier {

    public static final String LIST = "list";
    public static final String DETAIL = "detail";
    public static final String OTHER = "other";

    private PageTypeClassifier() {
    }

    public static String classify(Document document) {
        if (null == document || null == document.body()) return OTHER;

        String pageText = normalizedText(document.body().text());
        if (looksLikeErrorPage(document, pageText) || looksLikeFormPage(document, pageText)) {
            return OTHER;
        }

        int repeatedLinkGroups = maxRepeatedLinkGroups(document);
        int contentLinks = contentLinkCount(document);
        boolean pagination = hasPagination(document);
        boolean itemList = hasItemList(document);
        int richParagraphs = richParagraphCount(document);
        int paragraphTextLength = paragraphTextLength(document);
        int headingCount = document.select("h1").size();
        boolean article = !document.select("article, [itemprop=articleBody]").isEmpty();
        boolean metadata = !document.select("time, [datetime], [itemprop=datePublished], [class*=author], [class*=date]").isEmpty();
        double linkDensity = linkDensity(document);

        int listScore = 0;
        if (repeatedLinkGroups >= 5) listScore += 5;
        else if (repeatedLinkGroups >= 3) listScore += 3;
        if (contentLinks >= 10) listScore += 2;
        if (contentLinks >= 20) listScore += 1;
        if (pagination) listScore += 3;
        if (itemList) listScore += 5;
        if (contentLinks >= 5 && linkDensity >= 0.2) listScore += 1;

        int detailScore = 0;
        if (richParagraphs >= 2) detailScore += 2;
        if (richParagraphs >= 4) detailScore += 1;
        if (paragraphTextLength >= 300) detailScore += 2;
        if (paragraphTextLength >= 800) detailScore += 1;
        if (1 == headingCount) detailScore += 1;
        if (article) detailScore += 1;
        if (metadata) detailScore += 1;
        if (repeatedLinkGroups >= 5) detailScore -= 3;
        if (pagination) detailScore -= 2;
        if (contentLinks >= 15 && linkDensity >= 0.2) detailScore -= 1;

        if (listScore >= 4 && listScore > detailScore) return LIST;
        if (detailScore >= 3 && detailScore >= listScore) return DETAIL;
        return OTHER;
    }

    private static boolean looksLikeErrorPage(Document document, String text) {
        String title = normalizedText(document.title());
        String sample = title + " " + text.substring(0, Math.min(text.length(), 800));
        return sample.contains("404")
                || sample.contains("not found")
                || sample.contains("page not found")
                || sample.contains("页面不存在")
                || sample.contains("页面未找到")
                || sample.contains("访问出错")
                || sample.contains("系统错误");
    }

    private static boolean looksLikeFormPage(Document document, String pageText) {
        if (!document.select("input[type=password]").isEmpty()) return true;
        int controls = document.select("form input, form select, form textarea, form button").size();
        if (controls >= 4 && pageText.length() < 1500 && contentLinkCount(document) < 5) return true;

        String title = normalizedText(document.title());
        String sample = title + " " + pageText.substring(0, Math.min(pageText.length(), 500));
        boolean authText = containsAny(sample, "登录", "登陆", "注册", "验证码", "sign in", "log in", "captcha");
        return authText && !document.select("form").isEmpty();
    }

    private static int maxRepeatedLinkGroups(Document document) {
        int max = 0;
        Elements parents = document.select("main, section, article, ul, ol, div, table, tbody");
        for (Element parent : parents) {
            if (isPageChrome(parent)) continue;
            Elements children = parent.children();
            if (children.size() < 3) continue;

            Map<String, Integer> groups = new HashMap<>();
            for (Element child : children) {
                if (isPageChrome(child)) continue;
                if (child.select("a[href]").isEmpty()) continue;
                String signature = child.tagName() + "|" + normalizedClass(child.className());
                groups.put(signature, groups.getOrDefault(signature, 0) + 1);
            }
            for (Integer count : groups.values()) {
                max = Math.max(max, count);
            }
        }
        return max;
    }

    private static int contentLinkCount(Document document) {
        int count = 0;
        for (Element link : document.select("a[href]")) {
            if (!isPageChrome(link)) count++;
        }
        return count;
    }

    private static boolean hasPagination(Document document) {
        Elements candidates = document.select(
                "[rel=next], [class*=pagination], [class*=pager], [id*=pagination], [id*=pager], " +
                        "[class*=page-nav], [class*=pagebar], [aria-label*=分页], [aria-label*=pagination]"
        );
        for (Element element : candidates) {
            String text = normalizedText(element.text() + " " + element.attr("aria-label") + " " + element.attr("rel"));
            if (containsAny(text, "下一页", "下页", "末页", "next", "»", "›")) return true;
        }
        return false;
    }

    private static boolean hasItemList(Document document) {
        for (Element script : document.select("script[type=application/ld+json]")) {
            if (normalizedText(script.data()).contains("itemlist")) return true;
        }
        return !document.select("[itemtype*=ItemList], [itemtype*=ItemList] *").isEmpty();
    }

    private static int richParagraphCount(Document document) {
        Elements paragraphs = document.select("article p, main p, [itemprop=articleBody] p");
        if (paragraphs.isEmpty()) paragraphs = document.select("p");
        int count = 0;
        for (Element paragraph : paragraphs) {
            if (paragraph.text().trim().length() >= 80) count++;
        }
        return count;
    }

    private static int paragraphTextLength(Document document) {
        Elements paragraphs = document.select("article p, main p, [itemprop=articleBody] p");
        if (paragraphs.isEmpty()) paragraphs = document.select("p");
        int length = 0;
        for (Element paragraph : paragraphs) {
            length += paragraph.text().trim().length();
        }
        return length;
    }

    private static double linkDensity(Document document) {
        int textLength = document.body().text().length();
        if (textLength == 0) return 0;
        int linkTextLength = 0;
        for (Element link : document.select("a[href]")) {
            if (!isPageChrome(link)) linkTextLength += link.text().trim().length();
        }
        return (double) linkTextLength / textLength;
    }

    private static boolean isPageChrome(Element element) {
        Element current = element;
        while (null != current) {
            String tag = current.tagName().toLowerCase(Locale.ROOT);
            String marker = normalizedText(current.className() + " " + current.id());
            if ("nav".equals(tag) || "header".equals(tag) || "footer".equals(tag) || "aside".equals(tag)) {
                return true;
            }
            if (containsAny(marker, "nav", "menu", "header", "footer", "breadcrumb", "sidebar")) {
                return true;
            }
            if ("body".equals(tag) || "html".equals(tag)) break;
            current = current.parent();
        }
        return false;
    }

    private static String normalizedClass(String className) {
        return normalizedText(className).replaceAll("\\d+", "#");
    }

    private static String normalizedText(String text) {
        return null == text ? "" : text.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }

    private static boolean containsAny(String text, String... values) {
        for (String value : values) {
            if (text.contains(value)) return true;
        }
        return false;
    }
}
