package com.iisquare.fs.web.spark.tester;

import com.iisquare.fs.web.spark.unit.SourceUnit;
import org.junit.Test;

import java.util.List;
import java.util.Map;

public class DataTester {

    @Test
    public void expressionTest() {
        List<Map<String, String>> result = SourceUnit.parseExpressionColumn("`fs_t_user`.`c` = `fs_t_data`.`b` and `fs_t_data`.`c` = 1");
        System.out.println(result);
    }

}
