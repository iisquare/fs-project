package com.iisquare.fs.web.bi.tester;

import com.iisquare.fs.web.bi.service.SparkService;
import org.junit.Test;

import java.util.List;
import java.util.Map;

public class DataTester {

    @Test
    public void expressionTest() {
        SparkService sparkService = new SparkService();
        List<Map<String, String>> result = sparkService.parseExpressionColumn("`fs_t_user`.`c` = `fs_t_data`.`b` and `fs_t_data`.`c` = 1");
        System.out.println(result);
    }

}
