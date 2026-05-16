package com.iisquare.fs.web.lm.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.ai.tool.support.ToolUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MethodToolCallbackProvider implements ToolCallbackProvider {

    private static final Logger logger = LoggerFactory.getLogger(MethodToolCallbackProvider.class);

    private final List<Object> toolObjects;

    private MethodToolCallbackProvider(List<Object> toolObjects) {
        Assert.notNull(toolObjects, "toolObjects cannot be null");
        Assert.noNullElements(toolObjects, "toolObjects cannot contain null elements");
        assertToolAnnotatedMethodsPresent(toolObjects);
        this.toolObjects = toolObjects;
        validateToolCallbacks(getToolCallbacks());
    }

    private void assertToolAnnotatedMethodsPresent(List<Object> toolObjects) {

        for (Object toolObject : toolObjects) {
            List<Method> toolMethods = Stream
                    .of(ReflectionUtils.getDeclaredMethods(
                            AopUtils.isAopProxy(toolObject) ? AopUtils.getTargetClass(toolObject) : toolObject.getClass()))
                    .filter(this::isToolAnnotatedMethod)
                    .filter(toolMethod -> !isFunctionalType(toolMethod))
                    .toList();

            if (toolMethods.isEmpty()) {
                throw new IllegalStateException("No @Tool annotated methods found in " + toolObject + "."
                        + "Did you mean to pass a ToolCallback or ToolCallbackProvider? If so, you have to use .toolCallbacks() instead of .tool()");
            }
        }
    }

    @Override
    public ToolCallback[] getToolCallbacks() {
        var toolCallbacks = this.toolObjects.stream()
                .map(toolObject -> Stream
                        .of(ReflectionUtils.getDeclaredMethods(
                                AopUtils.isAopProxy(toolObject) ? AopUtils.getTargetClass(toolObject) : toolObject.getClass()))
                        .filter(this::isToolAnnotatedMethod)
                        .filter(toolMethod -> !isFunctionalType(toolMethod))
                        .filter(ReflectionUtils.USER_DECLARED_METHODS::matches)
                        .map(toolMethod -> MethodToolCallback.builder()
                                .toolDefinition(ToolDefinitions.from(toolMethod))
                                .toolMetadata(ToolMetadata.from(toolMethod))
                                .toolMethod(toolMethod)
                                .toolObject(toolObject)
                                .toolCallResultConverter(ToolUtils.getToolCallResultConverter(toolMethod))
                                .build())
                        .toArray(ToolCallback[]::new))
                .flatMap(Stream::of)
                .toArray(ToolCallback[]::new);

        validateToolCallbacks(toolCallbacks);

        return toolCallbacks;
    }

    private boolean isFunctionalType(Method toolMethod) {
        var isFunction = ClassUtils.isAssignable(Function.class, toolMethod.getReturnType())
                || ClassUtils.isAssignable(Supplier.class, toolMethod.getReturnType())
                || ClassUtils.isAssignable(Consumer.class, toolMethod.getReturnType());

        if (isFunction) {
            logger.warn("Method {} is annotated with @Tool but returns a functional type. "
                    + "This is not supported and the method will be ignored.", toolMethod.getName());
        }

        return isFunction;
    }

    private boolean isToolAnnotatedMethod(Method method) {
        Tool annotation = AnnotationUtils.findAnnotation(method, Tool.class);
        return Objects.nonNull(annotation);
    }

    private void validateToolCallbacks(ToolCallback[] toolCallbacks) {
        List<String> duplicateToolNames = ToolUtils.getDuplicateToolNames(toolCallbacks);
        if (!duplicateToolNames.isEmpty()) {
            throw new IllegalStateException("Multiple tools with the same name (%s) found in sources: %s".formatted(
                    String.join(", ", duplicateToolNames),
                    this.toolObjects.stream().map(o -> o.getClass().getName()).collect(Collectors.joining(", "))));
        }
    }

    public static MethodToolCallbackProvider.Builder builder() {
        return new MethodToolCallbackProvider.Builder();
    }

    public static final class Builder {

        private List<Object> toolObjects;

        private Builder() {
        }

        public MethodToolCallbackProvider.Builder toolObjects(Object... toolObjects) {
            Assert.notNull(toolObjects, "toolObjects cannot be null");
            this.toolObjects = Arrays.asList(toolObjects);
            return this;
        }

        public MethodToolCallbackProvider build() {
            return new MethodToolCallbackProvider(this.toolObjects);
        }

    }
    
}
