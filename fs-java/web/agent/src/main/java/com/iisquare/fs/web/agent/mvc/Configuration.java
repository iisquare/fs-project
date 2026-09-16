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

}
