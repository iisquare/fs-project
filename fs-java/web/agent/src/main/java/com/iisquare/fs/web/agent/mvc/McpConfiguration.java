package com.iisquare.fs.web.agent.mvc;

import com.iisquare.fs.web.agent.ai.MethodToolCallbackProvider;
import com.iisquare.fs.web.agent.mcp.BiTool;
import com.iisquare.fs.web.agent.mcp.DemoTool;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 模型上下文协议 (MCP)
 * {@code https://docs.springframework.org.cn/spring-ai/reference/api/mcp/mcp-overview.html}
 * MCP Java SDK
 * {@code https://github.com/modelcontextprotocol/java-sdk}
 */
@Configuration
public class McpConfiguration {

    @Bean
    public ToolCallbackProvider tool(DemoTool demoTool, BiTool biTool) {
        return MethodToolCallbackProvider.builder().toolObjects(demoTool, biTool).build();
    }

}
