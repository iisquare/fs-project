package com.iisquare.fs.web.flink.controller;

import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.web.mvc.ControllerBase;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/tool")
public class ToolController extends ControllerBase {

    @RequestMapping("/classname")
    public String classnameAction(@RequestParam Map<String, Object> param, ModelMap model, HttpServletRequest request) {
        Map<String, Map<String, Object>> data = new LinkedHashMap<>();
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("name", Boolean.class.getSimpleName());
        item.put("classname", Boolean.class.getName());
        data.put(Boolean.class.getName(), item);
        item = new LinkedHashMap<>();
        item.put("name", Byte.class.getSimpleName());
        item.put("classname", Byte.class.getName());
        data.put(Byte.class.getName(), item);
        item = new LinkedHashMap<>();
        item.put("name", Double.class.getSimpleName());
        item.put("classname", Double.class.getName());
        data.put(Double.class.getName(), item);
        item = new LinkedHashMap<>();
        item.put("name", Float.class.getSimpleName());
        item.put("classname", Float.class.getName());
        data.put(Float.class.getName(), item);
        item = new LinkedHashMap<>();
        item.put("name", Integer.class.getSimpleName());
        item.put("classname", Integer.class.getName());
        data.put(Integer.class.getName(), item);
        item = new LinkedHashMap<>();
        item.put("name", Long.class.getSimpleName());
        item.put("classname", Long.class.getName());
        data.put(Long.class.getName(), item);
        item = new LinkedHashMap<>();
        item.put("name", Short.class.getSimpleName());
        item.put("classname", Short.class.getName());
        data.put(Short.class.getName(), item);
        item = new LinkedHashMap<>();
        item.put("name", String.class.getSimpleName());
        item.put("classname", String.class.getName());
        data.put(String.class.getName(), item);
        item = new LinkedHashMap<>();
        item.put("name", Date.class.getSimpleName());
        item.put("classname", Date.class.getName());
        data.put(Date.class.getName(), item);
        item = new LinkedHashMap<>();
        item.put("name", java.sql.Date.class.getSimpleName());
        item.put("classname", java.sql.Date.class.getName());
        data.put(java.sql.Date.class.getName(), item);
        item = new LinkedHashMap<>();
        item.put("name", java.sql.Time.class.getSimpleName());
        item.put("classname", java.sql.Time.class.getName());
        data.put(java.sql.Time.class.getName(), item);
        item = new LinkedHashMap<>();
        item.put("name", java.sql.Timestamp.class.getSimpleName());
        item.put("classname", java.sql.Timestamp.class.getName());
        data.put(java.sql.Timestamp.class.getName(), item);
        return ApiUtil.echoResult(0, null, data.values());
    }

}
