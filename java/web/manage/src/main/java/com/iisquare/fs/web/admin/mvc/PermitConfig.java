package com.iisquare.fs.web.admin.mvc;

import com.iisquare.fs.base.web.config.WebMvcConfiguration;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
public class PermitConfig extends WebMvcConfiguration {

    @Bean
    public HandlerExceptionResolver permitExceptionResolver() {
        return new HandlerExceptionResolver() {
            @Override
            public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
                if(ex instanceof PermitException) {
                    Map<String, Object> model = new LinkedHashMap<>();
                    if("GET".equals(request.getMethod())) {
                        model.put("message", ex.getMessage());
                        return new ModelAndView("error/permit/web", model);
                    } else {
                        response.setHeader("Access-Control-Allow-Origin", request.getHeader("Origin"));
                        response.setHeader("Access-Control-Allow-Credentials", "true");
                        model.put("message", ApiUtil.echoResult(403, ex.getMessage(), null));
                        return new ModelAndView("error/permit/json", model);
                    }
                }
                return null;
            }
        };
    }

    @Override
    protected void addInterceptors(InterceptorRegistry registry) {
        super.addInterceptors(registry);
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
                if(!(handler instanceof HandlerMethod)) return true;
                HandlerMethod method = (HandlerMethod) handler;
                if(!(method.getBean() instanceof PermitController)) return true;
                PermitController instance = (PermitController) method.getBean();
                String[] names = DPUtil.explode(instance.getClass().getName(), "\\.", null, false);
                if(names.length < 2) throw new PermitException("class has no package");
                String module = names[names.length - 2];
                String controller = names[names.length - 1].replaceFirst("Controller", "");
                controller = controller.substring(0, 1).toLowerCase() + controller.substring(1);
                String action = method.getMethod().getName().replaceFirst("Action", "");
                action = action.substring(0, 1).toLowerCase() + action.substring(1);
                request.setAttribute("module", module);
                request.setAttribute("controller", controller);
                request.setAttribute("action", action);
                Permission permission = method.getMethodAnnotation(Permission.class);
                if(null == permission) return true;
                if(instance.uid(request) < 1) throw new PermitException("required login");
                String[] permissions = permission.value();
                if(null == permissions || permissions.length < 1) {
                    permissions = new String[]{ module + ":" + controller + ":" + action };
                }
                for (String str : permissions) {
                    if(null == str) str = "";
                    String[] strs = str.split(":");
                    if(strs.length > 3) {
                        throw new PermitException("path length great than 3");
                    } else if(strs.length > 2) {
                        strs = new String[]{ strs[0], strs[1], strs[2] };
                    } else if(strs.length > 1) {
                        strs = new String[]{ module, strs[0], strs[1] };
                    } else if(strs.length > 0) {
                        strs = new String[]{ module, controller, strs[0] };
                    } else {
                        throw new PermitException("path length less than 1");
                    }
                    if(instance.hasPermit(request, strs[0], strs[1].split("\\$\\$")[0], strs[2])) return true;
                }
                throw new PermitException("user has no permit or resource is disabled");
            }

            @Override
            public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {

            }

            @Override
            public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {

            }
        }).addPathPatterns("/manage/**");
    }

}
