package com.iisquare.fs.web.govern.rule;

import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.govern.core.Rule;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 分组对比
 * 按分组字段比较两张表的计算值是否一致
 */
public class GroupCompareRule extends Rule {
    @Override
    public Map<String, Object> execute() throws Exception {
        String[] checkGroup = DPUtil.explode(",", entity.getCheckGroup());
        String[] referGroup = DPUtil.explode(",", entity.getReferGroup());
        if (checkGroup.length < 1) {
            return ApiUtil.result(2001, "检查分组不能为空", entity.getCheckGroup());
        }
        if (checkGroup.length != referGroup.length) {
            return ApiUtil.result(2002, "检查分组与引用分组字段个数不一致", entity.getReferGroup());
        }
        List<String> filter = new ArrayList<>();
        for (int i = 0; i < checkGroup.length; i++) {
            filter.add(String.format("a.%s = b.%s", checkGroup[i], referGroup[i]));
        }
        StringBuilder sql = new StringBuilder();
        sql.append("select count(*) from (");
        sql.append("  select ").append(DPUtil.implode(", ", checkGroup));
        sql.append("  , ").append(checkMetric()).append(" as mc from ").append(entity.getCheckTable());
        sql.append("  where ").append(checkWhere());
        sql.append("  group by ").append(DPUtil.implode(", ", checkGroup));
        sql.append(") as a left join ("); // MySQL不支持全连接，可通过Spark进行full运算
        sql.append("  select ").append(DPUtil.implode(", ", referGroup));
        sql.append("  , ").append(referMetric()).append(" as mc from ").append(entity.getReferTable());
        sql.append("  where ").append(referWhere());
        sql.append("  group by ").append(DPUtil.implode(", ", referGroup));
        sql.append(") as b on ").append(DPUtil.implode(" and ", filter));
        this.checkCount = resultInteger(sql.toString());
        sql.append(" where a.mc = b.mc");
        this.expression = sql.toString();
        this.hitCount = resultInteger(expression);
        return result();
    }
}
