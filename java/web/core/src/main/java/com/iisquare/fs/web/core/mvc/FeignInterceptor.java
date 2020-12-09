package com.iisquare.fs.web.core.mvc;

import com.iisquare.fs.base.core.util.DPUtil;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;

public class FeignInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (null == attributes) return;
        HttpServletRequest request = attributes.getRequest();
        List<String> headers = Arrays.asList("token", "cookie", "user-agent");
        for (String name : headers) {
            String header = request.getHeader(name);
            if (!DPUtil.empty(header)) {
                template.header(name, header);
            }
        }
    }

}
