package com.iisquare.fs.spark.flow;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;

@Setter
@Getter
public abstract class BatchNode {

    protected Node current; // 所在节点实例

    public abstract Dataset<Row> run(JsonNode config) throws Exception;

}
