package com.iisquare.fs.app.spark.util;

import com.iisquare.fs.base.core.util.DPUtil;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.catalyst.expressions.GenericRowWithSchema;
import org.apache.spark.sql.types.DataType;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 基于 Apache POI 的 Excel 读取器。
 *
 * 首版支持 xls/xlsx、指定工作表、表头和基础类型推断。Excel 本身不是可并行格式，
 * 当前实现会在 Driver 读取后创建 Dataset，适用于配置导入、调试和中小规模数据。
 */
public final class ExcelUtil {

    public static final int DEFAULT_MAX_ROWS = 100_000;
    public static final int DEFAULT_MAX_COLUMNS = 1_024;
    public static final long DEFAULT_MAX_BYTES = 100L * 1024 * 1024;

    private ExcelUtil() {
    }

    /**
     * 读取 Excel 并创建 Spark Dataset。
     *
     * options 支持：sheetName、sheetIndex、dataAddress、header、headerRow、
     * limit、maxRows、maxColumns、maxBytes。
     */
    public static Dataset<Row> read(SparkSession session, String location, Map<String, String> options)
            throws IOException {
        if (DPUtil.empty(location)) throw new IllegalArgumentException("Excel文件路径不能为空");
        Map<String, String> config = null == options ? Collections.emptyMap() : options;
        Path path = new Path(location);
        FileSystem fileSystem = path.getFileSystem(session.sparkContext().hadoopConfiguration());
        FileStatus status = fileSystem.getFileStatus(path);
        if (!status.isFile()) throw new IllegalArgumentException("Excel路径不是文件：" + location);
        long maxBytes = number(config, "maxBytes", DEFAULT_MAX_BYTES);
        if (maxBytes > 0 && status.getLen() > maxBytes) {
            throw new IllegalArgumentException(String.format(
                    "Excel文件大小超过上限%d字节，实际%d字节", maxBytes, status.getLen()));
        }
        try (InputStream input = fileSystem.open(path)) {
            return dataset(session, parse(input, config));
        }
    }

    public static Dataset<Row> read(SparkSession session, String location) throws IOException {
        return read(session, location, Collections.emptyMap());
    }

    /**
     * 兼容 spark-excel 常用的 dataAddress 写法，例如：'Sheet1'!
     */
    public static Dataset<Row> read(SparkSession session, String location,
                                    String dataAddress, boolean header) throws IOException {
        Map<String, String> options = new LinkedHashMap<>();
        options.put("dataAddress", dataAddress);
        options.put("header", String.valueOf(header));
        return read(session, location, options);
    }

    public static Dataset<Row> dataset(SparkSession session, Table table) {
        return session.createDataFrame(table.rows(), table.schema());
    }

    /**
     * 解析内存中的 Excel。该入口不依赖 Spark，便于单元测试和文件预处理。
     */
    public static Table parse(InputStream input, Map<String, String> options) throws IOException {
        Map<String, String> config = null == options ? Collections.emptyMap() : options;
        int limit = Math.max(0, number(config, "limit", 0));
        int maxRows = number(config, "maxRows", DEFAULT_MAX_ROWS);
        int maxColumns = number(config, "maxColumns", DEFAULT_MAX_COLUMNS);
        boolean header = bool(config, "header", true);

        try (Workbook workbook = WorkbookFactory.create(input)) {
            if (workbook.getNumberOfSheets() < 1) {
                throw new IllegalArgumentException("Excel中没有工作表");
            }
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            DataFormatter formatter = new DataFormatter();
            Sheet sheet = sheet(workbook, config);
            int firstRow = sheet.getFirstRowNum();
            if (firstRow < 0) return empty();

            int headerRowIndex = number(config, "headerRow", header ? firstRow : firstRow - 1);
            if (headerRowIndex < firstRow && header) {
                throw new IllegalArgumentException("表头行不能早于工作表首行：" + headerRowIndex);
            }
            org.apache.poi.ss.usermodel.Row headerRow = header ? sheet.getRow(headerRowIndex) : null;
            if (header && null == headerRow) {
                throw new IllegalArgumentException("未找到表头行：" + headerRowIndex);
            }
            int firstColumn = header ? headerRow.getFirstCellNum() : 0;
            int lastColumn = header ? headerRow.getLastCellNum() : lastColumn(sheet);
            if (firstColumn < 0 || lastColumn <= firstColumn) {
                throw new IllegalArgumentException("Excel中没有有效列");
            }
            if (lastColumn - firstColumn > maxColumns) {
                throw new IllegalArgumentException(String.format(
                        "Excel列数超过上限%d，实际%d", maxColumns, lastColumn - firstColumn));
            }

            String[] headers = headers(headerRow, firstColumn, lastColumn);
            List<RawRow> rawRows = new ArrayList<>();
            int dataStart = header ? headerRowIndex + 1 : firstRow;
            for (int rowIndex = dataStart; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                org.apache.poi.ss.usermodel.Row row = sheet.getRow(rowIndex);
                RawCell[] values = row(row, firstColumn, lastColumn, formatter, evaluator);
                if (blank(values)) continue;
                if (maxRows > 0 && rawRows.size() >= maxRows) {
                    throw new IllegalArgumentException("Excel数据行数超过上限" + maxRows);
                }
                rawRows.add(new RawRow(rowIndex + 1, values));
                if (limit > 0 && rawRows.size() >= limit) break;
            }

            DataType[] types = infer(headers, rawRows);
            StructField[] fields = new StructField[headers.length];
            for (int index = 0; index < headers.length; index++) {
                fields[index] = DataTypes.createStructField(headers[index], types[index], true);
            }
            StructType schema = DataTypes.createStructType(fields);
            List<Row> rows = new ArrayList<>(rawRows.size());
            for (RawRow rawRow : rawRows) {
                Object[] values = new Object[headers.length];
                for (int index = 0; index < headers.length; index++) {
                    values[index] = convert(rawRow.cells[index], types[index]);
                }
                rows.add(new GenericRowWithSchema(values, schema));
            }
            return new Table(schema, rows, rawRows.size());
        }
    }

    private static Table empty() {
        return new Table(DataTypes.createStructType(Collections.emptyList()),
                Collections.emptyList(), 0);
    }

    private static Sheet sheet(Workbook workbook, Map<String, String> options) {
        String sheetName = DPUtil.trim(options.get("sheetName"));
        if (DPUtil.empty(sheetName)) sheetName = dataAddressSheet(options.get("dataAddress"));
        if (!DPUtil.empty(sheetName)) {
            Sheet sheet = workbook.getSheet(sheetName);
            if (null == sheet) throw new IllegalArgumentException("工作表不存在：" + sheetName);
            return sheet;
        }
        int sheetIndex = number(options, "sheetIndex", 0);
        if (sheetIndex < 0 || sheetIndex >= workbook.getNumberOfSheets()) {
            throw new IllegalArgumentException("工作表索引越界：" + sheetIndex);
        }
        return workbook.getSheetAt(sheetIndex);
    }

    static String dataAddressSheet(String dataAddress) {
        if (DPUtil.empty(dataAddress)) return "";
        String value = dataAddress.trim();
        int mark = value.indexOf('!');
        if (mark >= 0) value = value.substring(0, mark);
        value = value.trim();
        int colon = value.indexOf(':');
        if (colon >= 0) value = value.substring(0, colon);
        if (value.length() >= 2
                && ((value.startsWith("'") && value.endsWith("'"))
                || (value.startsWith("\"") && value.endsWith("\"")))) {
            value = value.substring(1, value.length() - 1).replace("''", "'");
        }
        return value.trim();
    }

    private static int lastColumn(Sheet sheet) {
        int result = 0;
        for (org.apache.poi.ss.usermodel.Row row : sheet) {
            if (row.getLastCellNum() > result) result = row.getLastCellNum();
        }
        return result;
    }

    private static String[] headers(org.apache.poi.ss.usermodel.Row headerRow,
                                    int firstColumn, int lastColumn) {
        String[] result = new String[lastColumn - firstColumn];
        Set<String> used = new HashSet<>();
        DataFormatter formatter = new DataFormatter();
        FormulaEvaluator evaluator = headerRow.getSheet().getWorkbook().getCreationHelper().createFormulaEvaluator();
        for (int column = firstColumn; column < lastColumn; column++) {
            Cell cell = headerRow.getCell(column);
            String name = null == cell ? "" : DPUtil.trim(formatter.formatCellValue(cell, evaluator));
            if (DPUtil.empty(name)) name = "_c" + column;
            String unique = name;
            int suffix = 2;
            while (!used.add(unique)) unique = name + "_" + suffix++;
            result[column - firstColumn] = unique;
        }
        return result;
    }

    private static RawCell[] row(org.apache.poi.ss.usermodel.Row row, int firstColumn, int lastColumn,
                                 DataFormatter formatter, FormulaEvaluator evaluator) {
        RawCell[] result = new RawCell[lastColumn - firstColumn];
        for (int column = firstColumn; column < lastColumn; column++) {
            result[column - firstColumn] = cell(null == row ? null : row.getCell(column), formatter, evaluator);
        }
        return result;
    }

    private static RawCell cell(Cell cell, DataFormatter formatter, FormulaEvaluator evaluator) {
        if (null == cell) return RawCell.empty();
        CellType type = cell.getCellType();
        try {
            if ((type == CellType.NUMERIC || type == CellType.FORMULA)
                    && DateUtil.isCellDateFormatted(cell)) {
                LocalDateTime value = cell.getLocalDateTimeCellValue();
                if (null != value) return new RawCell(Timestamp.valueOf(value),
                        formatter.formatCellValue(cell, evaluator));
            }
            CellValue evaluated = type == CellType.FORMULA ? evaluator.evaluate(cell) : null;
            CellType valueType = null == evaluated ? type : evaluated.getCellType();
            switch (valueType) {
                case BOOLEAN:
                    boolean bool = null == evaluated ? cell.getBooleanCellValue() : evaluated.getBooleanValue();
                    return new RawCell(bool, formatter.formatCellValue(cell, evaluator));
                case NUMERIC:
                    double number = null == evaluated ? cell.getNumericCellValue() : evaluated.getNumberValue();
                    Object value = integral(number) ? Long.valueOf((long) number) : Double.valueOf(number);
                    return new RawCell(value, formatter.formatCellValue(cell, evaluator));
                case STRING:
                    String text = null == evaluated ? cell.getStringCellValue() : evaluated.getStringValue();
                    return new RawCell(text, text);
                case BLANK:
                case _NONE:
                    return RawCell.empty();
                case ERROR:
                default:
                    String formatted = formatter.formatCellValue(cell, evaluator);
                    return DPUtil.empty(formatted) ? RawCell.empty() : new RawCell(formatted, formatted);
            }
        } catch (RuntimeException e) {
            String formatted = formatter.formatCellValue(cell, evaluator);
            return DPUtil.empty(formatted) ? RawCell.empty() : new RawCell(formatted, formatted);
        }
    }

    private static boolean integral(double value) {
        return Double.isFinite(value) && value == Math.rint(value)
                && value >= Long.MIN_VALUE && value <= Long.MAX_VALUE;
    }

    private static boolean blank(RawCell[] cells) {
        for (RawCell cell : cells) {
            if (null != cell.value || !DPUtil.empty(cell.text)) return false;
        }
        return true;
    }

    private static DataType[] infer(String[] headers, List<RawRow> rows) {
        DataType[] result = new DataType[headers.length];
        for (int column = 0; column < headers.length; column++) {
            boolean seen = false;
            boolean strings = false;
            boolean booleans = false;
            boolean numbers = false;
            boolean allIntegral = true;
            boolean timestamps = false;
            for (RawRow row : rows) {
                Object value = row.cells[column].value;
                if (null == value) continue;
                seen = true;
                strings |= value instanceof String;
                booleans |= value instanceof Boolean;
                if (value instanceof Number number) {
                    numbers = true;
                    if (!integral(number.doubleValue())) allIntegral = false;
                }
                timestamps |= value instanceof Timestamp;
            }
            if (!seen || strings) {
                result[column] = DataTypes.StringType;
            } else if (booleans && !numbers && !timestamps) {
                result[column] = DataTypes.BooleanType;
            } else if (timestamps && !booleans && !numbers) {
                result[column] = DataTypes.TimestampType;
            } else if (numbers && !booleans && !timestamps) {
                result[column] = allIntegral ? DataTypes.LongType : DataTypes.DoubleType;
            } else {
                result[column] = DataTypes.StringType;
            }
        }
        return result;
    }

    private static Object convert(RawCell cell, DataType type) {
        if (null == cell.value) return null;
        if (DataTypes.StringType.equals(type)) return DPUtil.empty(cell.text) ? null : cell.text;
        if (DataTypes.LongType.equals(type) && cell.value instanceof Number) {
            return ((Number) cell.value).longValue();
        }
        if (DataTypes.DoubleType.equals(type) && cell.value instanceof Number) {
            return ((Number) cell.value).doubleValue();
        }
        return cell.value;
    }

    private static int number(Map<String, String> options, String key, int defaultValue) {
        String value = DPUtil.trim(options.get(key));
        if (DPUtil.empty(value)) return defaultValue;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Excel参数" + key + "必须为整数：" + value);
        }
    }

    private static long number(Map<String, String> options, String key, long defaultValue) {
        String value = DPUtil.trim(options.get(key));
        if (DPUtil.empty(value)) return defaultValue;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Excel参数" + key + "必须为整数：" + value);
        }
    }

    private static boolean bool(Map<String, String> options, String key, boolean defaultValue) {
        String value = DPUtil.trim(options.get(key));
        return DPUtil.empty(value) ? defaultValue : Boolean.parseBoolean(value);
    }

    public static final class Table implements Serializable {
        private final StructType schema;
        private final List<Row> rows;
        private final int size;

        public Table(StructType schema, List<Row> rows, int size) {
            this.schema = schema;
            this.rows = rows;
            this.size = size;
        }

        public StructType schema() {
            return schema;
        }

        public List<Row> rows() {
            return rows;
        }

        public int size() {
            return size;
        }
    }

    private static final class RawRow {
        private final int rowNumber;
        private final RawCell[] cells;

        private RawRow(int rowNumber, RawCell[] cells) {
            this.rowNumber = rowNumber;
            this.cells = cells;
        }
    }

    private static final class RawCell {
        private final Object value;
        private final String text;

        private RawCell(Object value, String text) {
            this.value = value;
            this.text = null == text ? "" : text.trim();
        }

        private static RawCell empty() {
            return new RawCell(null, "");
        }
    }
}
