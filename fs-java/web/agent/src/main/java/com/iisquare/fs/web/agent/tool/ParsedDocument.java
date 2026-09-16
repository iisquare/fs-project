package com.iisquare.fs.web.agent.tool;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 文档解析结果
 * markdown 中用 {{kb-image:i}} 占位符标记第 i 张图片的插入位置，
 * 图片上传至文件服务后由业务层替换为 ![说明](kb:文件标识)
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParsedDocument {

    private String markdown;
    private List<ParsedImage> images;

}
