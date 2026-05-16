package com.iisquare.fs.web.oa.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.HttpUtil;
import com.iisquare.fs.base.jpa.util.JPAUtil;
import com.iisquare.fs.base.web.mvc.ServiceBase;
import com.iisquare.fs.web.core.rpc.MemberRpc;
import com.iisquare.fs.web.oa.dao.FormFrameDao;
import com.iisquare.fs.web.oa.entity.FormFrame;
import com.iisquare.fs.web.oa.storage.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

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
    @Autowired
    private FormRestStorage restStorage;
    @Autowired
    private FormEmptyStorage emptyStorage;
    @Autowired
    private FormDefaultStorage defaultStorage;

    public FormStorage storage(ObjectNode frame) {
        JsonNode storage = frame.at("/storage");
        switch (storage.at("/type").asText("")) {
            case "jdbc": return jdbcStorage;
            case "mongo": return mongoStorage;
            case "rest": return restStorage;
            case "empty": return emptyStorage;
            default: return defaultStorage;
        }
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

    public ObjectNode info(ObjectNode frame, String id) {
        return storage(frame).info(frame, id);
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
        if (maxLength > 0 && DPUtil.empty(maxTooltip)) maxTooltip = "最多选择" + maxLength + "项";
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
            if (minLength > 0 && value.size() < minLength) {
                return ApiUtil.result(80502, message(node, minTooltip), node);
            }
            if (maxLength > 0 && value.size() > maxLength) {
                return ApiUtil.result(80503, message(node, maxTooltip), node);
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

    public Map<String, Object> validateSubform(JsonNode node, ArrayNode value) {
        JsonNode options = node.at("/options");
        if (!options.at("/ruleEnabled").asBoolean(false)) {
            return ApiUtil.result(0, null, value);
        }
        int minLength = options.at("/minLength").asInt(0);
        if (minLength > 0 && value.size() < minLength) {
            String tooltip = options.at("/minTooltip").asText();
            if (DPUtil.empty(tooltip)) tooltip = "至少填写" + minLength + "条";
            return ApiUtil.result(80602, message(node, tooltip), node);
        }
        int maxLength = options.at("/maxLength").asInt(0);
        if (maxLength > 0 && value.size() > maxLength) {
            String tooltip = options.at("/maxTooltip").asText();
            if (DPUtil.empty(tooltip)) tooltip = "最多填写" + maxLength + "条";
            return ApiUtil.result(80603, message(node, tooltip), node);
        }
        return ApiUtil.result(0, null, value);
    }

    public boolean authority(JsonNode authority, JsonNode widget, String permission) {
        if (null == authority) return true; // 未启用权限校验
        if (!authority.isObject()) return false; // 权限配置异常
        if (null == widget || !widget.isObject()) return false; // 组件配置异常
        return authority.at("/" + widget.at("/id").asText("") + "/" + permission).asBoolean(false);
    }

    /**
     * 校验异常时，返回值data为node节点信息
     * @param widgets 表单组件
     * @param form 表单数据
     * @param authority 表单权限
     * @param origin 原数据，引用修改
     * @param regulars 校验规则
     * @return 校验结果
     */
    public Map<String, Object> validate(JsonNode widgets, JsonNode form, JsonNode authority, ObjectNode origin, JsonNode regulars) {
        if (null == widgets || !widgets.isArray()) {
            return ApiUtil.result(7001, "表单配置异常", widgets);
        }
        if (null == form || !form.isObject()) {
            return ApiUtil.result(8001, "表单数据异常", form);
        }
        if (null == origin || !origin.isObject()) origin = DPUtil.objectNode();
        if (null == regulars) regulars = formRegularService.all();
        if (!origin.has(FormStorage.FIELD_ID) && form.has(FormStorage.FIELD_ID)) { // 保留主键
            origin.replace(FormStorage.FIELD_ID, form.get(FormStorage.FIELD_ID));
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
                    Map<String, Object> result = validate(it.next().at("/widgets"), form, authority, origin, regulars);
                    if (0 != (int) result.get("code")) return result;
                }
                continue;
            }
            String field = DPUtil.trim(options.at("/field").asText());
            if (DPUtil.empty(field)) continue; // 忽略无字段数据
            if (!authority(authority, node, "editable")) continue; // 忽略无权限字段
            if ("subform".equals(type)) { // 子表单
                JsonNode formInfo = options.at("/formInfo");
                if (null == formInfo || !formInfo.isObject()) {
                    return ApiUtil.result(7002, "子表单配置异常", node);
                }
                JsonNode subforms = form.at("/" + field);
                if (!subforms.isArray()) {
                    return ApiUtil.result(8002, "子表单数据异常", node);
                }
                ArrayNode array = (origin.has(field) && origin.get(field).isArray()) ? (ArrayNode) origin.get(field) : origin.putArray(field);
                Map<String, Integer> map = DPUtil.indexes(array, FormStorage.FIELD_ID);
                Iterator<JsonNode> it = subforms.iterator();
                while (it.hasNext()) {
                    JsonNode subform = it.next();
                    String id = subform.at("/" + FormStorage.FIELD_ID).asText();
                    if (DPUtil.empty(id)) return ApiUtil.result(8003, "子表单数据标识异常", node);
                    Map<String, Object> result = null;
                    if (map.containsKey(id)) { // 原纪录已存在
                        if (authority(authority, node, "changeable")) { // 允许修改记录
                            result = validate(formInfo.at("/widgets"), subform, authority, (ObjectNode) array.get(map.get(id)), regulars);
                        }
                        map.remove(id);
                    } else { // 新记录
                        if (authority(authority, node, "addable")) {
                            result = validate(formInfo.at("/widgets"), subform, authority, array.addObject(), regulars);
                        }
                    }
                    if (null != result && 0 != (int) result.get("code")) return result;
                }
                if (map.size() > 0 && authority(authority, node, "removable")) {
                    DPUtil.remove(array, new ArrayList<>(map.values())); // 移除表单中不存在的原数据
                }
                Map<String, Object> result = validateSubform(node, array);
                if (0 != (int) result.get("code")) return result;
                continue;
            }
            if (Arrays.asList("text", "textarea", "password").contains(type)) {
                Map<String, Object> result = validateString(node, form.at("/" + field).asText(), regulars);
                if (0 != (int) result.get("code")) return result;
                origin.put(field, (String) result.get("data"));
                continue;
            }
            if ("number".equals(type)) {
                Map<String, Object> result = validateNumber(node, form.at("/" + field).asDouble(), regulars);
                if (0 != (int) result.get("code")) return result;
                origin.put(field, (Double) result.get("data"));
                continue;
            }
            if ("radio".equals(type)) {
                Map<String, Object> result = validateRadio(node, form.at("/" + field).asText(), regulars);
                if (0 != (int) result.get("code")) return result;
                origin.put(field, (String) result.get("data"));
                continue;
            }
            if ("checkbox".equals(type)) {
                Map<String, Object> result = validateCheckbox(node, form.at("/" + field), regulars);
                if (0 != (int) result.get("code")) return result;
                origin.replace(field, (JsonNode) result.get("data"));
                continue;
            }
            if ("select".equals(type)) {
                Map<String, Object> result = validateSelector(node, form.at("/" + field), regulars);
                if (0 != (int) result.get("code")) return result;
                origin.replace(field, (JsonNode) result.get("data"));
                continue;
            }
            if ("switch".equals(type)) {
                origin.put(field, form.at("/" + field).asBoolean(false));
                continue;
            }
            origin.replace(field, form.get(field));
        }
        return ApiUtil.result(0, null, origin);
    }

    /**
     * 根据权限过滤表单内容
     * @param widgets 表单组件
     * @param form 表单数据
     * @param authority 权限配置
     * @param permission 权限字段
     * @param keepId 保留ID字段
     * @return 过滤结果
     */
    public ObjectNode filtration(JsonNode widgets, JsonNode form, JsonNode authority, String permission, boolean keepId) {
        ObjectNode data = DPUtil.objectNode();
        if (null == widgets || !widgets.isArray()) return data;
        if (null == form || !form.isObject()) return data;
        if (keepId && form.has(FormStorage.FIELD_ID)) { // 保留主键
            data.replace(FormStorage.FIELD_ID, form.get(FormStorage.FIELD_ID));
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
                    data.setAll(filtration(it.next().at("/widgets"), form, authority, permission, keepId));
                }
                continue;
            }
            String field = DPUtil.trim(options.at("/field").asText());
            if (DPUtil.empty(field)) continue; // 忽略无字段数据
            if (!authority(authority, node, permission)) continue; // 忽略无权限字段
            if ("subform".equals(type)) { // 子表单
                JsonNode formInfo = options.at("/formInfo");
                if (null == formInfo || !formInfo.isObject()) continue;
                JsonNode subforms = form.at("/" + field);
                if (!subforms.isArray()) continue;
                ArrayNode array = data.putArray(field);
                Iterator<JsonNode> it = subforms.iterator();
                while (it.hasNext()) {
                    JsonNode subform = it.next();
                    String id = subform.at("/" + FormStorage.FIELD_ID).asText();
                    if (DPUtil.empty(id)) continue; // 忽略无标识数据
                    array.add(filtration(formInfo.at("/widgets"), subform, authority, permission, keepId));
                }
                continue;
            }
            if (form.has(field)) data.replace(field, form.get(field));
        }
        return data;
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
            JsonNode storage = DPUtil.parseJSON(frame.getStorage());
            if (null == storage || !storage.isObject()) storage = DPUtil.objectNode();
            node.replace("storage", storage);
        }
        JsonNode content = DPUtil.parseJSON(frame.getContent());
        if (null == content || !content.isObject()) content = DPUtil.objectNode();
        node.setAll((ObjectNode) content);
        fillWidget(filledFrames, node.at("/widgets"), withRemote);
        if (null == filledFrames) filledFrames.replace(sid, node);
        return node;
    }

}
