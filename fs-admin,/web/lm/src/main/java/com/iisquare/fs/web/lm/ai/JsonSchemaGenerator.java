package com.iisquare.fs.web.lm.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.*;
import com.github.victools.jsonschema.generator.Module;
import com.github.victools.jsonschema.module.jackson.JacksonModule;
import com.github.victools.jsonschema.module.jackson.JacksonOption;
import com.github.victools.jsonschema.module.swagger2.Swagger2Module;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.util.json.JsonParser;
import org.springframework.ai.util.json.schema.SpringAiSchemaModule;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class JsonSchemaGenerator {

    private static final boolean PROPERTY_REQUIRED_BY_DEFAULT = true;

    private static final SchemaGenerator TYPE_SCHEMA_GENERATOR;

    private static final SchemaGenerator SUBTYPE_SCHEMA_GENERATOR;

    /*
     * Initialize JSON Schema generators.
     */
    static {
        Module jacksonModule = new JacksonModule(JacksonOption.RESPECT_JSONPROPERTY_REQUIRED);
        Module openApiModule = new Swagger2Module();
        Module springAiSchemaModule = PROPERTY_REQUIRED_BY_DEFAULT ? new SpringAiSchemaModule()
                : new SpringAiSchemaModule(SpringAiSchemaModule.Option.PROPERTY_REQUIRED_FALSE_BY_DEFAULT);

        SchemaGeneratorConfigBuilder schemaGeneratorConfigBuilder = new SchemaGeneratorConfigBuilder(
                SchemaVersion.DRAFT_2020_12, OptionPreset.PLAIN_JSON)
                .with(jacksonModule)
                .with(openApiModule)
                .with(springAiSchemaModule)
                .with(Option.EXTRA_OPEN_API_FORMAT_VALUES)
                .with(Option.PLAIN_DEFINITION_KEYS);

        SchemaGeneratorConfig typeSchemaGeneratorConfig = schemaGeneratorConfigBuilder.build();
        TYPE_SCHEMA_GENERATOR = new SchemaGenerator(typeSchemaGeneratorConfig);

        SchemaGeneratorConfig subtypeSchemaGeneratorConfig = schemaGeneratorConfigBuilder
                .without(Option.SCHEMA_VERSION_INDICATOR)
                .build();
        SUBTYPE_SCHEMA_GENERATOR = new SchemaGenerator(subtypeSchemaGeneratorConfig);
    }

    private JsonSchemaGenerator() {
    }

    /**
     * Generate a JSON Schema for a method's input parameters.
     */
    public static String generateForMethodInput(Method method, JsonSchemaGenerator.SchemaOption... schemaOptions) {
        ObjectNode schema = JsonParser.getObjectMapper().createObjectNode();
        schema.put("$schema", SchemaVersion.DRAFT_2020_12.getIdentifier());
        schema.put("type", "object");

        ObjectNode properties = schema.putObject("properties");
        List<String> required = new ArrayList<>();

        for (int i = 0; i < method.getParameterCount(); i++) {
            String parameterName = getMethodParameterName(method, i);
            Type parameterType = method.getGenericParameterTypes()[i];
            if (parameterType instanceof Class<?> parameterClass
                    && ClassUtils.isAssignable(ToolContext.class, parameterClass)) {
                // A ToolContext method parameter is not included in the JSON Schema
                // generation.
                // It's a special type used by Spring AI to pass contextual data to tools
                // outside the model interaction flow.
                continue;
            }
            if (isMethodParameterRequired(method, i)) {
                required.add(parameterName);
            }
            ObjectNode parameterNode = SUBTYPE_SCHEMA_GENERATOR.generateSchema(parameterType);
            String parameterDescription = getMethodParameterDescription(method, i);
            if (StringUtils.hasText(parameterDescription)) {
                parameterNode.put("description", parameterDescription);
            }
            properties.set(parameterName, parameterNode);
        }

        var requiredArray = schema.putArray("required");
        required.forEach(requiredArray::add);

        processSchemaOptions(schemaOptions, schema);

        return schema.toPrettyString();
    }

    /**
     * Generate a JSON Schema for a class type.
     */
    public static String generateForType(Type type, JsonSchemaGenerator.SchemaOption... schemaOptions) {
        Assert.notNull(type, "type cannot be null");
        ObjectNode schema = TYPE_SCHEMA_GENERATOR.generateSchema(type);
        if ((type == Void.class) && !schema.has("properties")) {
            schema.putObject("properties");
        }
        processSchemaOptions(schemaOptions, schema);
        return schema.toPrettyString();
    }

    private static void processSchemaOptions(JsonSchemaGenerator.SchemaOption[] schemaOptions, ObjectNode schema) {
        if (Stream.of(schemaOptions)
                .noneMatch(option -> option == JsonSchemaGenerator.SchemaOption.ALLOW_ADDITIONAL_PROPERTIES_BY_DEFAULT)) {
            schema.put("additionalProperties", false);
        }
        if (Stream.of(schemaOptions).anyMatch(option -> option == JsonSchemaGenerator.SchemaOption.UPPER_CASE_TYPE_VALUES)) {
            convertTypeValuesToUpperCase(schema);
        }
    }

    /**
     * Determines whether a property is required based on the presence of a series of *
     * annotations.
     *
     * <p>
     * <ul>
     * <li>{@code @ToolParam(required = ...)}</li>
     * <li>{@code @JsonProperty(required = ...)}</li>
     * <li>{@code @Schema(required = ...)}</li>
     * <li>{@code @Nullable}</li>
     * </ul>
     * <p>
     *
     * If none of these annotations are present, the default behavior is to consider the *
     * property as required.
     */
    private static boolean isMethodParameterRequired(Method method, int index) {
        Parameter parameter = method.getParameters()[index];

        var toolParamAnnotation = parameter.getAnnotation(ToolParam.class);
        if (toolParamAnnotation != null) {
            return toolParamAnnotation.required();
        }

        var propertyAnnotation = parameter.getAnnotation(JsonProperty.class);
        if (propertyAnnotation != null) {
            return propertyAnnotation.required();
        }

        var schemaAnnotation = parameter.getAnnotation(Schema.class);
        if (schemaAnnotation != null) {
            return schemaAnnotation.requiredMode() == Schema.RequiredMode.REQUIRED
                    || schemaAnnotation.requiredMode() == Schema.RequiredMode.AUTO || schemaAnnotation.required();
        }

        var nullableAnnotation = parameter.getAnnotation(Nullable.class);
        if (nullableAnnotation != null) {
            return false;
        }

        return PROPERTY_REQUIRED_BY_DEFAULT;
    }

    /**
     * Determines a property description based on the presence of a series of annotations.
     *
     * <p>
     * <ul>
     * <li>{@code @ToolParam(description = ...)}</li>
     * <li>{@code @JsonPropertyDescription(...)}</li>
     * <li>{@code @Schema(description = ...)}</li>
     * </ul>
     * <p>
     */
    @Nullable
    private static String getMethodParameterDescription(Method method, int index) {
        Parameter parameter = method.getParameters()[index];

        var toolParamAnnotation = parameter.getAnnotation(ToolParam.class);
        if (toolParamAnnotation != null && StringUtils.hasText(toolParamAnnotation.description())) {
            return toolParamAnnotation.description();
        }

        var jacksonAnnotation = parameter.getAnnotation(JsonPropertyDescription.class);
        if (jacksonAnnotation != null && StringUtils.hasText(jacksonAnnotation.value())) {
            return jacksonAnnotation.value();
        }

        var schemaAnnotation = parameter.getAnnotation(Schema.class);
        if (schemaAnnotation != null && StringUtils.hasText(schemaAnnotation.description())) {
            return schemaAnnotation.description();
        }

        return null;
    }

    private static String getMethodParameterName(Method method, int index) {
        Parameter parameter = method.getParameters()[index];
        var toolParamAnnotation = parameter.getAnnotation(ToolParam.class);
        if (toolParamAnnotation != null) {
            if (StringUtils.hasText(toolParamAnnotation.value())) {
                return toolParamAnnotation.value();
            }
            if (StringUtils.hasText(toolParamAnnotation.name())) {
                return toolParamAnnotation.name();
            }
        }
        return parameter.getName();
    }

    // Based on the method in ModelOptionsUtils.
    public static void convertTypeValuesToUpperCase(ObjectNode node) {
        if (node.isObject()) {
            node.fields().forEachRemaining(entry -> {
                JsonNode value = entry.getValue();
                if (value.isObject()) {
                    convertTypeValuesToUpperCase((ObjectNode) value);
                }
                else if (value.isArray()) {
                    value.elements().forEachRemaining(element -> {
                        if (element.isObject() || element.isArray()) {
                            convertTypeValuesToUpperCase((ObjectNode) element);
                        }
                    });
                }
                else if (value.isTextual() && entry.getKey().equals("type")) {
                    String oldValue = node.get("type").asText();
                    node.put("type", oldValue.toUpperCase());
                }
            });
        }
        else if (node.isArray()) {
            node.elements().forEachRemaining(element -> {
                if (element.isObject() || element.isArray()) {
                    convertTypeValuesToUpperCase((ObjectNode) element);
                }
            });
        }
    }

    /**
     * Options for generating JSON Schemas.
     */
    public enum SchemaOption {

        /**
         * Allow an object to contain additional key/values not defined in the schema.
         */
        ALLOW_ADDITIONAL_PROPERTIES_BY_DEFAULT,

        /**
         * Convert all "type" values to upper case.
         */
        UPPER_CASE_TYPE_VALUES

    }

}
