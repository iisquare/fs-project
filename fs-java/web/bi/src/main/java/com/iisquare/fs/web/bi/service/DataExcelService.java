package com.iisquare.fs.web.bi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.jpa.helper.SpecificationHelper;
import com.iisquare.fs.base.jpa.mvc.JPAServiceBase;
import com.iisquare.fs.base.mongodb.MongoCore;
import com.iisquare.fs.base.mongodb.util.FindUtil;
import com.iisquare.fs.web.bi.dao.DataExcelDao;
import com.iisquare.fs.web.bi.entity.DataExcel;
import com.iisquare.fs.web.bi.mongodb.ExcelMongo;
import com.iisquare.fs.web.bi.mvc.Configuration;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.mongodb.client.model.*;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DataExcelService extends JPAServiceBase {

    @Autowired
    DataExcelDao dataExcelDao;
    @Autowired
    DefaultRbacService rbacService;
    @Autowired
    Configuration configuration;
    @Autowired
    ExcelMongo excelMongo;

    @Override
    public Map<String, String> sorts() {
        Map<String, String> sorts = new LinkedHashMap<>();
        sorts.put("id", "desc");
        sorts.put("status", "asc");
        sorts.put("sort", "desc");
        return sorts;
    }

    public Map<Integer, String> status() {
        Map<Integer, String> status = new LinkedHashMap<>();
        status.put(1, "启用");
        status.put(2, "禁用");
        return status;
    }

    public Map<String, String> modes() {
        Map<String, String> modes = new LinkedHashMap<>();
        modes.put("append", "追加");
        modes.put("replace", "替换");
        modes.put("overwrite", "覆盖");
        return modes;
    }

    public DataExcel info(Integer id) {
        return info(dataExcelDao, id);
    }

    public Map<String, Object> save(Map<?, ?> param, HttpServletRequest request) {
        int id = ValidateUtil.filterInteger(param.get("id"), 1, null, 0);
        String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
        if(DPUtil.empty(name)) return ApiUtil.result(1001, "名称不能为空", name);
        int status = DPUtil.parseInt(param.get("status"));
        if(!status().containsKey(status)) return ApiUtil.result(1005, "状态异常", status);
        DataExcel info;
        if(id > 0) {
            if(!rbacService.hasPermit(request, "modify")) return ApiUtil.result(9403, null, null);
            info = info(id);
            if(null == info) return ApiUtil.result(404, null, id);
        } else {
            if(!rbacService.hasPermit(request, "add")) return ApiUtil.result(9403, null, null);
            info = new DataExcel();
        }
        int count = dataExcelDao.exist(name, DPUtil.parseInt(info.getId()));
        if (count > 0) {
            return ApiUtil.result(1501, "名称已存在", name);
        }
        info.setName(name);
        info.setPks(DPUtil.implode(",", DPUtil.parseStringList(param.get("pks"))));
        info.setFields(DPUtil.stringify(param.get("fields")));
        info.setSort(DPUtil.parseInt(param.get("sort")));
        info.setStatus(status);
        info.setDescription(DPUtil.parseString(param.get("description")));
        info = save(dataExcelDao, info, rbacService.uid(request));
        return ApiUtil.result(0, null, info);
    }

    public ObjectNode search(Map<String, Object> param, Map<?, ?> args) {
        ObjectNode result = search(dataExcelDao, param, (root, query, cb) -> {
            SpecificationHelper<DataExcel> helper = SpecificationHelper.newInstance(root, cb, param);
            helper.dateFormat(configuration.getFormatDate()).equalWithIntGTZero("id");
            helper.equalWithIntNotEmpty("status").like("name");
            return cb.and(helper.predicates());
        }, Sort.by(Sort.Order.desc("sort"), Sort.Order.desc("id")), sorts().keySet());
        JsonNode rows = format(ApiUtil.rows(result));
        if(!DPUtil.empty(args.get("withUserInfo"))) {
            rbacService.fillUserInfo(rows, "createdUid", "updatedUid");
        }
        if(!DPUtil.empty(args.get("withStatusText"))) {
            fillStatus(rows, status());
        }
        return result;
    }

    public JsonNode format(JsonNode rows) {
        for (JsonNode row : rows) {
            ObjectNode node = (ObjectNode) row;
            List<String> pks = DPUtil.parseStringList(node.at("/pks").asText(""));
            node.replace("pks", DPUtil.toJSON(pks));
            node.replace("fields", DPUtil.parseJSON(node.at("/fields").asText("[]")));
        }
        return rows;
    }

    public boolean remove(List<Integer> ids) {
        return remove(dataExcelDao, ids);
    }

    public JsonNode fillInfo(JsonNode rows, String ...properties) {
        return fillInfo(dataExcelDao, rows, properties);
    }

    public JsonNode fillInfos(JsonNode rows, String ...properties) {
        return fillInfos(dataExcelDao, rows, properties);
    }

    public Map<String, Object> upload(Map<String, Object> param, MultipartFile file) {
        DataExcel info = info(DPUtil.parseInt(param.get("id")));
        if (null == info) return ApiUtil.result(1404, "配置不存在", param);
        JsonNode fields = DPUtil.parseJSON(info.getFields());
        if (null == fields || !fields.isArray() || fields.isEmpty()) {
            return ApiUtil.result(1002, "字段配置为空", info.getId());
        }
        String mode = DPUtil.parseString(param.get("mode"));
        if (!modes().containsKey(mode)) return ApiUtil.result(1003, "模式异常", mode);
        // 解析主键列表
        Set<String> pkSet = DPUtil.parseStringList(info.getPks()).stream()
                .filter(s -> !DPUtil.empty(s)).collect(Collectors.toSet());
        if (pkSet.isEmpty()) return ApiUtil.result(1004, "主键配置为空", info.getId());
        // 构建字段映射：表头标题 -> 字段名/类型
        Map<String, String> titleMap = new LinkedHashMap<>();
        Map<String, String> typeByTitle = new LinkedHashMap<>();
        for (int i = 0; i < fields.size(); i++) {
            JsonNode field = fields.get(i);
            String name = field.at("/name").asText("");
            String title = field.at("/title").asText("");
            String header = DPUtil.empty(title) ? name : title;
            if (DPUtil.empty(header)) continue;
            titleMap.put(header, name);
            typeByTitle.put(header, field.at("/type").asText("string"));
        }
        // 解析Excel文件
        List<Document> documents = new ArrayList<>();
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            int first = sheet.getFirstRowNum();
            int last = sheet.getLastRowNum();
            // 解析表头行（第一行），将列索引映射到对应字段
            Map<Integer, String> nameMap = new LinkedHashMap<>();
            Map<Integer, String> typeMap = new LinkedHashMap<>();
            Row headerRow = sheet.getRow(first);
            if (null != headerRow) {
                int firstCol = headerRow.getFirstCellNum();
                int lastCol = headerRow.getLastCellNum();
                for (int c = firstCol; c < lastCol; c++) {
                    Cell cell = headerRow.getCell(c);
                    String header = null != cell ? formatter.formatCellValue(cell).trim() : "";
                    if (DPUtil.empty(header)) continue;
                    String name = titleMap.get(header);
                    if (null == name) continue;
                    nameMap.put(c, name);
                    typeMap.put(c, typeByTitle.get(header));
                }
            }
            // 从第二行开始读取数据
            for (int r = first + 1; r <= last; r++) {
                Row row = sheet.getRow(r);
                if (null == row) continue;
                Document document = new Document();
                document.put("_excelId", info.getId());
                document.put("_time", System.currentTimeMillis());
                boolean hasValue = false;
                List<String> pkValues = new ArrayList<>();
                for (Map.Entry<Integer, String> entry : nameMap.entrySet()) {
                    int col = entry.getKey();
                    String name = entry.getValue();
                    String type = typeMap.get(col);
                    Cell cell = row.getCell(col);
                    String value = null != cell ? formatter.formatCellValue(cell) : "";
                    if (!value.isEmpty()) hasValue = true;
                    Object casted = castValue(value, type);
                    document.put(name, casted);
                    if (pkSet.contains(name)) {
                        pkValues.add(null != casted ? casted.toString() : "");
                    }
                }
                if (!hasValue) continue;
                String id = info.getId() + "-" + String.join("-", pkValues);
                document.put("_id", id);
                documents.add(document);
            }
        } catch (IOException e) {
            return ApiUtil.result(5001, "解析Excel失败", e.getMessage());
        }
        if (documents.isEmpty()) return ApiUtil.result(5002, "未解析到有效数据", null);
        int total = documents.size();
        long affected = 0;
        try {
            excelMongo.switchTable(info.getId());
            if ("overwrite".equals(mode)) {
                excelMongo.collection().drop();
                affected = excelMongo.upsert(documents).getUpserts().size();
            } else if ("replace".equals(mode)) {
                affected = excelMongo.replace(documents).getUpserts().size();
            } else if ("append".equals(mode)) {
                affected = excelMongo.append(documents).getUpserts().size();
            }
            return ApiUtil.result(0, null, DPUtil.buildMap(
                    "mode", mode, "total", total, "affected", affected));
        } catch (Exception e) {
            return ApiUtil.result(5003, "存储数据失败，请核查数据内容", e.getMessage());
        }
    }

    /**
     * 根据字段类型转换值
     */
    private Object castValue(String value, String type) {
        if (DPUtil.empty(value)) return null;
        return switch (type) {
            case "integer" -> {
                try {
                    yield Long.parseLong(value);
                } catch (NumberFormatException e) {
                    yield value;
                }
            }
            case "double", "float" -> {
                try {
                    yield Double.parseDouble(value);
                } catch (NumberFormatException e) {
                    yield value;
                }
            }
            case "boolean" -> {
                String lower = value.toLowerCase();
                yield "true".equals(lower) || "1".equals(lower) || "yes".equals(lower);
            }
            default -> value;
        };
    }

    public Map<String, Object> listMongoData(Map<String, Object> param) {
        Integer id = DPUtil.parseInt(param.get("id"));
        if (id < 1) return ApiUtil.result(1001, "所属记录异常", id);
        Bson filter = Filters.empty();
        ObjectNode result = FindUtil.search(excelMongo.switchTable(id), param, filter, "_id.asc", Arrays.asList("_id", "_excelId", "_time"));
        return ApiUtil.result(0, null, result);
    }

    public Map<String, Object> removeMongoData(Map<String, Object> param) {
        Integer id = DPUtil.parseInt(param.get("id"));
        if (id < 1) return ApiUtil.result(1001, "所属记录异常", id);
        List<String> ids = DPUtil.parseStringList(param.get("_ids"));
        long deleted = excelMongo.switchTable(id).delete(ids);
        return ApiUtil.result(0, null, DPUtil.buildMap("deleted", deleted));
    }

    public Map<String, Object> template(Integer id, HttpServletResponse response) {
        DataExcel info = info(id);
        if (null == info) return ApiUtil.result(404, "模板不存在", id);
        JsonNode fields = DPUtil.parseJSON(info.getFields());
        if (null == fields || !fields.isArray() || fields.isEmpty()) {
            return ApiUtil.result(1002, "字段配置为空", id);
        }
        Set<String> pkSet = DPUtil.parseStringList(info.getPks()).stream()
                .filter(s -> !DPUtil.empty(s)).collect(Collectors.toSet());
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(DPUtil.empty(info.getName()) ? "Sheet1" : info.getName());
            // 表头样式
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 11);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            // PK列高亮样式
            CellStyle pkStyle = workbook.createCellStyle();
            pkStyle.cloneStyleFrom(headerStyle);
            Font pkFont = workbook.createFont();
            pkFont.setBold(true);
            pkFont.setFontHeightInPoints((short) 11);
            pkFont.setColor(IndexedColors.RED.getIndex());
            pkStyle.setFont(pkFont);
            // 创建表头行
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < fields.size(); i++) {
                JsonNode field = fields.get(i);
                String name = field.at("/name").asText("");
                String title = field.at("/title").asText("");
                String header = DPUtil.empty(title) ? name : title;
                boolean isPk = pkSet.contains(name);
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(header);
                cell.setCellStyle(isPk ? pkStyle : headerStyle);
                sheet.setColumnWidth(i, Math.max(header.getBytes(StandardCharsets.UTF_8).length * 256 + 2048, 4096));
            }
            // 设置响应头
            String filename = "template-" + info.getId() + ".xlsx";
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            String encodedName = URLEncoder.encode(info.getName() + "-" + filename, StandardCharsets.UTF_8).replace("+", "%20");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + encodedName);
            ServletOutputStream out = response.getOutputStream();
            workbook.write(out);
            out.flush();
            return null;
        } catch (IOException e) {
            return ApiUtil.result(5001, "生成模板失败", e.getMessage());
        }
    }

}
