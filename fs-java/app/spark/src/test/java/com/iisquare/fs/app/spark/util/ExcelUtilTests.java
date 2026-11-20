package com.iisquare.fs.app.spark.util;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.spark.SparkConf;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.DataTypes;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Calendar;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class ExcelUtilTests {

    @Test
    public void parseDataAddressWithTypesAndFormula() throws Exception {
        byte[] content = workbook();
        Map<String, String> options = new LinkedHashMap<>();
        options.put("dataAddress", "'数据'!");
        options.put("header", "true");

        ExcelUtil.Table table = ExcelUtil.parse(new ByteArrayInputStream(content), options);
        assertEquals(2, table.size());
        assertEquals("名称", table.schema().fields()[0].name());
        assertEquals(DataTypes.StringType, table.schema().fields()[0].dataType());
        assertEquals(DataTypes.LongType, table.schema().fields()[1].dataType());
        assertEquals(DataTypes.DoubleType, table.schema().fields()[2].dataType());
        assertEquals(DataTypes.BooleanType, table.schema().fields()[3].dataType());
        assertEquals(DataTypes.TimestampType, table.schema().fields()[4].dataType());
        assertEquals(DataTypes.LongType, table.schema().fields()[5].dataType());
        assertEquals("苹果", table.rows().get(0).getAs("名称"));
        assertEquals(10L, ((Number) table.rows().get(0).getAs("数量")).longValue());
        assertEquals(125L, ((Number) table.rows().get(0).getAs("金额")).longValue());
        assertEquals("香蕉", table.rows().get(1).getAs("名称"));
    }

    @Test
    public void dataAddressSheetTest() {
        assertEquals("语料标注", ExcelUtil.dataAddressSheet("'语料标注'!"));
        assertEquals("Sheet0", ExcelUtil.dataAddressSheet("\"Sheet0\"!A1:F100"));
        assertEquals("", ExcelUtil.dataAddressSheet(""));
    }

    @Test
    public void readCreatesSparkDataset() throws Exception {
        java.nio.file.Path file = Files.createTempFile("fs-excel-", ".xlsx");
        Files.write(file, workbook());
        SparkConf conf = new SparkConf().setMaster("local[1]").setAppName("excel-util-test")
                .set("spark.ui.enabled", "false");
        SparkSession session = SparkSession.builder().config(conf).getOrCreate();
        try {
            Map<String, String> options = new LinkedHashMap<>();
            options.put("dataAddress", "'数据'!");
            options.put("header", "true");
            Dataset<Row> dataset = ExcelUtil.read(session, file.toString(), options);
            assertEquals(2L, dataset.count());
            assertEquals("苹果", dataset.first().getAs("名称"));
            assertEquals(6, dataset.columns().length);
        } finally {
            session.stop();
            Files.deleteIfExists(file);
        }
    }

    private static byte[] workbook() throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            CellStyle dateStyle = workbook.createCellStyle();
            dateStyle.setDataFormat(workbook.createDataFormat().getFormat("yyyy-mm-dd"));
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("数据");
            org.apache.poi.ss.usermodel.Row header = sheet.createRow(0);
            String[] headers = {"名称", "数量", "单价", "启用", "日期", "金额"};
            for (int index = 0; index < headers.length; index++) {
                header.createCell(index).setCellValue(headers[index]);
            }

            org.apache.poi.ss.usermodel.Row first = sheet.createRow(1);
            first.createCell(0).setCellValue("苹果");
            first.createCell(1).setCellValue(10);
            first.createCell(2).setCellValue(12.5);
            first.createCell(3).setCellValue(true);
            Cell firstDate = first.createCell(4);
            firstDate.setCellValue(date(2026, Calendar.JANUARY, 2));
            firstDate.setCellStyle(dateStyle);
            first.createCell(5).setCellFormula("B2*C2");

            sheet.createRow(2);

            org.apache.poi.ss.usermodel.Row second = sheet.createRow(3);
            second.createCell(0).setCellValue("香蕉");
            second.createCell(1).setCellValue(20);
            second.createCell(2).setCellValue(1.25);
            second.createCell(3).setCellValue(false);
            Cell secondDate = second.createCell(4);
            secondDate.setCellValue(date(2026, Calendar.FEBRUARY, 3));
            secondDate.setCellStyle(dateStyle);
            second.createCell(5).setCellFormula("B4*C4");

            workbook.write(output);
            return output.toByteArray();
        }
    }

    private static java.util.Date date(int year, int month, int day) {
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.set(year, month, day);
        return calendar.getTime();
    }
}
