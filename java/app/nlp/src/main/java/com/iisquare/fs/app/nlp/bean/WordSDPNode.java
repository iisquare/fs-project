package com.iisquare.fs.app.nlp.bean;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class WordSDPNode implements Serializable {

    public String name = null; // 分词
    public String tag = ""; // 词性
    public String relation = ""; // 关系
    public List<WordSDPNode> children = new ArrayList<>(); // 子级

}
