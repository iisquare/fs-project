package com.iisquare.fs.base.web.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

import java.nio.charset.Charset;
import java.util.List;

/**
 * WebMvcConfigurationSupport只能有一个实例
 */
public abstract class WebMvcConfiguration extends WebMvcConfigurationSupport {

    @Value("${spring.http.encoding.charset}")
    private String charset;

    @Override
    protected void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        super.configureMessageConverters(converters);
        StringHttpMessageConverter convert = new StringHttpMessageConverter(Charset.forName(charset));
        convert.setWriteAcceptCharset(false);
        converters.add(convert);
        converters.add(new MappingJackson2HttpMessageConverter());
    }

}
