package com.iisquare.fs.web.oa.service;

import com.deepoove.poi.XWPFTemplate;
import com.deepoove.poi.config.Configure;
import com.deepoove.poi.config.ConfigureBuilder;
import com.deepoove.poi.plugin.table.LoopRowTableRenderPolicy;
import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.oa.entity.FormFrame;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFHeaderFooter;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PrintRenderService {

    private static final Pattern TAG_PATTERN = Pattern.compile(
            "\\{\\{\\s*([^{}]+?)\\s*}}|(?<!\\{)\\{\\s*([^{}]+?)\\s*}(?!})"
    );
    private static final Pattern DISPLAY_TAG_PATTERN = Pattern.compile(
            "\\{\\{\\s*([^{}]+?)\\s*}}|(?<!\\{)\\{\\s*([^{}]+?)\\s*}(?!})|\\[([^\\[\\]]+)]"
    );
    private static final Pattern REWRITE_PATTERN = Pattern.compile(
            "\\{\\{\\s*([?/]?)([^{}]+?)\\s*}}|(?<!\\{)\\{\\s*([?/]?)([^{}]+?)\\s*}(?!})|\\[([^\\[\\]]+)]"
    );
    private static final Pattern SAFE_FIELD_PATTERN = Pattern.compile(
            "^[A-Za-z_$][A-Za-z0-9_$]*(?:\\.[A-Za-z_$][A-Za-z0-9_$]*)*$"
    );
    private static final Pattern SQUARE_TAG_PATTERN = Pattern.compile("\\[([^\\[\\]]+)]");
    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    public byte[] render(byte[] source, Map<String, Object> data, List<String> listFields) throws Exception {
        String templateText = plainText(source);
        Set<String> dataPaths = collectDataPaths(data, "");
        Map<String, String> aliases = buildAliases(templateText, dataPaths);
        Map<String, Object> renderData = aliasData(data, aliases);
        byte[] normalizedSource = rewriteTagNames(source, aliases);
        String normalizedText = aliases.isEmpty() ? templateText : plainText(normalizedSource);
        boolean doubleBraces = hasDoubleBraceTag(normalizedText);
        ConfigureBuilder builder = doubleBraces ? Configure.builder() : Configure.builder().buildGramer("{", "}");
        // poi-tl 的表格行循环标签使用 {{items}} + [字段] 约定，只在确有该标签时绑定。
        for (String field : listFields) {
            String renderField = aliases.getOrDefault(field, field);
            Pattern rowTag = doubleBraces
                    ? Pattern.compile("\\{\\{\\s*" + Pattern.quote(renderField) + "\\s*}}")
                    : Pattern.compile("(?<!\\{)\\{\\s*" + Pattern.quote(renderField) + "\\s*}(?!})");
            if(rowTag.matcher(normalizedText).find()) {
                builder.bind(renderField, new LoopRowTableRenderPolicy());
            }
        }
        Configure configure = builder.build();
        byte[] rendered;
        try (ByteArrayInputStream input = new ByteArrayInputStream(normalizedSource);
             ByteArrayOutputStream output = new ByteArrayOutputStream();
             XWPFTemplate template = XWPFTemplate.compile(input, configure)) {
            template.render(renderData);
            template.write(output);
            rendered = output.toByteArray();
        }
        return replaceResidualTokens(rendered, renderData);
    }

    public List<Map<String, Object>> fields(FormFrame frame) {
        if(null == frame) return Collections.emptyList();
        JsonNode content = DPUtil.parseJSON(frame.getContent());
        if(null == content || !content.isObject()) return Collections.emptyList();
        List<Map<String, Object>> result = new ArrayList<>();
        inspectWidgets(content.at("/widgets"), result, new LinkedHashSet<>(), "");
        return result;
    }

    /**
     * 为业务数据补上模板中可见的中文字段变量，同时保留原始字段 ID 供内部处理。
     */
    public Map<String, Object> aliasFieldData(
            Map<String, Object> data, List<Map<String, Object>> fields) {
        if(null == data || null == fields || fields.isEmpty()) return data;
        Map<String, Object> result = new LinkedHashMap<>(data);
        aliasFieldData(data, result, fields);
        return result;
    }

    public List<String> listFields(List<Map<String, Object>> fields) {
        List<String> result = new ArrayList<>();
        for (Map<String, Object> field : fields) {
            if(Boolean.TRUE.equals(field.get("list"))) {
                String name = DPUtil.parseString(field.get("field"));
                if(!DPUtil.empty(name)) result.add(name);
            }
            Object children = field.get("children");
            if(children instanceof List) {
                result.addAll(listFields((List<Map<String, Object>>) children));
            }
        }
        return result;
    }

    public Map<String, Object> sample(List<Map<String, Object>> fields) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map<String, Object> field : fields) {
            String name = DPUtil.parseString(field.get("field"));
            if(DPUtil.empty(name)) continue;
            if(Boolean.TRUE.equals(field.get("list"))) {
                List<Map<String, Object>> children = castMaps(field.get("children"));
                List<Map<String, Object>> rows = new ArrayList<>();
                rows.add(sample(children));
                rows.add(sample(children));
                result.put(name, rows);
            } else {
                result.put(name, sampleValue(field));
            }
        }
        return result;
    }

    public void decorate(Map<String, Object> model) {
        if(null == model) return;
        if(!model.containsKey("id") && model.containsKey("_id")) model.put("id", model.get("_id"));
        if(!model.containsKey("_id") && model.containsKey("id")) model.put("_id", model.get("id"));
        decorateDate(model, "createdTime");
        decorateDate(model, "updatedTime");
    }

    public List<Map<String, Object>> placeholders(byte[] source) throws Exception {
        LinkedHashMap<String, Map<String, Object>> result = new LinkedHashMap<>();
        try (ByteArrayInputStream input = new ByteArrayInputStream(source);
             XWPFDocument document = new XWPFDocument(input)) {
            collectTags(document, result);
            for (XWPFHeaderFooter headerFooter : document.getHeaderList()) collectTags(headerFooter, result);
            for (XWPFHeaderFooter headerFooter : document.getFooterList()) collectTags(headerFooter, result);
        }
        return new ArrayList<>(result.values());
    }

    public List<Map<String, Object>> outline(byte[] source) throws Exception {
        List<Map<String, Object>> result = new ArrayList<>();
        Map<String, Integer> repeats = new HashMap<>();
        Pattern headingStyle = Pattern.compile("(?:heading|标题)\\s*([1-6])", Pattern.CASE_INSENSITIVE);
        try (ByteArrayInputStream input = new ByteArrayInputStream(source);
             XWPFDocument document = new XWPFDocument(input)) {
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                String text = DPUtil.trim(paragraph.getText());
                if(DPUtil.empty(text)) continue;
                int level = 0;
                if(paragraph.getCTP().isSetPPr() && paragraph.getCTP().getPPr().isSetOutlineLvl()) {
                    level = paragraph.getCTP().getPPr().getOutlineLvl().getVal().intValue() + 1;
                }
                if(level < 1 || level > 6) {
                    String style = DPUtil.parseString(paragraph.getStyle()) + " "
                            + DPUtil.parseString(paragraph.getStyleID());
                    Matcher matcher = headingStyle.matcher(style);
                    if(matcher.find()) level = Integer.parseInt(matcher.group(1));
                }
                if(level < 1 || level > 6) continue;
                int repeat = repeats.getOrDefault(text, 0);
                repeats.put(text, repeat + 1);
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("name", text);
                item.put("level", level);
                item.put("repeatIndex", repeat);
                result.add(item);
            }
        }
        return result;
    }

    public List<String> unresolved(byte[] source, Map<String, Object> data) throws Exception {
        String text = plainText(source);
        LinkedHashSet<String> result = new LinkedHashSet<>();
        Deque<String> sections = new ArrayDeque<>();
        Matcher matcher = TAG_PATTERN.matcher(text);
        while (matcher.find()) {
            String doubleRaw = matcher.group(1);
            String raw = (null != doubleRaw ? doubleRaw : matcher.group(2)).trim();
            if(DPUtil.empty(raw)) continue;
            char prefix = raw.charAt(0);
            if(prefix == '?') {
                sections.push(raw.substring(1).trim());
                continue;
            }
            if(prefix == '/') {
                if(!sections.isEmpty()) sections.pop();
                continue;
            }
            if(prefix == '#' || prefix == '@' || prefix == '*' || prefix == '+' || raw.startsWith("=")) continue;
            if(sections.isEmpty()) {
                if(!resolve(data, raw).found) result.add(raw);
                continue;
            }
            // 列表内的字段相对于当前元素解析；空列表不产生缺失提示。
            ResolvedValue sectionValue = resolve(data, sections.peek());
            if(sectionValue.found && sectionValue.value instanceof List) {
                List<?> list = (List<?>) sectionValue.value;
                if(!list.isEmpty()) {
                    Object first = list.get(0);
                    if(first instanceof Map && !resolve((Map<String, Object>) first, raw).found) {
                        result.add(sections.peek() + "." + raw);
                    }
                }
            }
        }
        return new ArrayList<>(result);
    }

    private void inspectWidgets(
            JsonNode widgets,
            List<Map<String, Object>> result,
            Set<String> usedVariables,
            String parentPath) {
        if(null == widgets || !widgets.isArray()) return;
        for (JsonNode widget : widgets) {
            String type = widget.at("/type").asText();
            JsonNode options = widget.at("/options");
            if("grid".equals(type)) {
                for (JsonNode item : options.at("/items")) {
                    inspectWidgets(item.at("/widgets"), result, usedVariables, parentPath);
                }
                continue;
            }
            String field = options.at("/field").asText();
            if(DPUtil.empty(field)) continue;
            String label = DPUtil.trim(widget.at("/label").asText());
            if(DPUtil.empty(label)) label = field;
            String variable = uniqueVariable(label, field, usedVariables);
            String path = DPUtil.empty(parentPath) ? variable : parentPath + "." + variable;
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("field", variable);
            item.put("id", field);
            item.put("label", label);
            item.put("path", path);
            item.put("component", componentName(type, field));
            item.put("type", type);
            item.put("list", "subform".equals(type));
            item.put("required", options.at("/required").asBoolean(false));
            item.put("multiple", "checkbox".equals(type) || options.at("/multiple").asBoolean(false));
            item.put("placeholder", DPUtil.trim(options.at("/placeholder").asText()));
            item.put("optionCount", options.at("/items").isArray() ? options.at("/items").size() : 0);
            item.put("general", isGeneralField(field));
            if("subform".equals(type)) {
                List<Map<String, Object>> children = new ArrayList<>();
                inspectWidgets(
                        options.at("/formInfo/widgets"),
                        children,
                        new LinkedHashSet<>(),
                        path);
                item.put("children", children);
            }
            result.add(item);
        }
    }

    private String componentName(String type, String field) {
        switch (field) {
            case "_id": return "主键";
            case "createdUid":
            case "createdUidName":
            case "updatedUid":
            case "updatedUidName":
            case "bpmStartUserId":
            case "bpmStartUserIdName":
                return "人员";
            case "createdTime":
            case "updatedTime":
                return "时间";
            case "bpmWorkflowId":
            case "bpmWorkflowIdName":
                return "流程";
            case "bpmInstanceId":
                return "流程实例";
            default:
                break;
        }
        switch (type) {
            case "text": return "单行文本";
            case "textarea": return "多行文本";
            case "password": return "密码";
            case "number": return "数值";
            case "radio": return "单选";
            case "checkbox": return "多选";
            case "switch": return "开关";
            case "select": return "选择器";
            case "subform": return "子表单";
            case "date": return "日期";
            case "datetime": return "日期时间";
            case "time": return "时间";
            default: return DPUtil.empty(type) ? "未知组件" : type;
        }
    }

    private boolean isGeneralField(String field) {
        return "_id".equals(field)
                || field.startsWith("created")
                || field.startsWith("updated")
                || field.startsWith("bpm");
    }

    private String uniqueVariable(String label, String field, Set<String> usedVariables) {
        String base = DPUtil.trim(label)
                .replace("{", "")
                .replace("}", "")
                .replace("[", "")
                .replace("]", "")
                .replaceAll("^[?/#@*+=]+", "")
                .replace(".", "·")
                .replaceAll("\\s+", "");
        if(DPUtil.empty(base)) base = field;
        String variable = base;
        int index = 2;
        while(!usedVariables.add(variable)) {
            variable = base + "（" + index++ + "）";
        }
        return variable;
    }

    private void aliasFieldData(
            Map<String, Object> source,
            Map<String, Object> target,
            List<Map<String, Object>> fields) {
        for (Map<String, Object> field : fields) {
            String id = DPUtil.parseString(field.get("id"));
            String variable = DPUtil.parseString(field.get("field"));
            if(DPUtil.empty(id) || DPUtil.empty(variable) || !source.containsKey(id)) continue;
            Object value = source.get(id);
            if(Boolean.TRUE.equals(field.get("list")) && value instanceof List) {
                List<Object> rows = new ArrayList<>();
                List<Map<String, Object>> children = castMaps(field.get("children"));
                for (Object rowValue : (List<?>) value) {
                    if(!(rowValue instanceof Map)) {
                        rows.add(rowValue);
                        continue;
                    }
                    @SuppressWarnings("unchecked")
                    Map<String, Object> rowSource = (Map<String, Object>) rowValue;
                    Map<String, Object> row = new LinkedHashMap<>(rowSource);
                    aliasFieldData(rowSource, row, children);
                    rows.add(row);
                }
                target.put(variable, rows);
            } else {
                target.put(variable, value);
            }
        }
    }

    private Object sampleValue(Map<String, Object> field) {
        String type = DPUtil.parseString(field.get("type"));
        String label = DPUtil.parseString(field.get("label"));
        switch (type) {
            case "number":
                return 1;
            case "switch":
                return true;
            case "checkbox":
                return Arrays.asList("示例1", "示例2");
            default:
                return DPUtil.empty(label) ? "示例值" : label;
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> castMaps(Object value) {
        return value instanceof List ? (List<Map<String, Object>>) value : Collections.emptyList();
    }

    private void decorateDate(Map<String, Object> model, String field) {
        Object value = model.get(field);
        if(!(value instanceof Number)) return;
        long timestamp = ((Number) value).longValue();
        if(timestamp < 1) return;
        model.put(field + "Text", new SimpleDateFormat(DATE_FORMAT).format(new Date(timestamp)));
    }

    private String plainText(byte[] source) throws Exception {
        StringBuilder builder = new StringBuilder();
        try (ByteArrayInputStream input = new ByteArrayInputStream(source);
             XWPFDocument document = new XWPFDocument(input)) {
            for (XWPFParagraph paragraph : document.getParagraphs()) builder.append(paragraph.getText()).append('\n');
            for (XWPFTable table : document.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        for (XWPFParagraph paragraph : cell.getParagraphs()) {
                            builder.append(paragraph.getText()).append('\n');
                        }
                    }
                }
            }
            for (XWPFHeaderFooter headerFooter : document.getHeaderList()) {
                for (XWPFParagraph paragraph : headerFooter.getParagraphs()) {
                    builder.append(paragraph.getText()).append('\n');
                }
            }
            for (XWPFHeaderFooter headerFooter : document.getFooterList()) {
                for (XWPFParagraph paragraph : headerFooter.getParagraphs()) {
                    builder.append(paragraph.getText()).append('\n');
                }
            }
        }
        return builder.toString();
    }

    private boolean hasDoubleBraceTag(String text) {
        if(DPUtil.empty(text)) return false;
        Matcher matcher = TAG_PATTERN.matcher(text);
        while (matcher.find()) {
            if(null != matcher.group(1)) return true;
        }
        return false;
    }

    private Map<String, String> buildAliases(String templateText, Set<String> dataPaths) {
        LinkedHashMap<String, String> aliases = new LinkedHashMap<>();
        Matcher matcher = TAG_PATTERN.matcher(templateText);
        while (matcher.find()) {
            String raw = (null != matcher.group(1) ? matcher.group(1) : matcher.group(2)).trim();
            if(DPUtil.empty(raw)) continue;
            char prefix = raw.charAt(0);
            if("?/@*+=".indexOf(prefix) >= 0) raw = raw.substring(1).trim();
            if(DPUtil.empty(raw) || "#this".equals(raw) || SAFE_FIELD_PATTERN.matcher(raw).matches()
                    || !dataPaths.contains(raw)) continue;
            aliases.computeIfAbsent(raw, key -> "__pt_" + (aliases.size() + 1));
        }
        Matcher squareMatcher = SQUARE_TAG_PATTERN.matcher(templateText);
        while (squareMatcher.find()) {
            String raw = squareMatcher.group(1).trim();
            if(DPUtil.empty(raw) || SAFE_FIELD_PATTERN.matcher(raw).matches() || !dataPaths.contains(raw)) continue;
            aliases.computeIfAbsent(raw, key -> "__pt_" + (aliases.size() + 1));
        }
        return aliases;
    }

    private Set<String> collectDataPaths(Object value, String prefix) {
        Set<String> result = new LinkedHashSet<>();
        if(value instanceof Map) {
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                String key = DPUtil.parseString(entry.getKey());
                if(DPUtil.empty(key)) continue;
                String path = DPUtil.empty(prefix) ? key : prefix + "." + key;
                result.add(key);
                result.add(path);
                result.addAll(collectDataPaths(entry.getValue(), path));
            }
        } else if(value instanceof List) {
            for (Object item : (List<?>) value) result.addAll(collectDataPaths(item, prefix));
        }
        return result;
    }

    private byte[] rewriteTagNames(byte[] source, Map<String, String> aliases) throws Exception {
        if(aliases.isEmpty()) return source;
        try (ByteArrayInputStream input = new ByteArrayInputStream(source);
             XWPFDocument document = new XWPFDocument(input);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            for (XWPFParagraph paragraph : document.getParagraphs()) rewriteParagraphTags(paragraph, aliases);
            for (XWPFTable table : document.getTables()) rewriteTableTags(table, aliases);
            for (XWPFHeaderFooter headerFooter : document.getHeaderList()) rewriteHeaderFooterTags(headerFooter, aliases);
            for (XWPFHeaderFooter headerFooter : document.getFooterList()) rewriteHeaderFooterTags(headerFooter, aliases);
            document.write(output);
            return output.toByteArray();
        }
    }

    private void rewriteHeaderFooterTags(XWPFHeaderFooter headerFooter, Map<String, String> aliases) {
        for (XWPFParagraph paragraph : headerFooter.getParagraphs()) rewriteParagraphTags(paragraph, aliases);
        for (XWPFTable table : headerFooter.getTables()) rewriteTableTags(table, aliases);
    }

    private void rewriteTableTags(XWPFTable table, Map<String, String> aliases) {
        for (XWPFTableRow row : table.getRows()) {
            for (XWPFTableCell cell : row.getTableCells()) {
                for (XWPFParagraph paragraph : cell.getParagraphs()) rewriteParagraphTags(paragraph, aliases);
                for (XWPFTable nested : cell.getTables()) rewriteTableTags(nested, aliases);
            }
        }
    }

    private void rewriteParagraphTags(XWPFParagraph paragraph, Map<String, String> aliases) {
        String text = paragraph.getText();
        if(DPUtil.empty(text)) return;
        Matcher matcher = REWRITE_PATTERN.matcher(text);
        while (matcher.find()) {
            String prefix;
            String original;
            String replacement;
            if(null != matcher.group(1)) {
                prefix = matcher.group(1);
                original = matcher.group(2).trim();
                String alias = aliases.get(original);
                if(null == alias) continue;
                replacement = "{{" + prefix + alias + "}}";
            } else if(null != matcher.group(3)) {
                prefix = matcher.group(3);
                original = matcher.group(4).trim();
                String alias = aliases.get(original);
                if(null == alias) continue;
                replacement = "{" + prefix + alias + "}";
            } else {
                original = matcher.group(5).trim();
                String alias = aliases.get(original);
                if(null == alias) continue;
                replacement = "[" + alias + "]";
            }
            replaceAcrossRuns(paragraph, matcher.start(), matcher.end(), replacement);
            text = paragraph.getText();
            matcher = REWRITE_PATTERN.matcher(text);
        }
    }

    private Map<String, Object> aliasData(Map<String, Object> data, Map<String, String> aliases) {
        if(aliases.isEmpty()) return data;
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) aliasValue(data, aliases);
        for (String original : aliases.keySet()) {
            ResolvedValue value = resolve(data, original);
            if(value.found) result.putIfAbsent(aliases.get(original), value.value);
        }
        return result;
    }

    private Object aliasValue(Object value, Map<String, String> aliases) {
        if(value instanceof Map) {
            Map<String, Object> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                String key = DPUtil.parseString(entry.getKey());
                Object child = aliasValue(entry.getValue(), aliases);
                result.put(key, child);
                String alias = aliases.get(key);
                if(null != alias) result.put(alias, child);
            }
            return result;
        }
        if(value instanceof List) {
            List<Object> result = new ArrayList<>();
            for (Object item : (List<?>) value) result.add(aliasValue(item, aliases));
            return result;
        }
        return value;
    }

    private byte[] replaceResidualTokens(byte[] source, Map<String, Object> data) throws Exception {
        try (ByteArrayInputStream input = new ByteArrayInputStream(source);
             XWPFDocument document = new XWPFDocument(input);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            for (XWPFParagraph paragraph : document.getParagraphs()) replaceParagraphTokens(paragraph, data);
            for (XWPFTable table : document.getTables()) replaceTableTokens(table, data);
            for (XWPFHeaderFooter headerFooter : document.getHeaderList()) replaceHeaderFooterTokens(headerFooter, data);
            for (XWPFHeaderFooter headerFooter : document.getFooterList()) replaceHeaderFooterTokens(headerFooter, data);
            document.write(output);
            return output.toByteArray();
        }
    }

    private void replaceHeaderFooterTokens(XWPFHeaderFooter headerFooter, Map<String, Object> data) {
        for (XWPFParagraph paragraph : headerFooter.getParagraphs()) replaceParagraphTokens(paragraph, data);
        for (XWPFTable table : headerFooter.getTables()) replaceTableTokens(table, data);
    }

    private void replaceTableTokens(XWPFTable table, Map<String, Object> data) {
        for (XWPFTableRow row : table.getRows()) {
            for (XWPFTableCell cell : row.getTableCells()) {
                for (XWPFParagraph paragraph : cell.getParagraphs()) replaceParagraphTokens(paragraph, data);
                for (XWPFTable nested : cell.getTables()) replaceTableTokens(nested, data);
            }
        }
    }

    private void replaceParagraphTokens(XWPFParagraph paragraph, Map<String, Object> data) {
        String text = paragraph.getText();
        if(DPUtil.empty(text)) return;
        Matcher matcher = TAG_PATTERN.matcher(text);
        while (matcher.find()) {
            String raw = null != matcher.group(1) ? matcher.group(1) : matcher.group(2);
            if(null == raw || DPUtil.empty(raw.trim())) continue;
            raw = raw.trim();
            if("?/#@*+".indexOf(raw.charAt(0)) >= 0) continue;
            ResolvedValue value = resolve(data, raw);
            if(!value.found) continue;
            String replacement = stringify(value.value);
            replaceAcrossRuns(paragraph, matcher.start(), matcher.end(), replacement);
            text = paragraph.getText();
            matcher = TAG_PATTERN.matcher(text);
        }
    }

    private void replaceAcrossRuns(XWPFParagraph paragraph, int start, int end, String replacement) {
        List<XWPFRun> runs = paragraph.getRuns();
        int offset = 0;
        XWPFRun startRun = null;
        XWPFRun endRun = null;
        int startLocal = 0;
        int endLocal = 0;
        for (XWPFRun run : runs) {
            String text = DPUtil.parseString(run.getText(0));
            int length = text.length();
            if(null == startRun && start >= offset && start <= offset + length) {
                startRun = run;
                startLocal = start - offset;
            }
            if(end >= offset && end <= offset + length) {
                endRun = run;
                endLocal = end - offset;
                break;
            }
            offset += length;
        }
        if(null == startRun || null == endRun) return;
        String startText = DPUtil.parseString(startRun.getText(0));
        if(startRun == endRun) {
            startRun.setText(startText.substring(0, startLocal) + replacement + startText.substring(endLocal), 0);
            return;
        }
        String endText = DPUtil.parseString(endRun.getText(0));
        startRun.setText(startText.substring(0, startLocal) + replacement, 0);
        endRun.setText(endText.substring(endLocal), 0);
        boolean clear = false;
        for (XWPFRun run : runs) {
            if(run == startRun) {
                clear = true;
                continue;
            }
            if(run == endRun) break;
            if(clear) run.setText("", 0);
        }
    }

    private ResolvedValue resolve(Map<String, Object> data, String path) {
        Object current = data;
        for (String segment : path.split("\\.")) {
            if(current instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) current;
                if(!map.containsKey(segment)) return ResolvedValue.miss();
                current = map.get(segment);
            } else if(current instanceof List) {
                int index;
                try {
                    index = Integer.parseInt(segment);
                } catch (NumberFormatException e) {
                    return ResolvedValue.miss();
                }
                List<?> list = (List<?>) current;
                if(index < 0 || index >= list.size()) return ResolvedValue.miss();
                current = list.get(index);
            } else {
                return ResolvedValue.miss();
            }
        }
        return ResolvedValue.hit(current);
    }

    private String stringify(Object value) {
        if(null == value) return "";
        if(value instanceof Collection) {
            StringJoiner joiner = new StringJoiner("、");
            for (Object item : (Collection<?>) value) joiner.add(stringify(item));
            return joiner.toString();
        }
        if(value instanceof Map) return DPUtil.stringify(value);
        return DPUtil.parseString(value);
    }

    private void collectTags(XWPFDocument document, Map<String, Map<String, Object>> result) {
        for (XWPFParagraph paragraph : document.getParagraphs()) collectTags(paragraph.getText(), result);
        for (XWPFTable table : document.getTables()) collectTags(table, result);
    }

    private void collectTags(XWPFHeaderFooter headerFooter, Map<String, Map<String, Object>> result) {
        for (XWPFParagraph paragraph : headerFooter.getParagraphs()) collectTags(paragraph.getText(), result);
        for (XWPFTable table : headerFooter.getTables()) collectTags(table, result);
    }

    private void collectTags(XWPFTable table, Map<String, Map<String, Object>> result) {
        for (XWPFTableRow row : table.getRows()) {
            for (XWPFTableCell cell : row.getTableCells()) {
                for (XWPFParagraph paragraph : cell.getParagraphs()) collectTags(paragraph.getText(), result);
                for (XWPFTable table1 : cell.getTables()) collectTags(table1, result);
            }
        }
    }

    private void collectTags(String text, Map<String, Map<String, Object>> result) {
        if(DPUtil.empty(text)) return;
        Matcher matcher = DISPLAY_TAG_PATTERN.matcher(text);
        while (matcher.find()) {
            String doubleRaw = matcher.group(1);
            String singleRaw = matcher.group(2);
            String squareRaw = matcher.group(3);
            String raw = (null != doubleRaw ? doubleRaw : null != singleRaw ? singleRaw : squareRaw).trim();
            if(DPUtil.empty(raw)) continue;
            String name = raw;
            String type = null != squareRaw ? "row" : "value";
            char prefix = raw.charAt(0);
            if(null == squareRaw && "?/#@*+".indexOf(prefix) >= 0) {
                name = raw.substring(1).trim();
                type = prefix == '?' || prefix == '#' ? "list" : prefix == '/' ? "list-end" : "special";
            } else if(raw.startsWith("=#this")) {
                name = "#this";
                type = "current";
            }
            String displayRaw = null != doubleRaw
                    ? "{{" + raw + "}}"
                    : null != singleRaw ? "{" + raw + "}" : "[" + raw + "]";
            Map<String, Object> existing = result.get(displayRaw);
            if(null != existing) {
                existing.put("count", ((Number) existing.getOrDefault("count", 1)).intValue() + 1);
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", name);
            item.put("raw", displayRaw);
            item.put("type", type);
            item.put("count", 1);
            result.put(displayRaw, item);
        }
    }

    private static class ResolvedValue {
        private final boolean found;
        private final Object value;

        private ResolvedValue(boolean found, Object value) {
            this.found = found;
            this.value = value;
        }

        private static ResolvedValue hit(Object value) {
            return new ResolvedValue(true, value);
        }

        private static ResolvedValue miss() {
            return new ResolvedValue(false, null);
        }
    }

}
