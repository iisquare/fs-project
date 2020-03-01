package com.iisquare.fs.web.admin.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.web.mvc.ServiceBase;
import com.iisquare.fs.base.web.util.ServiceUtil;
import com.iisquare.fs.web.admin.dao.AnalysisDao;
import com.iisquare.fs.web.admin.entity.Analysis;
import com.iisquare.fs.web.admin.rpc.CronRpc;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.*;

@Service
@SuppressWarnings("unchecked")
public class AnalysisService extends ServiceBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(AnalysisService.class);
    private static final String CRON_GROUP = "flink-olap";
    @Autowired
    private AnalysisDao analysisDao;
    @Autowired
    private CronRpc cronRpc;
    @Autowired
    private UserService userService;

    public Map<?, ?> search(Map<?, ?> param, Map<?, ?> config) {
        Map<String, Object> result = new LinkedHashMap<>();
        int page = ValidateUtil.filterInteger(param.get("page"), true, 1, null, 1);
        int pageSize = ValidateUtil.filterInteger(param.get("pageSize"), true, 1, 500, 15);
        Page<Analysis> data = analysisDao.findAll(new Specification() {
            @Override
            public Predicate toPredicate(Root root, CriteriaQuery query, CriteriaBuilder cb) {
                List<Predicate> predicates = new ArrayList<>();
                predicates.add(cb.notEqual(root.get("status"), -1));
                String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
                if(!DPUtil.empty(name)) {
                    predicates.add(cb.like(root.get("name"), "%" + name + "%"));
                }
                String type = DPUtil.trim(DPUtil.parseString(param.get("type")));
                if(!DPUtil.empty(type)) {
                    predicates.add(cb.equal(root.get("type"), type));
                }
                return cb.and(predicates.toArray(new Predicate[predicates.size()]));
            }
        }, PageRequest.of(page - 1, pageSize, Sort.by(new Sort.Order(Sort.Direction.DESC, "sort"))));
        List<Analysis> rows = data.getContent();
        if (!DPUtil.empty(config.get("withCronInfo"))) {
            fillCronInfo(rows);
        }
        if(!DPUtil.empty(config.get("withUserInfo"))) {
            userService.fillInfo(rows, "createdUid", "updatedUid");
        }
        if(!DPUtil.empty(config.get("withStatusText"))) {
            ServiceUtil.fillProperties(rows, new String[]{"status"}, new String[]{"statusText"}, status("full"));
        }
        if(!DPUtil.empty(config.get("withTypeText"))) {
            ServiceUtil.fillProperties(rows, new String[]{"type"}, new String[]{"typeText"}, types());
        }
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("total", data.getTotalElements());
        result.put("rows", rows);
        return result;
    }

    public Map<?, ?> types() {
        Map<String, String> types = new LinkedHashMap<>();
        types.put("view", "界面编辑");
        types.put("sql", "查询语句");
        return types;
    }

    public Map<?, ?> status(String level) {
        Map<Integer, String> status = new LinkedHashMap<>();
        status.put(1, "启用");
        status.put(2, "禁用");
        switch (level) {
            case "default":
                break;
            case "full":
                status.put(-1, "已删除");
                break;
            default:
                return null;
        }
        return status;
    }

    public Analysis info(Integer id, boolean withCronInfo) {
        if(null == id || id < 1) return null;
        Optional<Analysis> info = analysisDao.findById(id);
        if (info.isPresent()) {
            Analysis analysis = info.get();
            if (withCronInfo) {
                List<Analysis> list = new ArrayList<>();
                list.add(analysis);
                fillCronInfo(list);
            }
            return analysis;
        }
        return null;
    }

    public Analysis save(Analysis info, int uid, boolean isModifyCron) {
        long time = System.currentTimeMillis();
        info.setUpdatedTime(time);
        info.setUpdatedUid(uid);
        if (info.getId() == null) {
            info.setCreatedTime(time);
            info.setCreatedUid(uid);
        }
        Analysis saveInfo = analysisDao.save(info);
        if (isModifyCron) {
            Integer id = info.getId();
            if (id == null) {
                return null;
            }
            Map<String, Object> cronParamsMap = new LinkedHashMap<>();
            cronParamsMap.put("analysisId", id);
            cronParamsMap.put("yarn", new LinkedHashMap<>());
            JsonNode cronParams = DPUtil.parseJSON(info.getCronParams());
            if (cronParams != null) {
                Iterator<Map.Entry<String, JsonNode>> iterator = cronParams.fields();
                while (iterator.hasNext()) {
                    Map.Entry<String, JsonNode> item = iterator.next();
                    cronParamsMap.put(item.getKey(), item.getValue());
                }
            }
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("name", getCronName(id));
            params.put("group", CRON_GROUP);
            params.put("cron", info.getCronExpression());
            params.put("jobClass", "com.iisquare.fs.web.quartz.job.FlinkJob");
            params.put("params", cronParamsMap);
            params.put("desc", info.getDescription());
            params.put("uid", uid);
            cronRpc.modify(params);
        }
        return saveInfo;
    }

    public boolean delete(List<Integer> ids, int uid) {
        if(null == ids || ids.size() < 1) return false;
        List<Analysis> list = analysisDao.findAllById(ids);
        long time = System.currentTimeMillis();
        List<Map<String, Object>> keys = new ArrayList<>();
        for (Analysis item : list) {
            item.setStatus(-1);
            item.setUpdatedTime(time);
            item.setUpdatedUid(uid);
            Map<String, Object> key = new LinkedHashMap<>();
            key.put("name", getCronName(item.getId()));
            key.put("group", CRON_GROUP);
            keys.add(key);
        }
        analysisDao.saveAll(list);
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("keys", keys);
        params.put("uid", uid);
        cronRpc.delete(params);
        return true;
    }

//    public Map<String, Object> results(int analysisid, long stime, long etime, int page, int pageSize) {
//        return analysisResultHBase.getByAnalysisidAndTime(analysisid, stime, etime, page, pageSize);
//    }
//
//    public Map<String, Object> result(String resultid) {
//        return analysisResultHBase.getOneByResultid(resultid);
//    }
//
//    public SXSSFWorkbook exportResult(String resultid) {
//        return new ExcelCreater(analysisResultHBase.getOneByResultid(resultid)).createWorkbook();
//    }
//
//    public boolean deleteResult(String resultid) {
//        return analysisResultHBase.delete(resultid);
//    }

    private String getCronName(int id) {
        return "analysis" + String.valueOf(id);
    }

    private void fillCronInfo(List<Analysis> list) {
        List<Map<String, Object>> keys = new ArrayList<>();
        for (Analysis analysis : list) {
            Map<String, Object> key = new LinkedHashMap<>();
            key.put("name", getCronName(analysis.getId()));
            key.put("group", CRON_GROUP);
            keys.add(key);
        }
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("keys", keys);
        JsonNode cronResult = DPUtil.parseJSON(cronRpc.map(params));
        JsonNode cronMap = cronResult != null ? cronResult.findPath("data") : DPUtil.objectNode();
        for (Analysis analysis : list) {
            String cronName = getCronName(analysis.getId());
            String cronExpression, cronParams;
            String key = CRON_GROUP + "." + cronName;
            if (cronMap.has(key)) {
                JsonNode cronInfo = cronMap.get(key);
                cronExpression = cronInfo.findPath("cron").asText();
                cronParams = cronInfo.findPath("params").asText();
            } else {
                cronName = getCronName(-1);
                cronExpression = cronParams = "";
            }
            analysis.setCronName(cronName);
            analysis.setCronGroup(CRON_GROUP);
            analysis.setCronExpression(cronExpression);
            analysis.setCronParams(cronParams);
        }
    }

    private class ExcelCreater {
        private int rowIndex;
        private JsonNode content, grades;
        private Map<String, String> columns;

        private ExcelCreater(Map<String, Object> result) {
            content = DPUtil.convertJSON(result.get("content"));
            grades = DPUtil.convertJSON(result.get("grades"));
            columns = new LinkedHashMap<>();
            Iterator<JsonNode> iterator = DPUtil.convertJSON(result.get("columns")).elements();
            while (iterator.hasNext()) {
                JsonNode column = iterator.next();
                columns.put(column.get("alias").asText(), column.get("label").asText());
            }
        }

        private SXSSFWorkbook createWorkbook() {
            SXSSFWorkbook workbook = new SXSSFWorkbook(100000);
            if (grades != null && content != null) {
                try {
                    setWorkbook(workbook, grades, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), 0);
                } catch (Exception e) {
                    LOGGER.error(e.getMessage(), e);
                }
            }
            return workbook;
        }

        private void setWorkbook(SXSSFWorkbook workbook, JsonNode grades, List<String> parentFields, List<String> fields, List<Integer> indexes, int level) {
            int index = 0;
            for (JsonNode grade : grades) {
                Set<String> parentSet = new HashSet<>(parentFields);
                int fieldsCount = 0;
                Iterator<JsonNode> iterator = grade.get("primaries").elements();
                while (iterator.hasNext()) {
                    String field = iterator.next().asText();
                    if (!parentSet.contains(field)) {
                        fields.add(field);
                        fieldsCount++;
                    }
                }
                iterator = grade.get("columns").elements();
                while (iterator.hasNext()) {
                    String field = iterator.next().asText();
                    if (!parentSet.contains(field)) {
                        fields.add(field);
                        fieldsCount++;
                    }
                }
                indexes.add(index++);
                JsonNode children = grade.get("children");
                if (children.size() == 0) {
                    rowIndex = 0;
                    Sheet sheet = workbook.createSheet();
                    Row row = sheet.createRow(rowIndex++);
                    int columIndex = 0;
                    for (String field : fields) {
                        row.createCell(columIndex++).setCellValue(this.columns.getOrDefault(field, field));
                    }
                    setSheet(sheet, content, indexes, 0, 0);
                } else {
                    int parentFieldsCount = 0;
                    iterator = grade.get("primaries").elements();
                    while (iterator.hasNext()) {
                        parentFields.add(iterator.next().asText());
                        parentFieldsCount++;
                    }
                    setWorkbook(workbook, children, parentFields, fields, indexes, ++level);
                    level--;
                    while (parentFieldsCount-- > 0) {
                        parentFields.remove(parentFields.size() - 1);
                    }
                }
                while (fieldsCount-- > 0) {
                    fields.remove(fields.size() - 1);
                }
                indexes.remove(level);
            }
        }

        private ObjectNode setSheet(Sheet sheet, JsonNode content, List<Integer> indexes, int level, int columIndex) {
            int firstRow = -1, lastRow = -1;
            for (JsonNode item : content.get(indexes.get(level))) {
                JsonNode children = item.get("children");
                Iterator<Map.Entry<String, JsonNode>> fields = item.fields();
                if (children.size() == 0) {
                    setRow(sheet.createRow(rowIndex), fields, columIndex);
                    if (firstRow == -1) {
                        firstRow = rowIndex;
                    }
                    lastRow = rowIndex++;
                } else {
                    int fieldsCount = 0;
                    Iterator<String> fieldNames = item.fieldNames();
                    while (fieldNames.hasNext()) {
                        if (!fieldNames.next().equals("children")) {
                            fieldsCount++;
                        }
                    }
                    columIndex += fieldsCount;
                    ObjectNode itemRow = setSheet(sheet, children, indexes, ++level, columIndex);
                    level--;
                    columIndex -= fieldsCount;
                    int itemFirstRow = itemRow.get("first").asInt();
                    int itemLastRow = itemRow.get("last").asInt();
                    if (itemFirstRow == -1) {
                        itemFirstRow = rowIndex;
                        itemLastRow = rowIndex++;
                    }
                    Row row = sheet.getRow(itemFirstRow);
                    if (row == null) {
                        row = sheet.createRow(itemFirstRow);
                    }
                    setRow(row, fields, columIndex);
                    if (itemFirstRow < itemLastRow) {
                        fieldNames = item.fieldNames();
                        int itemColumIndex = columIndex;
                        while (fieldNames.hasNext()) {
                            if (!fieldNames.next().equals("children")) {
                                sheet.addMergedRegion(new CellRangeAddress(itemFirstRow, itemLastRow, itemColumIndex, itemColumIndex++));
                            }
                        }
                    }
                    if (firstRow == -1) {
                        firstRow = itemFirstRow;
                    }
                    lastRow = itemLastRow;
                }
            }
            ObjectNode row = DPUtil.objectNode();
            row.put("first", firstRow);
            row.put("last", lastRow);
            return row;
        }

        private void setRow(Row row, Iterator<Map.Entry<String, JsonNode>> fields, int columIndex) {
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                if (field.getKey().equals("children")) {
                    continue;
                }
                Cell cell = row.createCell(columIndex++);
                JsonNode value = field.getValue();
                if (value.isNumber()) {
                    cell.setCellValue(value.asDouble());
                } else {
                    cell.setCellValue(value.asText());
                }
            }
        }
    }
}
