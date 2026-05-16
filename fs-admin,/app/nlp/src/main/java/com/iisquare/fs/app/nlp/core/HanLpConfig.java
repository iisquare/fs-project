package com.iisquare.fs.app.nlp.core;

import com.hankcs.hanlp.HanLP;

public class HanLpConfig {

    public static String ROOT = HanLP.Config.CoreDictionaryPath.substring(0, HanLP.Config.CoreDictionaryPath.indexOf("/data/dictionary/"));

    public static String MSR_VECTOR = ROOT + "/data/pretrained/merge_sgns_bigram_char300_20220130_214613.txt";

}
