package com.iisquare.fs.web.lm.mvc;

import com.iisquare.fs.web.lm.ai.MethodToolCallbackProvider;
import com.iisquare.fs.web.lm.mcp.DemoTool;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 模型上下文协议 (MCP)
 * @see(https://docs.springframework.org.cn/spring-ai/reference/api/mcp/mcp-overview.html)
 * MCP Java SDK
 * @see(https://github.com/modelcontextprotocol/java-sdk)
 */
@Configuration
public class McpConfiguration {

    @Bean
    public ToolCallbackProvider tool(DemoTool demoTool) {
        return MethodToolCallbackProvider.builder().toolObjects(demoTool).build();
    }

}
