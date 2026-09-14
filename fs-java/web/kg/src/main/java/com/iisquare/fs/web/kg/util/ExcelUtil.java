package com.iisquare.fs.web.kg.util;

import com.iisquare.fs.base.core.util.DPUtil;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Excel 读写，基于 POI 实现 xlsx 的导入导出
 */
public class ExcelUtil {

    public static final int MAX_ROWS = 50000; // 单次解析上限，避免超大文件导致内存溢出

    /**
     * 工作表内容，用于一次生成多个工作表（例如导入模板）
     */
    public static class SheetData {
        public final String name;
        public final List<String> headers;
        public final List<List<Object>> rows;

        public SheetData(String name, List<String> headers, List<List<Object>> rows) {
            this.name = name;
            this.headers = null == headers ? new ArrayList<>() : headers;
            this.rows = null == rows ? new ArrayList<>() : rows;
        }
    }

    /**
     * 生成xlsx并以Base64返回
     */
    public static String write(String sheetName, List<String> headers, List<List<Object>> rows) throws Exception {
        return write(Collections.singletonList(new SheetData(sheetName, headers, rows)));
    }

    /**
     * 生成包含多个工作表的xlsx并以Base64返回，表头加粗并冻结首行
     */
    public static String write(List<SheetData> sheets) throws Exception {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            CellStyle headerStyle = headerStyle(workbook);
            Set<String> used = new HashSet<>();
            for (SheetData data : sheets) {
                Sheet sheet = workbook.createSheet(sheetName(data.name, used));
                List<String> headers = data.headers;
                Row header = sheet.createRow(0);
                for (int index = 0; index < headers.size(); index++) {
                    Cell cell = header.createCell(index);
                    cell.setCellValue(headers.get(index));
                    cell.setCellStyle(headerStyle);
                    sheet.setColumnWidth(index, 20 * 256);
                }
                if (!headers.isEmpty()) sheet.createFreezePane(0, 1);
                int rowIndex = 1;
                for (List<Object> values : data.rows) {
                    Row row = sheet.createRow(rowIndex++);
                    for (int index = 0; index < values.size(); index++) {
                        writeCell(row, index, values.get(index));
                    }
                }
            }
            workbook.write(output);
            return Base64.getEncoder().encodeToString(output.toByteArray());
        }
    }

    protected static void writeCell(Row row, int index, Object value) {
        if (null == value) return;
        Cell cell = row.createCell(index);
        if (value instanceof Number) {
            cell.setCellValue(((Number) value).doubleValue());
        } else if (value instanceof Boolean) {
            cell.setCellValue((Boolean) value);
        } else {
            cell.setCellValue(DPUtil.parseString(value));
        }
    }

    protected static CellStyle headerStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    /**
     * 工作表名称去重并移除Excel不允许的字符
     */
    protected static String sheetName(String name, Set<String> used) {
        String value = DPUtil.trim(DPUtil.parseString(name)).replaceAll("[\\[\\]:*?/\\\\]", "-");
        if (value.length() > 31) value = value.substring(0, 31);
        if (DPUtil.empty(value)) value = "sheet" + (used.size() + 1);
        String result = value;
        int index = 2;
        while (!used.add(result)) {
            String suffix = "(" + index++ + ")";
            result = value.length() + suffix.length() > 31
                    ? value.substring(0, 31 - suffix.length()) + suffix : value + suffix;
        }
        return result;
    }

    /**
     * 读取首个工作表，全部单元格按字符串返回
     */
    public static List<List<String>> read(InputStream input) throws Exception {
        List<List<String>> rows = new ArrayList<>();
        try (Workbook workbook = new XSSFWorkbook(input)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (null == sheet) return rows;
            if (sheet.getLastRowNum() + 1 > MAX_ROWS) {
                throw new IllegalArgumentException(String.format("Excel行数超过上限%d，请拆分后再导入", MAX_ROWS));
            }
            int columns = 0;
            for (Row row : sheet) {
                columns = Math.max(columns, row.getLastCellNum());
            }
            for (Row row : sheet) {
                List<String> values = new ArrayList<>();
                for (int index = 0; index < columns; index++) {
                    values.add(cellText(row.getCell(index)));
                }
                boolean empty = true;
                for (String value : values) {
                    if (!DPUtil.empty(value)) {
                        empty = false;
                        break;
                    }
                }
                if (!empty) rows.add(values);
            }
        }
        return rows;
    }

    public static List<List<String>> read(byte[] content) throws Exception {
        try (InputStream input = new ByteArrayInputStream(content)) {
            return read(input);
        }
    }

    protected static String cellText(Cell cell) {
        if (null == cell) return "";
        CellType type = cell.getCellType();
        switch (type) {
            case STRING:
                return DPUtil.trim(cell.getStringCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return DPUtil.parseString(cell.getLocalDateTimeCellValue());
                }
                double value = cell.getNumericCellValue();
                if (value == Math.floor(value) && !Double.isInfinite(value)) {
                    return String.valueOf((long) value);
                }
                return String.valueOf(value);
            case FORMULA:
                try {
                    return DPUtil.trim(cell.getStringCellValue());
                } catch (Exception e) {
                    return String.valueOf(cell.getNumericCellValue());
                }
            default:
                return "";
        }
    }

}
