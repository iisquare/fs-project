package com.iisquare.fs.web.lm.core;

import lombok.Data;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class TrieNode implements Serializable {

    public String world = ""; // 单字

    public int level = 0; // 所在层级

    public boolean composable = false; // 仅标识当前节点可组合为一个关键词，不表示无子级元素

    public TrieNode parent = null; // 上级节点

    public Map<String, TrieNode> children = new LinkedHashMap<>();

    @Override
    public String toString() {
        return "TrieNode{" +
                "world='" + world + '\'' +
                ", level=" + level +
                ", composable=" + composable +
                '}';
    }
}
