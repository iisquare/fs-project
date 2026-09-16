package com.iisquare.fs.web.agent.tool;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 文档解析出的图片：图谱、图表、示意图等
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParsedImage {

    private String name; // 原始文件名
    private String suffix; // 含点后缀
    private String type; // 内容类型
    private byte[] data; // 原始字节
    private Integer page; // 来源页码，0 表示未知
    private String alt; // 图片说明，用于检索与无障碍展示

}
