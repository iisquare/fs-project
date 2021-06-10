package com.iisquare.fs.web.oa.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.HttpUtil;
import com.iisquare.fs.base.jpa.util.JPAUtil;
import com.iisquare.fs.base.mongodb.MongoCore;
import com.iisquare.fs.base.web.mvc.ServiceBase;
import com.iisquare.fs.web.core.rpc.MemberRpc;
import com.iisquare.fs.web.oa.dao.FormFrameDao;
import com.iisquare.fs.web.oa.entity.FormFrame;
import com.iisquare.fs.web.oa.storage.FormJDBCStorage;
import com.iisquare.fs.web.oa.storage.FormMongoStorage;
import com.iisquare.fs.web.oa.storage.FormStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Service
public class FormService extends ServiceBase {

    @Autowired
    private FormFrameDao formFrameDao;
    @Autowired
    private MemberRpc memberRpc;
    @Autowired
    private FormRegularService formRegularService;
    @Autowired
    private FormJDBCStorage jdbcStorage;
    @Autowired
    private FormMongoStorage mongoStorage;

    public FormStorage storage(ObjectNode frame) {
        String physicalTable = frame.get("physicalTable").asText();
        return DPUtil.empty(physicalTable) ? mongoStorage : jdbcStorage;
    }

    public Map<String, Object> search(ObjectNode frame, Map<String, Object> param, Map<String, Object> config) {
        return storage(frame).search(frame, param, config);
    }

    public ObjectNode save(ObjectNode frame, ObjectNode info, int uid) {
        return storage(frame).save(frame, info, uid);
    }

    public long delete(ObjectNode frame, List<String> ids, int uid) {
        return storage(frame).delete(frame, ids, uid);
    }

    public ObjectNode fields(JsonNode widgets, boolean reduceSubform) {
        if (null == widgets || !widgets.isArray()) return null;
        ObjectNode nodes = DPUtil.objectNode();
        Iterator<JsonNode> iterator = widgets.iterator();
        while (iterator.hasNext()) {
            JsonNode node = iterator.next();
            ObjectNode options = (ObjectNode) node.at("/options");
            String field = DPUtil.trim(options.at("/field").asText());
            if (DPUtil.empty(field)) continue;
            String type = node.at("/type").asText();
            if (Arrays.asList("txt", "html", "divider").contains(type)) continue;
            if ("grid".equals(type)) {
                JsonNode items = node.at("/items");
                if (null == items || !items.isArray()) continue;
                Iterator<JsonNode> it = items.iterator();
                while (it.hasNext()) {
                    ObjectNode fields = fields(it.next().at("/widgets"), reduceSubform);
                    if (null == fields) continue;
                    nodes.setAll(fields);
                }
                continue;
            }
            if ("subform".equals(type)) {
                if (!reduceSubform) continue;
                JsonNode subform = options.at("/formInfo");
                if (null == subform || !subform.isObject()) {
                    options.replace("formFields", null);
                } else {
                    options.replace("formFields", fields(subform.at("/widgets"), reduceSubform));
                }
                nodes.replace(field, node);
                continue;
            }
            nodes.replace(field, node);
        }
        return nodes;
    }

    public String message(JsonNode node, String tooltip) {
        String field = DPUtil.trim(node.at("/options/field").asText());
        String label = DPUtil.trim(node.at("/label").asText());
        String message = "";
        if (!DPUtil.empty(label)) {
            message += "字段{" + label + "}";
        }
        if (!DPUtil.empty(field)) {
            message += "[" + field + "]";
        }
        if (!DPUtil.empty(message)) message += ",";
        return message + tooltip;
    }
    
    public Map<String, Object> validateRegular(JsonNode node, String value, JsonNode regulars) {
        Iterator<JsonNode> iterator = node.at("/options/regulars").iterator();
        while (iterator.hasNext()) {
            JsonNode item = iterator.next();
            String regular = item.at("/regular").asText("");
            String tooltip = item.at("/tooltip").asText("");
            if (regular.startsWith("regex:")) {
                if (regulars.has(regular)) {
                    regular = regulars.get(regular).at("/regex").asText();
                    if (DPUtil.empty(tooltip)) {
                        tooltip = regulars.get(regular).at("/tooltip").asText();
                    }
                } else {
                    return ApiUtil.result(80001, message(node, "正则校验已禁用"), node);
                }
            }
            if (DPUtil.empty(tooltip)) tooltip = "数据格式不正确";
            if (!DPUtil.isMatcher(regular, value)) {
                return ApiUtil.result(80002, message(node, tooltip), node);
            }
        }
        return ApiUtil.result(0, null, value);
    }

    public Map<String, Object> validateString(JsonNode node, String value, JsonNode regulars) {
        JsonNode options = node.at("/options");
        if (!options.at("/ruleEnabled").asBoolean(false)) {
            return ApiUtil.result(0, null, value);
        }
        if (options.at("/required").asBoolean(false) && DPUtil.empty(value)) {
            String tooltip = options.at("/requiredTooltip").asText();
            if (DPUtil.empty(tooltip)) tooltip = "字段值不能为空";
            return ApiUtil.result(80101, message(node, tooltip), node);
        }
        int minLength = options.at("/minLength").asInt(0);
        if (minLength > 0 && (null == value || value.length() < minLength)) {
            String tooltip = options.at("/minTooltip").asText();
            if (DPUtil.empty(tooltip)) tooltip = "字段长度不能少于" + minLength;
            return ApiUtil.result(80102, message(node, tooltip), node);
        }
        int maxLength = options.at("/maxLength").asInt(0);
        if (maxLength > 0 && (null == value || value.length() > maxLength)) {
            String tooltip = options.at("/maxTooltip").asText();
            if (DPUtil.empty(tooltip)) tooltip = "字段长度不能多于" + maxLength;
            return ApiUtil.result(80103, message(node, tooltip), node);
        }
        return validateRegular(node, value, regulars);
    }

    public Map<String, Object> validateNumber(JsonNode node, Double value, JsonNode regulars) {
        JsonNode options = node.at("/options");
        if (!options.at("/ruleEnabled").asBoolean(false)) {
            return ApiUtil.result(0, null, value);
        }
        if (options.at("/minEnabled").asBoolean(false)) {
            double min = options.at("/min").asDouble(0.0);
            if (null == value || value < min) {
                String tooltip = options.at("/minTooltip").asText();
                if (DPUtil.empty(tooltip)) tooltip = "字段值不能小于" + min;
                return ApiUtil.result(80202, message(node, tooltip), node);
            }
        }
        if (options.at("/maxEnabled").asBoolean(false)) {
            double max = options.at("/max").asDouble(0.0);
            if (null == value || value > max) {
                String tooltip = options.at("/maxTooltip").asText();
                if (DPUtil.empty(tooltip)) tooltip = "字段值不能大于" + max;
                return ApiUtil.result(80203, message(node, tooltip), node);
            }
        }
        Map<String, Object> result = validateRegular(node, String.valueOf(value), regulars);
        if (0 != (int) result.get("code")) return result;
        return ApiUtil.result(0, null, value);
    }

    public JsonNode selectorItemMap(JsonNode items) {
        ObjectNode result = DPUtil.objectNode();
        Iterator<JsonNode> iterator = items.iterator();
        while (iterator.hasNext()) {
            JsonNode item = iterator.next();
            result.replace(item.at("/value").asText(), item);
        }
        return result;
    }

    public Map<String, Object> validateRadio(JsonNode node, String value, JsonNode regulars) {
        JsonNode options = node.at("/options");
        if (!options.at("/ruleEnabled").asBoolean(false)) {
            return ApiUtil.result(0, null, value);
        }
        if (!options.at("/required").asBoolean(false) && DPUtil.empty(value)) {
            return ApiUtil.result(0, null, value);
        }
        JsonNode items = selectorItemMap(options.at("/items"));
        if (!items.has(value)) {
            String tooltip = options.at("/requiredTooltip").asText();
            if (DPUtil.empty(tooltip)) tooltip = "选项不能为空";
            return ApiUtil.result(80301, message(node, tooltip), node);
        }
        return ApiUtil.result(0 ,null, value);
    }

    public Map<String, Object> validateCheckbox(JsonNode node, JsonNode value, JsonNode regulars) {
        JsonNode options = node.at("/options");
        if (!options.at("/ruleEnabled").asBoolean(false)) {
            return ApiUtil.result(0, null, value);
        }
        if (null == value || !value.isArray()) {
            return ApiUtil.result(80401, message(node, "选项值必须为数组"), node);
        }
        JsonNode items = selectorItemMap(options.at("/items"));
        Iterator<JsonNode> iterator = value.iterator();
        while (iterator.hasNext()) {
            if (items.has(iterator.next().asText())) continue;
            return ApiUtil.result(80400, message(node, "选项值不合法"), node);
        }
        int minLength = options.at("/minLength").asInt(0);
        if (minLength > 0 && value.size() < minLength) {
            String tooltip = options.at("/minTooltip").asText();
            if (DPUtil.empty(tooltip)) tooltip = "至少选择" + minLength + "项";
            return ApiUtil.result(80402, message(node, tooltip), node);
        }
        int maxLength = options.at("/maxLength").asInt(0);
        if (maxLength > 0 && value.size() > maxLength) {
            String tooltip = options.at("/maxTooltip").asText();
            if (DPUtil.empty(tooltip)) tooltip = "最多选择" + maxLength + "项";
            return ApiUtil.result(80403, message(node, tooltip), node);
        }
        return ApiUtil.result(0 ,null, value);
    }

    public Map<String, Object> validateSelector(JsonNode node, JsonNode value, JsonNode regulars) {
        JsonNode options = node.at("/options");
        if (!options.at("/ruleEnabled").asBoolean(false)) {
            return ApiUtil.result(0, null, value);
        }
        String mode = options.at("/mode").asText("");
        JsonNode items = selectorItemMap(options.at("/items"));
        int minLength = options.at("/minLength").asInt(0);
        int maxLength = options.at("/maxLength").asInt(0);
        String minTooltip = options.at("/minTooltip").asText();
        if (minLength > 0 && DPUtil.empty(minTooltip)) minTooltip = "至少选择" + minLength + "项";
        String maxTooltip = options.at("/minTooltip").asText();
        if (maxLength > 0 && DPUtil.empty(maxTooltip)) maxTooltip = "最多选择" + minLength + "项";
        if ("default".equals(mode)) {
            String valueString = value.asText();
            if (minLength > 0 && (DPUtil.empty(valueString) || !items.has(valueString))) {
                return ApiUtil.result(80501, minTooltip, node);
            } else {
                return ApiUtil.result(0, null, value);
            }
        }
        if ("multiple".equals(mode)) {
            return validateCheckbox(node, value, regulars);
        }
        if ("tags".equals(mode)) {
            Iterator<JsonNode> iterator = value.iterator();
            while (iterator.hasNext()) {
                Map<String, Object> result = validateRegular(node, iterator.next().asText(), regulars);
                if (0 != (int) result.get("code")) return result;
            }
            return ApiUtil.result(0, null, value);
        }
        if ("combobox".equals(mode)) {
            Map<String, Object> result = validateRegular(node, value.asText(), regulars);
            if (0 != (int) result.get("code")) return result;
            return ApiUtil.result(0, null, value);
        }
        return ApiUtil.result(0, null, value);
    }

    /**
     * 校验异常时，data字段为node节点信息
     */
    public Map<String, Object> validate(JsonNode widgets, JsonNode form, JsonNode regulars) {
        if (null == widgets || !widgets.isArray()) {
            return ApiUtil.result(7001, "表单配置异常", widgets);
        }
        if (null == form || !form.isObject()) {
            return ApiUtil.result(8001, "表单数据异常", form);
        }
        if (null == regulars) regulars = formRegularService.all();
        ObjectNode data = DPUtil.objectNode(); // 数据结果
        if (form.has(MongoCore.FIELD_ID)) { // 保留主键
            data.replace(MongoCore.FIELD_ID, form.get(MongoCore.FIELD_ID));
        }
        Iterator<JsonNode> iterator = widgets.iterator();
        while (iterator.hasNext()) {
            JsonNode node = iterator.next();
            ObjectNode options = (ObjectNode) node.at("/options");
            String type = node.at("/type").asText();
            if (Arrays.asList("txt", "html", "divider").contains(type)) continue; // 忽略非数据字段
            if ("grid".equals(type)) { // 栅格布局
                JsonNode items = node.at("/options/items");
                if (null == items || !items.isArray()) continue;
                Iterator<JsonNode> it = items.iterator();
                while (it.hasNext()) {
                    Map<String, Object> result = validate(it.next().at("/widgets"), form, regulars);
                    if (0 != (int) result.get("code")) return result;
                    data.setAll((ObjectNode) result.get("data"));
                }
                continue;
            }
            String field = DPUtil.trim(options.at("/field").asText());
            if (DPUtil.empty(field)) continue; // 忽略无字段数据
            if ("subform".equals(type)) { // 子表单
                JsonNode formInfo = options.at("/formInfo");
                if (null == formInfo || !formInfo.isObject()) {
                    return ApiUtil.result(7002, "子表单配置异常", node);
                }
                JsonNode subforms = form.get(field);
                if (!subforms.isArray()) {
                    return ApiUtil.result(8002, "子表单数据异常", node);
                }
                ArrayNode array = data.putArray(field);
                Iterator<JsonNode> it = subforms.iterator();
                while (it.hasNext()) {
                    Map<String, Object> result = validate(formInfo.at("/widgets"), it.next(), regulars);
                    if (0 != (int) result.get("code")) return result;
                    array.add((ObjectNode) result.get("data"));
                }
                continue;
            }
            if (Arrays.asList("text", "textarea", "password").contains(type)) {
                Map<String, Object> result = validateString(node, form.at("/" + field).asText(), regulars);
                if (0 != (int) result.get("code")) return result;
                data.put(field, (String) result.get("data"));
                continue;
            }
            if ("number".equals(type)) {
                Map<String, Object> result = validateNumber(node, form.at("/" + field).asDouble(), regulars);
                if (0 != (int) result.get("code")) return result;
                data.put(field, (Double) result.get("data"));
                continue;
            }
            if ("radio".equals(type)) {
                Map<String, Object> result = validateRadio(node, form.at("/" + field).asText(), regulars);
                if (0 != (int) result.get("code")) return result;
                data.put(field, (String) result.get("data"));
                continue;
            }
            if ("checkbox".equals(type)) {
                Map<String, Object> result = validateCheckbox(node, form.at("/" + field), regulars);
                if (0 != (int) result.get("code")) return result;
                data.replace(field, (JsonNode) result.get("data"));
                continue;
            }
            if ("select".equals(type)) {
                Map<String, Object> result = validateSelector(node, form.at("/" + field), regulars);
                if (0 != (int) result.get("code")) return result;
                data.replace(field, (JsonNode) result.get("data"));
                continue;
            }
            if ("switch".equals(type)) {
                data.put(field, form.at("/" + field).asBoolean(false));
                continue;
            }
            data.replace(field, form.get(field));
        }
        return ApiUtil.result(0, null, data);
    }

    public JsonNode findRemoteResult (JsonNode options, JsonNode result) {
        ArrayNode nodes = DPUtil.arrayNode();
        if (null == result) return nodes;
        String fieldData = options.at("/fieldData").asText();
        if (!DPUtil.empty(fieldData)) {
            result = result.at("/" + fieldData.replaceAll("\\.", "/"));
        }
        if (result.isNull()) return nodes;
        String fieldValue = "/" + options.at("/fieldValue").asText("value");
        String fieldLabel = "/" + options.at("/fieldLabel").asText("label");
        Iterator<JsonNode> iterator = result.iterator();
        while (iterator.hasNext()) {
            JsonNode item = iterator.next();
            ObjectNode node = nodes.addObject();
            node.put("value", item.at(fieldValue).asText());
            node.put("label", item.at(fieldLabel).asText());
        }
        return nodes;
    }

    public boolean fillRemote(ObjectNode options) {
        if (null == options || !options.isObject()) return false;
        switch (options.at("/type").asText()) {
            case "dictionary":
                String path = options.at("/dictionary").asText();
                JsonNode dict = DPUtil.parseJSON(
                    memberRpc.post("/dictionary/available", DPUtil.buildMap("path", path, "formatArray", true))
                );
                int code = dict.at("/code").asInt();
                options.replace("items", 0 == code ? dict.at("/data") : DPUtil.arrayNode());
                break;
            case "api":
                String url = options.at("/apiUrl").asText("");
                if (!url.startsWith("http")) {
                    options.replace("items", DPUtil.arrayNode());
                    break;
                }
                JsonNode result = DPUtil.parseJSON(HttpUtil.get(url));
                options.replace("items", findRemoteResult(options, result));
                break;
            case "static":
                break;
            default:
                options.replace("items", DPUtil.arrayNode());
        }
        return true;
    }

    public boolean fillWidget (ObjectNode filledFrames, JsonNode widgets, boolean withRemote) {
        if (null == widgets || !widgets.isArray()) return false;
        Iterator<JsonNode> iterator = widgets.iterator();
        while (iterator.hasNext()) {
            JsonNode node = iterator.next();
            ObjectNode options = (ObjectNode) node.at("/options");
            String field = DPUtil.trim(options.at("/field").asText());
            if (DPUtil.empty(field)) continue;
            String type = node.at("/type").asText();
            if ("grid".equals(type)) {
                JsonNode items = node.at("/items");
                if (null == items || !items.isArray()) continue;
                Iterator<JsonNode> it = items.iterator();
                while (it.hasNext()) fillWidget(filledFrames, it.next().at("/widgets"), withRemote);
                continue;
            }
            if (withRemote && Arrays.asList("radio", "checkbox", "select").contains(type)) {
                fillRemote((ObjectNode) node.at("/options"));
                continue;
            }
            if (null != filledFrames && "subform".equals(type)) {
                String formId = options.at("/formId").asText();
                options.replace("formInfo", frame(DPUtil.parseInt(formId), filledFrames, false, withRemote));
                continue;
            }
        }
        return true;
    }

    /**
     * 获取表单配置信息
     * @param id 表单标识
     * @param filledFrames 临时缓存，若为null则不填充子表单
     * @param withDetail 是否填充详情字段
     * @param withRemote 是否填充远程接口数据
     * @return 表单信息
     */
    public ObjectNode frame(Integer id, ObjectNode filledFrames, boolean withDetail, boolean withRemote) {
        if(null == id || id < 1) return null;
        String sid = String.valueOf(id);
        if (null != filledFrames && filledFrames.has(sid)) return (ObjectNode) filledFrames.get(sid);
        FormFrame frame = JPAUtil.findById(formFrameDao, id, FormFrame.class);
        if (null == frame || 1 != frame.getStatus()) {
            if (null != filledFrames) filledFrames.replace(sid, null);
            return null;
        }
        ObjectNode node = DPUtil.objectNode();
        node.put("id", frame.getId()).put("name", frame.getName());
        if (withDetail) {
            node.put("bpmId", frame.getBpmId());
            node.put("physicalTable", frame.getPhysicalTable());
        }
        JsonNode content = DPUtil.parseJSON(frame.getContent());
        if (null == content || !content.isObject()) content = DPUtil.objectNode();
        node.setAll((ObjectNode) content);
        fillWidget(filledFrames, node.at("/widgets"), withRemote);
        if (null == filledFrames) filledFrames.replace(sid, node);
        return node;
    }

}
