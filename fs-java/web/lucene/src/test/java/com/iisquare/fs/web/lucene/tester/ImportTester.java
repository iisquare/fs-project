package com.iisquare.fs.web.lucene.tester;

import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.FileUtil;
import org.junit.Test;

import java.net.URL;
import java.util.*;

public class ImportTester {

    @Test
    public void synonymTest() {
        // @see(http://www.360doc.com/content/11/0803/19/268489_137740482.shtml)
        URL url = getClass().getClassLoader().getResource("suggest_synonym.txt");
        String content = FileUtil.getContent(url, false, "UTF-8");
        Set<String> result = new LinkedHashSet<>();
        for (String line : DPUtil.explode("\n", content)) {
            System.out.println(line);
            String[] strings = DPUtil.explode("=&gt;", line);
            if (strings.length != 2) continue;
            String pinyin = DPUtil.trim(strings[0]).toLowerCase().replaceAll("ü", "v");
            line = DPUtil.trim(strings[1].replaceAll("[',]", ""));
            if (DPUtil.empty(pinyin) || DPUtil.empty(line)) continue;
            int lineLength = line.length(), pinyinLength = pinyin.length();
            for (int i = 0; i < lineLength; i++) {
                List<String> list = new ArrayList<>();
                list.add(String.valueOf(line.charAt(i)));
                String word = "";
                for (int j = 0; j < pinyinLength; j++) {
                    word += pinyin.charAt(j);
                    list.add(word);
                }
                result.add(line.charAt(i) + "=>" + DPUtil.implode(",", list.toArray(new String[0])));
            }
        }
        System.out.println("# --------Synonym-------- #");
        for (String item : result) {
            System.out.println(item);
        }
        System.out.println("# --------Synonym-------- #");
    }

    @Test
    public void pinyinTest() throws Exception {
        // @see(http://xh.5156edu.com/pinyi.php)
        URL url = getClass().getClassLoader().getResource("suggest_pinyi.txt");
        String content = FileUtil.getContent(url, false, "UTF-8");
        Set<String> result = new LinkedHashSet<>();
        for (String line : DPUtil.explode("\n", content)) {
            line = line.replaceAll("[\\s　\\t]+", " ");
            line = line.replaceAll("ü", "v");
            System.out.println(line);
            for (String item : DPUtil.explode(" ", line.toLowerCase())) {
                if (DPUtil.empty(item)) continue;
                String word = "";
                int length = item.length();
                for (int i = 0; i < length; i++) {
                    word += item.charAt(i);
                    result.add(word);
                }
            }
        }
        System.out.println("# --------PinYin-------- #");
        for (String item : result) {
            System.out.println(item);
        }
        System.out.println("# --------PinYin-------- #");
    }

}
