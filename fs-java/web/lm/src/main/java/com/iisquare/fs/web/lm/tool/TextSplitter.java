package com.iisquare.fs.web.lm.tool;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 文本切分工具：按知识库配置将全文切分为分段(segment)与分块(chunk)
 * tokenSize 以字符数衡量（与实体注释“字符数量”一致）
 */
public class TextSplitter {

    private TextSplitter() {}

    /**
     * 将全文切分为分段
     * @param content 全文 Markdown
     * @param separator 段落分隔符，非空时优先按分隔符切分
     * @param segmentTokens 每段最大字符数，<=0 表示不按长度切分
     */
    public static List<String> splitSegments(String content, String separator, int segmentTokens) {
        List<String> segments = new ArrayList<>();
        if (null == content || content.isEmpty()) return segments;
        if (null != separator && !separator.isEmpty()) {
            String[] parts = content.split(Pattern.quote(separator));
            for (String part : parts) {
                String text = part.strip();
                if (!text.isEmpty()) segments.add(text);
            }
            return segments.isEmpty() ? List.of(content) : segments;
        }
        // 无分隔符：先按段落（空行）拆分
        String[] paragraphs = content.split("\\n{2,}");
        List<String> buffer = new ArrayList<>();
        for (String paragraph : paragraphs) {
            String text = paragraph.strip();
            if (text.isEmpty()) continue;
            buffer.add(text);
        }
        if (buffer.isEmpty()) return List.of(content);
        if (segmentTokens <= 0) {
            return buffer; // 每段一块
        }
        // 贪心合并段落至 segmentTokens
        List<String> merged = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String text : buffer) {
            if (current.length() > 0 && current.length() + text.length() + 2 > segmentTokens) {
                merged.add(current.toString());
                current.setLength(0);
            }
            if (current.length() > 0) current.append("\n\n");
            current.append(text);
        }
        if (current.length() > 0) merged.add(current.toString());
        return merged.isEmpty() ? List.of(content) : merged;
    }

    /**
     * 将一个分段切分为多个分块（带重叠滑窗）
     * @param segment 分段文本
     * @param chunkTokens 每块最大字符数，<=0 表示整段单块
     * @param overlayTokens 相邻块重叠字符数
     */
    public static List<String> splitChunks(String segment, int chunkTokens, int overlayTokens) {
        List<String> chunks = new ArrayList<>();
        if (null == segment || segment.isEmpty()) return chunks;
        if (chunkTokens <= 0 || segment.length() <= chunkTokens) {
            chunks.add(segment);
            return chunks;
        }
        int overlay = Math.max(0, Math.min(overlayTokens, chunkTokens - 1));
        int step = Math.max(1, chunkTokens - overlay);
        int start = 0;
        while (start < segment.length()) {
            int end = Math.min(start + chunkTokens, segment.length());
            chunks.add(segment.substring(start, end));
            if (end >= segment.length()) break;
            start += step;
        }
        return chunks;
    }

}
