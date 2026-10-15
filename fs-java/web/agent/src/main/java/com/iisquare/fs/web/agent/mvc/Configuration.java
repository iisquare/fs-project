package com.iisquare.fs.web.agent.mvc;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
public class Configuration {

    @Value("${fs.format.date}")
    private String formatDate;

    @Value("${fs.agent.image.expire:1800000}")
    private int imageExpire; // 图片原图地址签发有效期（毫秒）
    @Value("${fs.agent.image.placeholder:/images/loading.svg}")
    private String imagePlaceholder; // 正文中图片引用的占位地址，标识以片段形式跟随其后

    @Value("${fs.agent.mcp.sampleUrl:http://localhost:7827/sse}")
    private String mcpSampleUrl; // 工具页「MCP样例」按钮填入的地址：本服务自带的 MCP 服务

}
