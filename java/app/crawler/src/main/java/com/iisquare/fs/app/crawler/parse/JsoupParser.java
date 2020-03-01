package com.iisquare.fs.app.crawler.parse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.lang.reflect.Method;
import java.util.*;

/**
 * <a href="https://jsoup.org/cookbook/extracting-data/selector-syntax">Use selector-syntax to find elements</a>
 */
public class JsoupParser extends Parser {

    public static final String PROTOCOL = "jsoup"; // 标识符

    public static final String TYPE_BLANK = " "; // 空格（无效字符）
    public static final String TYPE_TAB = "\t"; // 制表符（无效字符）
    public static final String TYPE_LINE = "\n"; // 换行（无效字符）
    public static final String TYPE_ENTER = "\r"; // 回车（无效字符）
    public static final String TYPE_QUOTES = "\""; // 引号（字符标识）
    public static final String TYPE_COLON = ":"; // 冒号（分割键值对）
    public static final String TYPE_COMMA = ","; // 逗号（分割参数）
    public static final String TYPE_DOT = "."; // 句号（分割调用）
    public static final String TYPE_ESCAPE = "\\"; // 转义符（转移字符）

    public static final String TYPE_OBJECT_BEGIN = "{"; // 左花括号（对象开始）
    public static final String TYPE_OBJECT_END = "}"; // 右花括号（对象结束）
    public static final String TYPE_ARRAY_BEGIN = "["; // 左中括号（数组开始）
    public static final String TYPE_ARRAY_END = "]"; // 右中括号（数组结束）
    public static final String TYPE_METHOD_BEGIN = "("; // 左括号（方法开始）
    public static final String TYPE_METHOD_END = ")"; // 右括号（方法结束）

    public static final String TYPE_NUMBER = "NUMBER"; // 数值
    public static final String TYPE_STRING = "STRING"; // 字符串
    public static final String TYPE_OBJECT = "OBJECT"; // 对象
    public static final String TYPE_ARRAY = "ARRAY"; // 数组
    public static final String TYPE_METHOD = "METHOD"; // 方法
    public static final String TYPE_KEY = "KEY"; // 键值
    public static final String TYPE_PARAMETER = "PARAMETER"; // 参数

    private JsonNode expression;

    public JsoupParser() {

    }

    @Override
    public JsonNode expression() {
        return this.expression;
    }

    public ObjectNode extractObject(Object dom, JsonNode expression) throws Exception {
        ObjectNode node = DPUtil.objectNode();
        Iterator<Map.Entry<String, JsonNode>> iterator = expression.fields();
        while (iterator.hasNext()) {
            Map.Entry<String, JsonNode> entry = iterator.next();
            Object result = invoke(dom, entry.getValue());
            if (result instanceof Boolean) {
                node.put(entry.getKey(), (Boolean) result);
            } else if (result instanceof Integer) {
                node.put(entry.getKey(), (Integer) result);
            } else if (result instanceof String) {
                node.put(entry.getKey(), (String) result);
            } else if(result instanceof Collection) {
                ArrayNode array = node.putArray(entry.getKey());
                for (Object item : (Collection) result) {
                    array.add(item.toString());
                }
            } else if (result instanceof ObjectNode) {
                node.replace(entry.getKey(), (ObjectNode) result);
            } else if (result instanceof ArrayNode) {
                node.replace(entry.getKey(), (ArrayNode) result);
            } else {
                node.put(entry.getKey(), result.toString());
            }
        }
        return node;
    }

    public Object invoke(Object dom, JsonNode expression) throws Exception {
        if (null == expression || !TYPE_METHOD.equals(expression.get("type").asText())) return dom;
        List<Class> classes = new ArrayList<>();
        List<Object> params = new ArrayList<>();
        Iterator<JsonNode> iterator = expression.get("parameters").elements();
        while (iterator.hasNext()) {
            JsonNode parameter = iterator.next();
            String type= parameter.get("valueType").asText();
            switch (type) {
                case TYPE_STRING:
                    classes.add(String.class);
                    params.add(parameter.get("value").asText());
                    break;
                case TYPE_NUMBER:
                    classes.add(int.class);
                    params.add(Integer.valueOf(parameter.get("value").asText()));
                    break;
                default:
                    throw new Exception("parameter's type " + type + " not supported");
            }
        }
        Method method = dom.getClass().getMethod(expression.get("name").asText(), classes.toArray(new Class[classes.size()]));
        dom = method.invoke(dom, params.toArray(new Object[params.size()]));
        if (expression.has("pipeline")) return invoke(dom, expression.get("pipeline"));
        if (expression.has("subject")) return extract(dom, expression.get("subject"));
        return dom;
    }

    public JsonNode extractArray(Object dom, JsonNode expression) throws Exception {
        ArrayNode nodes = DPUtil.arrayNode();
        if (dom instanceof Elements) {
            for (Element element : (Elements) dom) {
                nodes.add(extractObject(element, expression));
            }
        } else if(dom instanceof Element) {
            nodes.add(extractObject(dom, expression));
        }
        return nodes;
    }

    public JsonNode extract(Object dom, JsonNode expression) throws Exception {
        String type = expression.get("type").asText();
        switch (type) {
            case TYPE_OBJECT:
                return extractObject(dom, expression.get("properties"));
            case TYPE_ARRAY:
                if (!(dom instanceof Elements)) return null;
                return extractArray(dom, expression.get("properties"));
            default:
                throw new Exception("expression type " + type + " not supported");
        }
    }

    @Override
    public JsonNode parse(String data) throws Exception {
        return parse(Jsoup.parse(data));
    }

    public JsonNode parse(Document document) throws Exception {
        return reduce(extract(document, expression));
    }

    public JsonNode reduce(JsonNode data) {
        if (null == data || data.isNull()) return data;
        if (!data.isObject() && !data.isArray()) return data;
        ObjectNode result = DPUtil.objectNode();
        Iterator<Map.Entry<String, JsonNode>> iterator = data.fields();
        while (iterator.hasNext()) {
            Map.Entry<String, JsonNode> entry = iterator.next();
            String key = entry.getKey();
            JsonNode item = entry.getValue();
            if (item.isArray()) {
                ArrayNode array = DPUtil.arrayNode();
                Iterator<JsonNode> elements = item.elements();
                while (elements.hasNext()) {
                    array.add(reduce(elements.next()));
                }
                result.replace(key, array);
            } else if (item.isObject()) {
                JsonNode value = reduce(item);
                if (key.startsWith("-")) {
                    result.setAll((ObjectNode) value);
                } else {
                    result.replace(key, value);
                }
            } else {
                result.replace(key, item);
            }
        }
        return result;
    }

    @Override
    public Parser unload() {
        this.expression = null;
        return this;
    }

    @Override
    public Parser load(String template) throws Exception {
        this.unload();
        this.expression = expression(template);
        return this;
    }

    public ArrayNode parameter(Deque<ObjectNode> deque) {
        ArrayNode parameters = DPUtil.arrayNode();
        while (deque.size() > 0) {
            ObjectNode item = deque.pollLast();
            String itemType = item.get("type").asText();
            switch (itemType) {
                case TYPE_PARAMETER:
                    parameters.add(item);
                    break;
                case TYPE_METHOD_BEGIN:
                case TYPE_COMMA:
                    continue;
                case TYPE_METHOD_END:
                    return parameters;
                default:
                    return null;
            }
        }
        return null;
    }

    public ObjectNode method(Deque<ObjectNode> deque) {
        ObjectNode method = DPUtil.objectNode();
        ObjectNode root = method;
        while (deque.size() > 0) {
            ObjectNode item = deque.pollLast();
            String itemType = item.get("type").asText();
            switch (itemType) {
                case TYPE_METHOD:
                    method.put("name", item.get("name").asText());
                    method.put("type", itemType);
                    method.replace("parameters", parameter(deque));
                    break;
                case TYPE_DOT:
                    method = method.putObject("pipeline");
                    break;
                case TYPE_COLON:
                    continue;
                default:
                    deque.offerLast(item);
                    return root;
            }
        }
        return null;
    }

    public ObjectNode tree(Deque<ObjectNode> deque) {
        Deque<ObjectNode> result = new LinkedList<>();
        while (deque.size() > 0) {
            ObjectNode item = deque.pollLast();
            String itemType = item.get("type").asText();
            switch (itemType) {
                case TYPE_OBJECT_BEGIN:
                case TYPE_ARRAY_BEGIN:
                    ObjectNode node = DPUtil.objectNode();
                    node.put("type", itemType.equals(TYPE_OBJECT_BEGIN) ? TYPE_OBJECT : TYPE_ARRAY);
                    node.putObject("properties");
                    if (result.size() > 0) {
                        result.pop().replace("subject", node);
                    }
                    result.push(node);
                    break;
                case TYPE_KEY:
                    ObjectNode properties = (ObjectNode) result.peek().get("properties");
                    ObjectNode method =  method(deque);
                    properties.replace(item.get("name").asText(), method);
                    result.push(method);
                    break;
                case TYPE_COMMA:
                    result.pop();
                    continue;
                case TYPE_OBJECT_END:
                case TYPE_ARRAY_END:
                    result.pop();
                    break;
                default:
                    return null;
            }
        }
        return result.pop();
    }

    public int trim(String[] strings, int index) {
        for (int i = index; i < strings.length; i++) {
            String str = strings[i];
            if (str.equals(TYPE_BLANK)) continue;
            if (str.equals(TYPE_TAB)) continue;
            if (str.equals(TYPE_ENTER)) continue;
            return i;
        }
        return index;
    }

    private Exception exception(String message, String str, int line, int column) {
        return new Exception("parse error:" + message + " in line " + ++line + ", column " + ++column + ", char " + str);
    }

    public JsonNode expression(String expression) throws Exception {
        Deque<ObjectNode> deque = new LinkedList<>();
        String[] lines = expression.split(TYPE_LINE);
        for (int line = 0; line < lines.length; line++) {
            if (lines[line].matches("^\\s*$")) continue;
            String[] strings = lines[line].split("");
            for (int column = 0; column <= strings.length; column++) { // 每行向后多循环一次
                ObjectNode parentNode = deque.size() > 0 ? deque.peek() : null;
                String parentType = parentNode == null ? "" : parentNode.get("type").asText();
                if (parentType != TYPE_STRING && parentType != TYPE_ESCAPE) {
                    column = trim(strings, column); // 移除无效字符
                }
                if (column == strings.length) { // 行尾换行符
                    if (line + 1 < lines.length) {
                        continue; // 继续下一行处理
                    }
                    return tree(deque); // 格式化
                }
                String str = strings[column];
                if (TYPE_LINE.equals(str) || TYPE_ENTER.equals(str)) {
                    switch (parentType) {
                        case TYPE_STRING: break;
                        default: continue;
                    }
                }
                switch (parentType) {
                    case TYPE_ESCAPE: // 转移符只能存在于字符串中
                        deque.pop();
                        parentNode = deque.peek();
                        parentType = parentNode.get("type").asText();
                        switch (parentType) {
                            case TYPE_STRING: // 将转义符写入到字符串中
                                parentNode.put("value", parentNode.get("value").asText() + str);
                                break;
                            default:
                                throw exception("expected string", str, line, column);
                        }
                        break;
                    case TYPE_COLON: // 冒号或句号标识方法的开始
                    case TYPE_DOT:
                        deque.push(DPUtil.objectNode().put("type", TYPE_METHOD).put("name", str));
                        break;
                    case TYPE_OBJECT_BEGIN: // 对象开始、方法开始、逗号之后为键名称
                    case TYPE_ARRAY_BEGIN:
                    case TYPE_COMMA:
                        deque.push(DPUtil.objectNode().put("type", TYPE_KEY).put("name", str));
                        break;
                    case TYPE_KEY: // 键名称以冒号结尾
                        switch (str) {
                            case TYPE_COLON:
                                deque.push(DPUtil.objectNode().put("type", str));
                                break;
                            default:
                                parentNode.put("name", parentNode.get("name").asText() + str);
                        }
                        break;
                    case TYPE_NUMBER: // 数值仅作为方法参数，以逗号或右括号结束
                        switch (str) {
                            case TYPE_COMMA:
                            case TYPE_METHOD_END:
                                deque.pop();
                                deque.push(DPUtil.objectNode().put("type", TYPE_PARAMETER)
                                    .put("value", parentNode.get("value").asText()).put("valueType", parentType));
                                deque.push(DPUtil.objectNode().put("type", str));
                                break;
                            default:
                                parentNode.put("value", parentNode.get("value").asText() + str);
                        }
                        break;
                    case TYPE_STRING: // 字符串使用双引号标识
                        switch (str) {
                            case TYPE_ESCAPE:
                                deque.push(DPUtil.objectNode().put("type", str));
                                break;
                            case TYPE_QUOTES:
                                deque.pop();
                                deque.push(DPUtil.objectNode().put("type", TYPE_PARAMETER)
                                    .put("value", parentNode.get("value").asText()).put("valueType", parentType));
                                break;
                            default:
                                parentNode.put("value", parentNode.get("value").asText() + str);
                        }
                        break;
                    case TYPE_PARAMETER: // 将数值或字符串提取为参数
                        switch (str) {
                            case TYPE_COMMA:
                            case TYPE_METHOD_END:
                                deque.push(DPUtil.objectNode().put("type", str));
                                break;
                            default:
                                throw exception("expected ',' or ')'", str, line, column);
                        }
                        break;
                    case TYPE_METHOD: // 方法名称在冒号或逗号之后，在左括号之前
                        switch (str) {
                            case TYPE_METHOD_BEGIN:
                                deque.push(DPUtil.objectNode().put("type", str));
                                break;
                            default:
                                parentNode.put("name", parentNode.get("name").asText() + str);
                        }
                        break;
                    case TYPE_METHOD_BEGIN: // 方法内支持数值、字符串或空参数
                        switch (str) {
                            case TYPE_METHOD_END:
                                deque.push(DPUtil.objectNode().put("type", str));
                                break;
                            case TYPE_QUOTES:
                                deque.push(DPUtil.objectNode().put("type", TYPE_STRING).put("value", ""));
                                break;
                            default:
                                deque.push(DPUtil.objectNode().put("type", TYPE_NUMBER).put("value", str));
                        }
                        break;
                    case TYPE_METHOD_END:
                        switch (str) {
                            case TYPE_DOT:
                            case TYPE_COMMA:
                            case TYPE_ARRAY_BEGIN:
                            case TYPE_ARRAY_END:
                            case TYPE_OBJECT_BEGIN:
                            case TYPE_OBJECT_END:
                                deque.push(DPUtil.objectNode().put("type", str));
                                break;
                            default:
                                throw exception("expected '.' or ',' or '[]' or '{}'", str, line, column);
                        }
                        break;
                    case TYPE_ARRAY_END:
                    case TYPE_OBJECT_END:
                        switch (str) {
                            case TYPE_COMMA:
                            case TYPE_ARRAY_END:
                            case TYPE_OBJECT_END:
                                deque.push(DPUtil.objectNode().put("type", str));
                                break;
                            default:
                                throw exception("expected ',' or ']' or '}'", str, line, column);
                        }
                        break;
                    default: // 新的解析开始
                        switch (str) {
                            case TYPE_OBJECT_BEGIN:
                            case TYPE_ARRAY_BEGIN:
                                deque.push(DPUtil.objectNode().put("type", str));
                                break;
                            case TYPE_COMMA:
                                deque.push(DPUtil.objectNode().put("type", str));
                                break;
                            default:
                                throw exception("must start with '{' or '['", str, line, column);
                        }
                }
            }
        }
        return null;
    }



}
