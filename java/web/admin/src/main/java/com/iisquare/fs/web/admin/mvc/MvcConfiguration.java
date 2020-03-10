package com.iisquare.fs.web.admin.mvc;

import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.web.config.WebMvcConfiguration;
import com.iisquare.fs.web.core.rbac.PermitException;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.view.json.MappingJackson2JsonView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Configuration
public class MvcConfiguration extends WebMvcConfiguration implements HandlerExceptionResolver {

    @Override
    public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        if(ex instanceof PermitException) {
            MappingJackson2JsonView view = new MappingJackson2JsonView();
            view.setAttributesMap(ApiUtil.result(403, ex.getMessage(), null));
            return new ModelAndView(view);
        }
        return null;
    }

    @Override
    protected void addCorsMappings(CorsRegistry registry) {
        super.addCorsMappings(registry);
        registry.addMapping("/**")
            .allowedOrigins("*")
            .allowCredentials(true)
            .allowedMethods("GET", "POST", "DELETE", "PUT")
            .maxAge(3600);
    }

}
