package com.iisquare.fs.spark.app.test;

import com.iisquare.fs.spark.app.Word;
import org.junit.Test;

import java.util.List;

public class WordTester {

    @Test
    public void splitTest() {
        Word word = new Word();
        List<String> list = word.split("0123456789ABCDE,FGH", 2, 12);
        System.out.println(list);
    }

    @Test
    public void letterTest() {
        String content = "aB,*，中\n0";
        int length = content.length();
        for (int i = 0; i < length; i++) {
            char c = content.charAt(i);
            System.out.println("-----" + c + "-----");
            System.out.println("isLetterOrDigit:" + Character.isLetterOrDigit(c));
            System.out.println("isAlphabetic:" + Character.isAlphabetic(c)); // 子集
            System.out.println("isLetter:" + Character.isLetter(c)); // 全集
        }
    }

}
