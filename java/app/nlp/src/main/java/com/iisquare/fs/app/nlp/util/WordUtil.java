package com.iisquare.fs.app.nlp.util;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.corpus.dependency.CoNll.CoNLLSentence;
import com.hankcs.hanlp.corpus.dependency.CoNll.CoNLLWord;
import com.hankcs.hanlp.corpus.document.sentence.Sentence;
import com.hankcs.hanlp.corpus.document.sentence.word.IWord;
import com.hankcs.hanlp.corpus.document.sentence.word.Word;
import com.hankcs.hanlp.model.crf.CRFLexicalAnalyzer;
import com.iisquare.fs.app.nlp.bean.WordSDPNode;
import com.iisquare.fs.app.nlp.bean.WordTitleNode;
import com.iisquare.fs.base.core.util.DPUtil;
import org.apache.commons.collections.IteratorUtils;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import scala.Tuple2;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class WordUtil {

    static List<String> regexTiles = Arrays.asList(
            "^([一二三四五六七八九十]+)、",
            "^（([一二三四五六七八九十]+)）",
            "^([0-9]+)[\\.、]+",
            "^([一二三四五六七八九十]+)是",
            "^第([一二三四五六七八九十]+)[章条]",
            "^(——)"
    );

    public static CRFLexicalAnalyzer analyzer;

    static {
        try {
            analyzer = new CRFLexicalAnalyzer();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static WordSDPNode sdp(String text) {
        CoNLLSentence sentence = HanLP.parseDependency(text);
        Iterator<CoNLLWord> iterator = sentence.iterator();
        Map<Integer, WordSDPNode> map = new LinkedHashMap<>();
        List<Integer> roots = new ArrayList<>();
        while (iterator.hasNext()) {
            CoNLLWord word = iterator.next();
            WordSDPNode node = new WordSDPNode();
            node.name = word.LEMMA;
            node.tag = word.POSTAG;
            node.relation = word.DEPREL;
            map.put(word.ID, node);
        }
        iterator = sentence.iterator();
        while (iterator.hasNext()) {
            CoNLLWord word = iterator.next();
            if (null == word.HEAD || 0 == word.HEAD.ID) {
                roots.add(word.ID);
            } else {
                map.get(word.HEAD.ID).children.add(map.get(word.ID));
            }
        }
        if (roots.size() != 1) {
            throw new RuntimeException("too many roots:" + DPUtil.implode(",", roots));
        }
        return map.get(roots.get(0));
    }

    /**
     * 清除HTML标签，仅保留段落和换行
     */
    public static String clean(String html) {
        Safelist tags = new Safelist().addTags("p", "br");
        String text = Jsoup.clean(html, tags);
        text = text.replaceAll("　", "");
        text = text.replaceAll("<p>", "");
        text = text.replaceAll("</p>", "\n");
        text = text.replaceAll("<br/?>", "\n");
        return text;
    }

    /**
     * 提取标题并生成节点
     * 如果不满足子标题格式，则返回为NULL
     */
    public static WordTitleNode title(String section) {
        section = DPUtil.trim(section);
        for (String regex : regexTiles) {
            List<String> matcher = DPUtil.matcher(regex, section, true);
            if (matcher.size() == 2) {
                WordTitleNode node = new WordTitleNode();
                node.prefix = matcher.get(0);
                node.text = sentence(section.substring(node.prefix.length()));
                node.sequence = matcher.get(1);
                return node;
            }
        }
        return null;
    }

    /**
     * 提取首句内容
     * 如果没有句号，则返回全部内容
     */
    public static String sentence(String section) {
        section = DPUtil.trim(section);
        int index = section.indexOf("。");
        if (index < 0) return section;
        return section.substring(0, index);
    }

    /**
     * 判断是否为同级节点
     */
    public static boolean isSibling(String t1, String t2) {
        for (String regex: regexTiles) {
            if (t1.matches(regex) && t2.matches(regex)) return true;
        }
        return false;
    }

    /**
     * 挂载节点
     */
    public static boolean mount(WordTitleNode ancestor, WordTitleNode node) {
        int size = ancestor.children.size();
        if (size == 0) {
            ancestor.children.add(node);
            return true;
        }
        WordTitleNode child = ancestor.children.get(size - 1);
        if (isSibling(child.prefix, node.prefix)) {
            ancestor.children.add(node);
            return true;
        }
        return mount(child, node);
    }

    /**
     * 挂载段落
     */
    public static boolean mount(WordTitleNode ancestor, String paragraph) {
        int size = ancestor.children.size();
        if (size == 0) {
            ancestor.paragraphs.add(paragraph);
            return true;
        }
        WordTitleNode child = ancestor.children.get(size - 1);
        return mount(child, paragraph);
    }

    public static List<Tuple2<String, Double>> words(String text, int minLength, int maxLength) {
        List<Tuple2<String, Double>> result = new ArrayList<>();
        Sentence sentence = analyzer.analyze(text);
        List words = IteratorUtils.toList(sentence.iterator());
        // 叹词（啊）,拟声词（哈哈）,助词（的）,标点符号,连词（和、与）,人名,地名,机构团体
        List<String> stops = Arrays.asList("e", "o", "u", "w", "c", "nr", "ns", "nt");
        int size = words.size();
        for (int i = 0; i < size; i++) {
            String word = "";
            for (int j = i; j < size; j++) {
                IWord b = (IWord) words.get(j);
                if (stops.contains(b.getLabel())) break;
                word += b.getValue();
                if (word.length() > maxLength) break;
                if (word.length() >= minLength) {
                    result.add(Tuple2.apply(word, 1.0));
                }
            }
        }
        return result;
    }

    /**
     * 采用类N-Gram方式提取词汇，字符顺序排列但无需紧连
     * @param text 候选文本
     * @param minLength 最小词汇长度
     * @param maxLength 最大词汇长度
     * @param tokenLength 可选字符长度
     * @param gap 字符自建是否允许存在间隙
     * @return 词汇列表
     */
    public static List<Tuple2<String, Double>> words(String text, int minLength, int maxLength, int tokenLength, boolean gap) {
        text = text.replaceAll("[^\\u4E00-\\u9FFFa-zA-Z0-9]", " "); // 去除特殊字符
        List<Tuple2<String, Double>> result = new ArrayList<>();
        for (String s : text.split(" ", -1)) { // 按空格拆分
            if ("".equals(s)) continue;
            if (s.length() <= minLength) {
                result.add(Tuple2.apply(s, 1.0));
                continue;
            }
            String[] strings = s.split("");
            if (gap) {
                for (int i = 0; i <= strings.length - minLength; i++) {
                    String word = strings[i]; // 首个字符固定
                    int begin = i + 1; // 待排列开始位置
                    int end = Math.min(i + tokenLength - 1, strings.length - 1); // 待排列结束位置
                    for (int length = minLength; length <= maxLength; length++) { // 词汇长度
                        if (end - begin + 1 < length - 1) break; // 剩余词汇不满足排列长度
                        rank(result, word, i, 0, strings, begin, end, length - 1);
                    }
                }
            } else {
                for (int i = 0; i <= strings.length - minLength; i++) {
                    for (int length = minLength; length <= maxLength; length++) { // 词汇长度
                        if (i + length > strings.length) break; // 剩余词汇不满足排列长度
                        result.add(Tuple2.apply(DPUtil.implode("", DPUtil.subarray(strings, i, length)), 1.0));
                    }
                }
            }
        }
        return result;
    }

    public static List<Tuple2<String, Double>> rank(List<Tuple2<String, Double>> result, String word, int first, int sum, String[] strings, int begin, int end, int length) {
//        System.out.println(String.format("%s, %s, %d, %d, %d", Arrays.toString(strings), word, begin, end, length));
        if (1 == length) {
            for (int i = begin; i <= end; i++) {
                result.add(Tuple2.apply(word + strings[i], score(sum + i -first, word.length() + 1)));
            }
        } else {
            for (int i = begin; i <= end; i++) {
                rank(result, word + strings[i], first, sum + i -first, strings, i + 1, end, length - 1);
            }
        }
        return result;
    }

    public static double score(int sum, int length) {
        int sum2 = 0;
        for (int i = 1; i < length; i++) {
            sum2 += i;
        }
        return (double) sum2 / (double) sum;
    }

    public static String combine(Map<String, Integer> counts, List<String> stops, Word... words) {
        StringBuilder sb = new StringBuilder();
        for (Word word : words) {
            if (stops.contains(word.getLabel())) return null;
            String value = word.getValue();
            if (value.contains(" ")) return null;
            sb.append(value);
        }
        String result = sb.toString();
        counts.put(result, counts.getOrDefault(result, 0) + 1);
        return result;
    }

    public static List<String> combine(List<String> list) {
        for (int i = 0; i < list.size(); i++) {
            String a = list.get(i);
            if (null == a) continue;
            for (int j = 0; j < list.size(); j++) {
                if (i == j) continue;
                String b = list.get(j);
                if (null == b) continue;
                if (a.contains(b)) {
                    list.set(j, null);
                }
            }
        }
        return list.stream().filter(Objects::nonNull).collect(Collectors.toList());
    }

}
