package com.iisquare.fs.web.bi.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "fs.bi.datasource")
public class DatasourceProperties {

    /** CSV/Excel文件上传目录 */
    private String uploadDir = "./uploads/datasource";

    /** JDBC连接超时时间（毫秒） */
    private int jdbcTimeout = 5000;

}
