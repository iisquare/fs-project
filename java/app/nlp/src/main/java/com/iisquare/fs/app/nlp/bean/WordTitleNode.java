package com.iisquare.fs.app.nlp.bean;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class WordTitleNode implements Serializable {

    public String prefix = ""; // 前缀
    public String text = null; // 标题
    public String sequence = ""; // 序号
    public List<WordTitleNode> children = new ArrayList<>();

}
