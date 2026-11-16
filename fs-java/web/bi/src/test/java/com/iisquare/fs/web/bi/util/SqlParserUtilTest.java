package com.iisquare.fs.web.bi.util;

import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.Select;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class SqlParserUtilTest {

    @Test
    public void stripsTrailingSemicolonsAndWhitespace() {
        String sql = "  SELECT * FROM orders;;  \n";

        String normalized = SqlParserUtil.stripTrailingSemicolons(sql);

        Assert.assertEquals("SELECT * FROM orders", normalized);
    }

    @Test
    public void keepsSemicolonInsideStringLiteral() throws Exception {
        String sql = "SELECT ';' AS value";

        String normalized = SqlParserUtil.stripTrailingSemicolons(sql);
        Statement statement = SqlParserUtil.parse(normalized);

        Assert.assertEquals(sql, normalized);
        Assert.assertTrue(statement instanceof Select);
    }

    @Test
    public void parsesNormalizedDatasetQueryAsSelect() throws Exception {
        String normalized = SqlParserUtil.stripTrailingSemicolons("SELECT * FROM orders;\n");

        Statement statement = SqlParserUtil.parse(normalized);

        Assert.assertTrue(statement instanceof Select);
        Assert.assertEquals(List.of("orders"), SqlParserUtil.tableNames(statement));
    }
}
