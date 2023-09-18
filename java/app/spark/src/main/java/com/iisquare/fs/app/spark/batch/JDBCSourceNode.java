package com.iisquare.fs.app.spark.batch;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.app.spark.core.SparkRunner;
import com.iisquare.fs.app.spark.util.SparkUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.dag.DAGCore;
import com.iisquare.fs.base.dag.source.AbstractJDBCSource;
import com.iisquare.fs.base.dag.util.DAGUtil;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

import java.util.*;

/**
 * @see(https://spark.apache.org/docs/latest/sql-data-sources-jdbc.html)
 * fetchSize参数生效前提：
 *  - MySQL
 *      com.mysql.jdbc.Statement.enableStreamingResults() -> {
 *          setFetchSize(Integer.MIN_VALUE);
 *          setResultSetType(ResultSet.TYPE_FORWARD_ONLY);
 *      }
 *  - PostgreSQL@see(https://jdbc.postgresql.org/documentation/head/query.html#fetchsize-example)
 *      setAutoCommit(false);
 */
public class JDBCSourceNode extends AbstractJDBCSource {

    List<Map<String, String>> configs = new ArrayList<>();

    @Override
    public boolean configure(JsonNode... configs) {
        JsonNode config = DPUtil.value(DAGUtil.mergeConfig(configs), kvConfigPrefix);
        if (!this.options.at("/iterable").asBoolean(false)) {
            config = DPUtil.arrayNode().add(config);
        }
        Iterator<JsonNode> iterator = config.iterator();
        while (iterator.hasNext()) {
            JsonNode options = DAGUtil.formatOptions(this.options.deepCopy(), iterator.next());
            Map<String, String> cfg = new LinkedHashMap<>();
            cfg.put("url", options.at("/url").asText());
            cfg.put("query", options.at("/sql").asText());
            cfg.put("driver", DAGCore.jdbcDriver(options.at("/driver").asText()));
            cfg.put("user", options.at("/username").asText());
            cfg.put("password", options.at("/password").asText());
            String partitionColumn = options.at("/partitionColumn").asText();
            if (!DPUtil.empty(partitionColumn)) {
                cfg.put("partitionColumn", partitionColumn);
                cfg.put("lowerBound", options.at("/lowerBound").asText());
                cfg.put("upperBound", options.at("/upperBound").asText());
                int numPartitions = options.at("/numPartitions").asInt(0);
                if (numPartitions > 0) cfg.put("numPartitions", String.valueOf(numPartitions));
            }
            // setFetchSize(Integer.MIN_VALUE) -> ((com.mysql.jdbc.Statement) statement).enableStreamingResults()
            cfg.put("fetchsize", String.valueOf(options.at("/fetchSize").asInt(0)));
            this.configs.add(cfg);
        }
        return true;
    }

    @Override
    public Object process() {
        SparkSession session = runner(SparkRunner.class).session();
        List<Dataset<Row>> list = new ArrayList<>();
        for (Map<String, String> config : configs) {
            Dataset<Row> dataset = session.read().format("jdbc").options(config).load();
            list.add(dataset);
        }
        return SparkUtil.union(list.toArray(new Dataset[0]));
    }
}
