package com.iisquare.jwframe.core.component;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.iisquare.jwframe.mvc.ControllerBase;

@Controller
@Scope("prototype")
public abstract class CoreController extends ControllerBase {

	protected Object displayMessage(int code, Object message, Object data) throws Exception {
		if(null == message) {
            switch (code) {
                case 0 :
                    message = "操作成功";
                    break;
                case 403 :
                    message = "禁止访问";
                    break;
                case 404 :
                    message = "信息不存在";
                    break;
                case 500 :
                    message = "操作失败";
                    break;
            }
        }
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("code", code);
		map.put("message", message);
		map.put("data", data);
		return displayJSON(map);
	}

	@Override
	protected Object displayTemplate(String controller, String action) throws Exception {
		assign("webUrl", appPath.substring(0, appPath.length() - 1));
		return super.displayTemplate(controller, action);
	}
	
}
