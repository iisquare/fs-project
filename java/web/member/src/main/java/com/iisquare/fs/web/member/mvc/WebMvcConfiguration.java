package com.iisquare.fs.web.member.mvc;

import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.web.core.rbac.PermitException;
import com.iisquare.fs.web.core.rbac.PermitInterceptor;
import com.iisquare.fs.web.member.service.RbacService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Configuration
public class WebMvcConfiguration extends WebMvcConfigurationSupport implements HandlerExceptionResolver {

    @Autowired
    private RbacService rbacService;

    @Override
    protected void addInterceptors(InterceptorRegistry registry) {
        super.addInterceptors(registry);
        registry.addInterceptor(new PermitInterceptor(rbacService)).addPathPatterns("/**");
    }

    @Override
    public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        if(ex instanceof PermitException) {
            return new ModelAndView(ApiUtil.echoResult(403, ex.getMessage(), null));
        }
        return null;
    }

}
