package com.iisquare.fs.web.lm.mcp;

import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.lm.ai.ToolParam;
import com.iisquare.fs.web.lm.mvc.Configuration;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DemoTool {

    @Autowired
    Configuration configuration;

    @Tool(description = "Get weather information by city name")
    public String weather(@ToolParam(name = "city", description = "city name") String city) {
        return switch (city) {
            case "北京" -> "sunny";
            case "上海" -> "rainy";
            case "深圳" -> "cloudy";
            default -> "unknown";
        };
    }

    @Tool(description = "Get current datetime")
    public String datetime() {
        return DPUtil.millis2dateTime(System.currentTimeMillis(), configuration.getFormatDate());
    }

    @Tool(description = "Get service info by name")
    public String information(@ToolParam("name") String name) {
        return "fs-mcp-service for " + name;
    }

}
