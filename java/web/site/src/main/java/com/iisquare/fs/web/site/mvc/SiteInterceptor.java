package com.iisquare.fs.web.site.mvc;

import com.iisquare.fs.base.core.util.DPUtil;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class SiteInterceptor implements HandlerInterceptor {

    public static final String SUFFIX_CONTROLLER = "Controller";
    public static final String SUFFIX_ACTION = "Action";
    public static final String ATTRIBUTE_MODULE = "module";
    public static final String ATTRIBUTE_CONTROLLER = "controller";
    public static final String ATTRIBUTE_ACTION = "action";
    public static final String ATTRIBUTE_TEMPLATE = "template";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if(!(handler instanceof HandlerMethod)) return true;
        HandlerMethod method = (HandlerMethod) handler;
        if(!(method.getBean() instanceof SiteControllerBase)) return true;
        SiteControllerBase instance = (SiteControllerBase) method.getBean();
        String[] names = DPUtil.explode("\\.", instance.getClass().getName(), null, false);
        if(names.length < 5) return false;
        String module = names[names.length - 2];
        String controller = names[names.length - 1].split("\\$\\$")[0]; // Spring代理类名称会追加特殊标识
        controller = controller.replaceFirst(SUFFIX_CONTROLLER, "");
        controller = controller.substring(0, 1).toLowerCase() + controller.substring(1); // 首字母小写
        String action = method.getMethod().getName().replaceFirst(SUFFIX_ACTION, "");
        action = action.substring(0, 1).toLowerCase() + action.substring(1); // 首字母小写
        request.setAttribute(ATTRIBUTE_MODULE, module);
        request.setAttribute(ATTRIBUTE_CONTROLLER, controller);
        request.setAttribute(ATTRIBUTE_ACTION, action);
        request.setAttribute(ATTRIBUTE_TEMPLATE, String.format("%s-%s", module, names[names.length - 3]));
        return true;
    }

}
