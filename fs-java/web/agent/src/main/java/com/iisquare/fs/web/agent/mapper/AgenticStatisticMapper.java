package com.iisquare.fs.web.agent.mapper;

import java.util.List;
import java.util.Map;

/**
 * 流程统计的数据访问：与 lm 用量统计（UsageMapper）同一套做法——
 * SQL 只按条件取明细列（不取标题、入参、输出、步骤等大字段），
 * 分桶、排名与去重都留到服务层在内存里完成。
 *
 * 条件片段由服务层拼好放进 params 的 where 键（`${where}`），取值一律走 `#{}` 占位。
 */
public interface AgenticStatisticMapper {

    /** 会话明细：会话数量与「用户 / 流程」两个维度 */
    List<Map<String, Object>> chatRows(Map<String, Object> params);

    /** 对话轮次明细：一轮对话一条运行日志，成败与耗时取自这里 */
    List<Map<String, Object>> logRows(Map<String, Object> params);

}
