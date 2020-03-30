package com.iisquare.fs.web.core.rbac;

import com.iisquare.fs.base.core.util.DPUtil;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

public class PermitInterceptor implements HandlerInterceptor {

    public static final String PACKAGE_CONTROLLER = "controller";
    public static final String SUFFIX_CONTROLLER = "Controller";
    public static final String SUFFIX_ACTION = "Action";
    public static final String ATTRIBUTE_USER = "user";
    public static final String ATTRIBUTE_RESOURCE = "resource";
    public static final String ATTRIBUTE_MODULE = "module";
    public static final String ATTRIBUTE_CONTROLLER = "controller";
    public static final String ATTRIBUTE_ACTION = "action";
    public static final String ATTRIBUTE_TEMPLATE = "template";
    protected RbacServiceBase rbacService;

    public PermitInterceptor(RbacServiceBase rbacService) {
        this.rbacService = rbacService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if(!(handler instanceof HandlerMethod)) return true;
        HandlerMethod method = (HandlerMethod) handler;
        if(!(method.getBean() instanceof PermitControllerBase)) return true;
        PermitControllerBase instance = (PermitControllerBase) method.getBean();
        String[] names = DPUtil.explode(instance.getClass().getName(), "\\.", null, false);
        if(names.length < 3) throw new PermitException(PermitException.NO_PACKAGE);
        String module = names[names.length - 2];
        if (module.equalsIgnoreCase(PACKAGE_CONTROLLER)) {
            module = names[names.length - 3];
            request.setAttribute(ATTRIBUTE_TEMPLATE, "");
        } else {
            request.setAttribute(ATTRIBUTE_TEMPLATE, module);
        }
        String controller = names[names.length - 1].split("\\$\\$")[0]; // Spring代理类名称会追加特殊标识
        controller = controller.replaceFirst(SUFFIX_CONTROLLER, "");
        controller = controller.substring(0, 1).toLowerCase() + controller.substring(1); // 首字母小写
        String action = method.getMethod().getName().replaceFirst(SUFFIX_ACTION, "");
        action = action.substring(0, 1).toLowerCase() + action.substring(1); // 首字母小写
        request.setAttribute(ATTRIBUTE_MODULE, module);
        request.setAttribute(ATTRIBUTE_CONTROLLER, controller);
        request.setAttribute(ATTRIBUTE_ACTION, action);
        Permission permission = method.getMethodAnnotation(Permission.class);
        if(null == permission) return true;
        String[] permissions = permission.value();
        if(null == permissions || permissions.length < 1) {
            permissions = new String[]{ module + ":" + controller + ":" + action };
        }
        Map<String, Boolean> name2boolean = new HashMap<>();
        for (String str : permissions) {
            if(null == str) str = "";
            String[] strs = str.split(":");
            if(strs.length > 3) {
                throw new PermitException(PermitException.NAME_TOO_LONG);
            } else if(strs.length > 2) {
                strs = new String[]{ strs[0], strs[1], strs[2] };
            } else if(strs.length > 1) {
                strs = new String[]{ module, strs[0], strs[1] };
            } else if(strs.length > 0) {
                strs = new String[]{ module, controller, strs[0] };
            } else {
                throw new PermitException(PermitException.NAME_TOO_SHORT);
            }
            name2boolean.put(rbacService.keyPermit(strs[0], strs[1], strs[2]), false);
        }
        if (rbacService.hasPermit(request, name2boolean)) return true;
        if (rbacService.uid(request) < 1) {
            throw new PermitException(PermitException.REQUIRED_LOGIN);
        } else {
            throw new PermitException(PermitException.PERMIT_DENIED);
        }
    }

}
