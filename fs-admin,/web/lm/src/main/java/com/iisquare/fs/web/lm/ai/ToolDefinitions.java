package com.iisquare.fs.web.lm.ai;

import org.springframework.ai.tool.definition.DefaultToolDefinition;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.support.ToolUtils;
import org.springframework.util.Assert;

import java.lang.reflect.Method;

public class ToolDefinitions {

    private ToolDefinitions() {
        // prevents instantiation.
    }

    /**
     * Create a default {@link ToolDefinition} builder from a {@link Method}.
     */
    public static DefaultToolDefinition.Builder builder(Method method) {
        Assert.notNull(method, "method cannot be null");
        return DefaultToolDefinition.builder()
                .name(ToolUtils.getToolName(method))
                .description(ToolUtils.getToolDescription(method))
                .inputSchema(JsonSchemaGenerator.generateForMethodInput(method));
    }

    /**
     * Create a default {@link ToolDefinition} instance from a {@link Method}.
     */
    public static ToolDefinition from(Method method) {
        return builder(method).build();
    }

}
