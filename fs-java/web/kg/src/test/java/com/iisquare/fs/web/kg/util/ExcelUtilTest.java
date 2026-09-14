package com.iisquare.fs.web.kg.util;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Excel 多工作表写入与工作表命名
 */
public class ExcelUtilTest {

    @Test
    public void writeMultipleSheets() throws Exception {
        List<ExcelUtil.SheetData> sheets = new ArrayList<>();
        sheets.add(new ExcelUtil.SheetData("实体-企业", Arrays.asList("code", "name"), Arrays.asList(
                new ArrayList<Object>(Arrays.asList("ID001", "示例名称")))));
        sheets.add(new ExcelUtil.SheetData("关系-任职", Arrays.asList("source", "target"), Arrays.asList(
                new ArrayList<Object>(Arrays.asList("ID001", "ID002")))));
        String content = ExcelUtil.write(sheets);
        Assert.assertTrue(content.length() > 0);
        List<List<String>> rows = ExcelUtil.read(Base64.getDecoder().decode(content));
        Assert.assertEquals(Arrays.asList("code", "name"), rows.get(0));
        Assert.assertEquals(Arrays.asList("ID001", "示例名称"), rows.get(1));
    }

    @Test
    public void sheetNameIsSanitizedAndUnique() {
        HashSet<String> used = new LinkedHashSet<>();
        Assert.assertEquals("实体-企业", ExcelUtil.sheetName("实体/企业", used));
        Assert.assertEquals("实体-企业(2)", ExcelUtil.sheetName("实体/企业", used));
        Assert.assertEquals("sheet1", ExcelUtil.sheetName("", new LinkedHashSet<>()));
        String name = ExcelUtil.sheetName("0123456789012345678901234567890123456789", new LinkedHashSet<>());
        Assert.assertEquals(31, name.length());
    }

}
