package com.iisquare.fs.web.bi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.web.mvc.ServiceBase;
import com.iisquare.fs.web.bi.util.SqlParserUtil;
import com.iisquare.fs.web.bi.dao.DatasetDao;
import com.iisquare.fs.web.bi.entity.Dataset;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.Select;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class OlapService extends ServiceBase {

    @Autowired
    TrinoService trinoService;

    @Autowired
    DefaultRbacService rbacService;

    @Autowired
    DatasetDao datasetDao;

    public Map<String, Object> catalogs(Map<?, ?> param) {
        ObjectNode result = DPUtil.objectNode();
        ArrayNode catalogs = result.putArray("catalogs");
        try {
            trinoService.query("SELECT catalog_name FROM system.metadata.catalogs ORDER BY catalog_name", null, resultSet -> {
                ObjectNode catalog = DPUtil.objectNode();
                catalog.put("name", resultSet.getString(1));
                catalogs.add(catalog);
            });
            return ApiUtil.result(0, null, result);
        } catch (Exception e) {
            return ApiUtil.result(1500, "获取目录列表失败", e.getMessage());
        }
    }

    public Map<String, Object> schemas(Map<?, ?> param) {
        String catalog = DPUtil.parseString(param.get("catalog"));
        if (DPUtil.empty(catalog)) {
            return ApiUtil.result(1001, "目录不能为空", null);
        }
        ObjectNode result = DPUtil.objectNode();
        result.put("catalog", catalog);
        ArrayNode schemas = result.putArray("schemas");
        try {
            String sql = "SHOW SCHEMAS FROM " + trinoService.quoteIdentifier(catalog);
            trinoService.query(sql, null, resultSet -> {
                ObjectNode schema = DPUtil.objectNode();
                schema.put("name", resultSet.getString(1));
                schemas.add(schema);
            });
            return ApiUtil.result(0, null, result);
        } catch (Exception e) {
            return ApiUtil.result(1500, "获取Schema列表失败", e.getMessage());
        }
    }

    public Map<String, Object> tables(Map<?, ?> param) {
        String catalog = DPUtil.parseString(param.get("catalog"));
        String schema = DPUtil.parseString(param.get("schema"));
        if (DPUtil.empty(catalog)) {
            return ApiUtil.result(1001, "目录不能为空", null);
        }
        if (DPUtil.empty(schema)) {
            return ApiUtil.result(1002, "Schema不能为空", null);
        }
        ObjectNode result = DPUtil.objectNode();
        result.put("catalog", catalog);
        result.put("schema", schema);
        ArrayNode tables = result.putArray("tables");
        try {
            String sql = "SELECT table_name, table_type FROM " + trinoService.quoteIdentifier(catalog)
                    + ".information_schema.tables WHERE table_schema = ? ORDER BY table_name";
            trinoService.query(sql, statement -> {
                statement.setString(1, schema);
            }, resultSet -> {
                ObjectNode table = DPUtil.objectNode();
                table.put("name", resultSet.getString(1));
                String type = resultSet.getString(2);
                table.put("type", "BASE TABLE".equals(type) ? "TABLE" : type);
                table.put("remark", "");
                tables.add(table);
            });
            return ApiUtil.result(0, null, result);
        } catch (Exception e) {
            return ApiUtil.result(1500, "获取表列表失败", e.getMessage());
        }
    }

    public Map<String, Object> columns(Map<?, ?> param) {
        String catalog = DPUtil.parseString(param.get("catalog"));
        String schema = DPUtil.parseString(param.get("schema"));
        String table = DPUtil.parseString(param.get("table"));
        if (DPUtil.empty(catalog)) {
            return ApiUtil.result(1001, "目录不能为空", null);
        }
        if (DPUtil.empty(schema)) {
            return ApiUtil.result(1002, "Schema不能为空", null);
        }
        if (DPUtil.empty(table)) {
            return ApiUtil.result(1003, "表名不能为空", null);
        }
        ObjectNode result = DPUtil.objectNode();
        result.put("catalog", catalog);
        result.put("schema", schema);
        result.put("table", table);
        ArrayNode columns = result.putArray("columns");
        try {
            String sql = "SELECT column_name, data_type, is_nullable, ordinal_position, comment "
                    + "FROM " + trinoService.quoteIdentifier(catalog)
                    + ".information_schema.columns WHERE table_schema = ? AND table_name = ? ORDER BY ordinal_position";
            trinoService.query(sql, statement -> {
                statement.setString(1, schema);
                statement.setString(2, table);
            }, resultSet -> {
                ObjectNode column = DPUtil.objectNode();
                column.put("name", resultSet.getString(1));
                column.put("type", resultSet.getString(2));
                column.put("size", 0);
                column.put("nullable", resultSet.getString(3));
                column.put("remark", DPUtil.parseString(resultSet.getString(5)));
                column.put("index", resultSet.getInt(4));
                columns.add(column);
            });
            return ApiUtil.result(0, null, result);
        } catch (Exception e) {
            return ApiUtil.result(1500, "获取表结构失败", e.getMessage());
        }
    }

    public ObjectNode variables() {
        ObjectNode variables = DPUtil.objectNode();
        variable(variables, "userId", "用户主键", "当前登录用户ID");
        variable(variables, "userAccount", "用户账号", "当前登录用户账号");
        variable(variables, "userName", "用户名称", "当前登录用户名称");
        variable(variables, "userRoleIds", "用户角色ID列表", "采用英文逗号分割的字符串");
        return variables;
    }

    private void variable(ObjectNode variables, String name, String label, String description) {
        ObjectNode node = variables.putObject(name);
        node.put("name", name);
        node.put("text", "${" + name + "}");
        node.put("label", label);
        node.put("description", description);
    }

    public Map<String, Object> query(Map<?, ?> param, boolean bOnlyDataset, HttpServletRequest request, HttpServletResponse response) {
        String sql = DPUtil.parseString(param.get("sql"));
        if (DPUtil.empty(sql)) {
            return ApiUtil.result(1001, "SQL不能为空", null);
        }
        boolean explain = DPUtil.parseBoolean(param.get("explain"));
        ObjectNode result = DPUtil.objectNode();
        sql = replaceVariables(sql, request);
        result.put("sql", sql);
        if (bOnlyDataset) {
            Map<String, Object> checked = checkDataset(sql, request, response);
            if (ApiUtil.failed(checked)) return checked;
        }
        if (explain) {
            sql = "EXPLAIN " + sql;
        }
        boolean download = DPUtil.parseBoolean(param.get("download"));
        int limit = ValidateUtil.filterInteger(param.get("limit"), 1, 10000, 10);
        int timeout = ValidateUtil.filterInteger(param.get("timeout"), 0, 3600, 15);
        result.put("limit", limit);
        result.put("timeout", timeout);
        try (Connection connection = trinoService.trinoDataSource.getConnection()) {
            if (bOnlyDataset) {
                connection.setCatalog(TrinoService.ICEBERG_CATALOG);
                connection.setSchema(TrinoService.DATASET_SCHEMA);
            }
            return executeQuery(result, connection, sql, limit, timeout, download, response);
        } catch (Exception e) {
            return ApiUtil.result(1501, "查询异常", e.getMessage());
        }
    }

    public Map<String, Object> checkDataset(String sql, HttpServletRequest request, HttpServletResponse response) {
        Set<Integer> userRoleIds = rbacService.roleIds(request);
        if (DPUtil.empty(userRoleIds)) {
            return ApiUtil.result(171002, "权限不足，无授权角色", null);
        }
        List<String> names;
        try {
            Statement statement = SqlParserUtil.parse(sql);
            if (!(statement instanceof Select)) {
                return ApiUtil.result(171003, "仅支持查询语句", null);
            }
            names = SqlParserUtil.tableNames(statement);
        } catch (JSQLParserException e) {
            return ApiUtil.result(171501, "SQL解析失败", e.getMessage());
        }
        List<Dataset> datasets = datasetDao.findAllByNameIn(names);
        if (datasets.isEmpty() || !DPUtil.values(datasets, String.class, "name").containsAll(names)) {
            return ApiUtil.result(171101, "数据集不存在或名称异常", names);
        }
        for (Dataset dataset : datasets) {
            if (1 != dataset.getStatus()) {
                return ApiUtil.result(171101, "数据集不存在或临时停用", dataset.getName());
            }
            List<Integer> roleIdes = DPUtil.parseIntList(dataset.getRoleIds());
            if (!roleIdes.isEmpty() && Collections.disjoint(userRoleIds, roleIdes)) {
                return ApiUtil.result(171101, "权限不足，禁止访问", dataset.getName());
            }
        }
        return ApiUtil.result(0, null, sql);
    }

    private Map<String, Object> executeQuery(ObjectNode result, Connection connection, String sql,
            int limit, int timeout, boolean download, HttpServletResponse response) throws Exception {
        try (java.sql.Statement statement = connection.createStatement()) {
            statement.setQueryTimeout(timeout);
            if (limit > 0) {
                statement.setMaxRows(limit);
            }
            boolean isQuery = statement.execute(sql);
            if (isQuery) {
                try (ResultSet resultSet = statement.getResultSet()) {
                    ArrayNode columns = columns(resultSet);
                    ArrayNode rows = rows(resultSet);
                    if (download) {
                        return download(response, columns, rows);
                    }
                    result.replace("columns", columns);
                    result.replace("rows", rows);
                }
            } else {
                result.putArray("columns");
                result.putArray("rows");
            }
            return ApiUtil.result(0, null, result);
        }
    }

    private String replaceVariables(String sql, HttpServletRequest request) {
        if (DPUtil.empty(sql) || sql.indexOf("${") < 0) return sql;
        Map<String, String> variables = variables(request);
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            sql = sql.replace("${" + entry.getKey() + "}", entry.getValue());
        }
        return sql;
    }

    private Map<String, String> variables(HttpServletRequest request) {
        Map<String, String> variables = new LinkedHashMap<>();
        JsonNode identity = rbacService.identity(request);
        int uid = DPUtil.parseInt(identity.at("/id").asInt(0));
        String account = DPUtil.parseString(identity.at("/serial").asText(""));
        String name = DPUtil.parseString(identity.at("/name").asText(""));
        Set<Integer> roleIds = DPUtil.values(identity.at("/roles"), Integer.class, "id");
        variables.put("userId", String.valueOf(uid));
        variables.put("userAccount", account.replace("'", "''"));
        variables.put("userName", name.replace("'", "''"));
        variables.put("userRoleIds", DPUtil.implode(",", roleIds));
        return variables;
    }

    private ArrayNode columns(ResultSet resultSet) throws SQLException {
        ResultSetMetaData meta = resultSet.getMetaData();
        ArrayNode columns = DPUtil.arrayNode();
        for (int i = 1; i <= meta.getColumnCount(); i++) {
            ObjectNode column = DPUtil.objectNode();
            column.put("index", i);
            column.put("name", meta.getColumnName(i));
            column.put("label", meta.getColumnLabel(i));
            column.put("type", meta.getColumnTypeName(i));
            column.put("jdbcType", meta.getColumnType(i));
            column.put("className", meta.getColumnClassName(i));
            column.put("nullable", meta.isNullable(i));
            column.put("precision", meta.getPrecision(i));
            column.put("scale", meta.getScale(i));
            columns.add(column);
        }
        return columns;
    }

    private ArrayNode rows(ResultSet resultSet) throws SQLException {
        ResultSetMetaData meta = resultSet.getMetaData();
        int count = meta.getColumnCount();
        ArrayNode rows = DPUtil.arrayNode();
        while (resultSet.next()) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 1; i <= count; i++) {
                row.put(meta.getColumnName(i), resultSet.getObject(i));
            }
            rows.add(DPUtil.toJSON(row));
        }
        return rows;
    }

    private Map<String, Object> download(HttpServletResponse response, ArrayNode columns, ArrayNode rows) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Sheet1");
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            List<String> names = new ArrayList<>();
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < columns.size(); i++) {
                JsonNode column = columns.get(i);
                String name = column.at("/name").asText("");
                String label = column.at("/label").asText("");
                names.add(name);
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(DPUtil.empty(label) ? name : label);
                cell.setCellStyle(headerStyle);
            }
            int rowIndex = 1;
            for (JsonNode row : rows) {
                Row dataRow = sheet.createRow(rowIndex++);
                for (int i = 0; i < names.size(); i++) {
                    JsonNode value = row.get(names.get(i));
                    if (null == value || value.isNull()) continue;
                    dataRow.createCell(i).setCellValue(value.asText());
                }
            }
            String filename = "olap-query-" + System.currentTimeMillis() + ".xlsx";
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            String encodedName = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + encodedName);
            ServletOutputStream out = response.getOutputStream();
            workbook.write(out);
            out.flush();
            return null;
        } catch (IOException e) {
            return ApiUtil.result(5001, "导出失败", e.getMessage());
        }
    }

}
