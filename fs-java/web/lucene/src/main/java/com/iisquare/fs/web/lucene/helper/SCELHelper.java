package com.iisquare.fs.web.lucene.helper;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.*;

/**
 * 解析搜狗细胞词库
 *
 * @see(https://github.com/Zehao/sogouSCEL/blob/master/src/org/hnote/sogou/SCEL.java)
 */
public class SCELHelper {

    private int wordCount = 0;
    private File file;
    private Map<Integer, String> dict = new HashMap<>();
    private Map<String, LinkedList<String>> wordList = new HashMap<>();

    public SCELHelper(File file) {
        this.file = file;
    }

    public static SCELHelper getInstance(File file) {
        return new SCELHelper(file);
    }

    public Map<String, LinkedList<String>> wordList() {
        return this.wordList;
    }

    public SCELHelper printWordListWithPinyin() {
        for (String w : wordList.keySet()) {
            System.out.println(w + Arrays.asList(wordList.get(w).toArray()));
        }
        return this;
    }

    public SCELHelper printWordList() {
        for (String w : wordList.keySet()) {
            System.out.println(w);
        }
        return this;
    }

    public SCELHelper parse() throws Exception {
        RandomAccessFile raf = new RandomAccessFile(file, "r");
        byte[] str = new byte[128];
        int hzPosition = 0;
        raf.read(str, 0, 128); // \x40\x15\x00\x00\x44\x43\x53\x01
        if (str[4] == 0x44) {
            hzPosition = 0x2628;
        }
        if (str[4] == 0x45) {
            hzPosition = 0x26C4;
        }
        raf.seek(0x124); // get word count at 0x124
        wordCount = readInt(raf);
        raf.seek(0x1544); // get pinyin position
        while (true) {
            byte[] num = new byte[4];
            raf.read(num, 0, 4);
            int mark = num[0] + num[1] * 256;
            byte[] buff = new byte[20];
            raf.read(buff, 0, num[2]);
            String py = getString(buff, num[2]);
            dict.put(mark, py);
            if (py.equals("zuo")) {
                break;
            }
        }
        raf.seek(hzPosition); // get hanzi position
        while (true) {
            byte[] num = new byte[4];
            raf.read(num, 0, 4);
            int samePYcount = num[0] + num[1] * 256;
            int count = num[2] + num[3] * 256;

            byte[] buff = new byte[256];
            for (int i = 0; i < count; i++)
                buff[i] = raf.readByte();

            List<String> wordPY = new LinkedList<String>();
            for (int i = 0; i < count / 2; i++) {
                int key = buff[i * 2] + buff[i * 2 + 1] * 256;
                wordPY.add(dict.get(key));
            }
            for (int s = 0; s < samePYcount; s++) { // 同音词，使用前面相同的拼音
                raf.read(num, 0, 2);
                int hzBytecount = num[0] + num[1] * 256;
                // System.out.println("hzBytecount:" + hzBytecount);
                raf.read(buff, 0, hzBytecount);
                String word = getString(buff, hzBytecount);
                // System.out.println(word);
                raf.readShort();
                raf.readInt();
                wordList.put(word, (LinkedList<String>) wordPY);
                for (int i = 0; i < 6; i++) {
                    raf.readByte();
                }
            }
            if (raf.getFilePointer() == raf.length()) break;
        }
        raf.close();
        return this;
    }

    private int readInt(RandomAccessFile raf) throws Exception {
        byte[] buff = new byte[4];
        raf.read(buff, 0, 4);
        return (int) buff[0] & 0xFF + (((int) buff[1] & 0xFF) << 8) + (((int) buff[2] & 0xFF) << 16) | (((int) buff[3] & 0xFF) << 24);
    }

    private String getString(byte[] buff, int num) throws Exception {
        String str = new String(buff, 0, num, "UTF-16LE");
        return str;
    }

}
