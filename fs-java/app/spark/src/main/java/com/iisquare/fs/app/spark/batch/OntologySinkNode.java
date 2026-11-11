package com.iisquare.fs.app.spark.batch;

/**
 * 知识图谱本体写入节点。
 *
 * web:bi 在提交前按本体实体生成 MERGE/SET Cypher，运行语义与 Neo4jSink 保持一致。
 */
public class OntologySinkNode extends Neo4jSinkNode {
}
