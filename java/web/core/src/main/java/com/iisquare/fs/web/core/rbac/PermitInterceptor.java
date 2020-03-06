package com.iisquare.fs.web.core.rbac;

import com.iisquare.fs.base.core.util.DPUtil;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

public class PermitInterceptor implements HandlerInterceptor {

    public static final String CONTROLLER_SUFFIX = "Controller";
    public static final String CONTROLLER_PACKAGE = "controller";
    public static final String ACTION_SUFFIX = "Action";
    protected RbacServiceBase rbac;

    public PermitInterceptor(RbacServiceBase rbac) {
        this.rbac = rbac;
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
        if (module.equalsIgnoreCase(CONTROLLER_PACKAGE)) {
            module = names[names.length - 3];
        }
        String controller = names[names.length - 1].split("\\$\\$")[0]; // Spring代理类名称会追加特殊标识
        controller = controller.replaceFirst(CONTROLLER_SUFFIX, "");
        controller = controller.substring(0, 1).toLowerCase() + controller.substring(1); // 首字母小写
        String action = method.getMethod().getName().replaceFirst(ACTION_SUFFIX, "");
        action = action.substring(0, 1).toLowerCase() + action.substring(1); // 首字母小写
        request.setAttribute("module", module);
        request.setAttribute("controller", controller);
        request.setAttribute("action", action);
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
            name2boolean.put(rbac.keyPermit(strs[0], strs[1], strs[2]), false);
        }
        if (rbac.hasPermit(request, name2boolean)) return true;
        throw new PermitException(PermitException.PERMIT_DENIED);
    }

}
