package com.iisquare.fs.ext.spark;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.app.spark.core.SparkTypes;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.dag.core.DAGTransform;
import com.iisquare.fs.base.dag.core.DAGType;
import org.apache.spark.sql.Column;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;

import java.util.Arrays;

import static org.apache.spark.sql.functions.*;

/**
 * 从采集索引中清洗科技、工信、发改、人社等官方渠道。
 *
 * 输入：MongoSource(collection=fs_spider_index) 或任意包含 url/title 字段的 Dataset。
 * 输出：与 fs_site_entry 对应的清洗字段，可直接连接通用 JDBC 写入节点。
 */
public class OfficialSiteCleanTransformNode extends DAGTransform<Dataset<Row>> {

    private static final String SCIENCE =
            "most\\.gov\\.cn|(^|\\.)kjt\\.|(^|\\.)kjj\\.|(^|\\.)kjw\\.|科技|科学技术|科技创新";
    private static final String INDUSTRY =
            "miit\\.gov\\.cn|工业和信息化|工信";
    private static final String DEVELOPMENT =
            "ndrc\\.gov\\.cn|发展和改革|改革发展|发改委|发改";
    private static final String HUMAN_RESOURCES =
            "mohrss\\.gov\\.cn|(^|\\.)rst\\.|(^|\\.)rsj\\.|(^|\\.)rss\\.|(^|\\.)hrss\\.|人力资源|社会保障|人社";

    @Override
    public DAGType<Dataset<Row>> outputType() {
        return SparkTypes.DATASET;
    }

    @Override
    public boolean configure(JsonNode... configs) {
        return super.configure(configs);
    }

    @Override
    public Dataset<Row> process() {
        Dataset<Row> input = input(SparkTypes.DATASET);
        String urlField = options.at("/urlField").asText("url");
        String titleField = options.at("/titleField").asText("title");
        if (!Arrays.asList(input.columns()).contains(urlField)) {
            throw new IllegalArgumentException("输入数据缺少 URL 字段：" + urlField);
        }
        Column url = lower(trim(col(urlField).cast("string")));
        Column titleValue = Arrays.asList(input.columns()).contains(titleField)
                ? coalesce(col(titleField).cast("string"), lit(""))
                : regexp_extract(url, "^https?://([^/]+)", 1);
        Column title = lower(titleValue);
        Column searchText = concat_ws(" ", url, title);
        Column category = when(searchText.rlike(SCIENCE), "gov-tech")
                .when(searchText.rlike(INDUSTRY), "gov-industry")
                .when(searchText.rlike(DEVELOPMENT), "gov-development")
                .when(searchText.rlike(HUMAN_RESOURCES), "gov-human-resources");
        Column domain = regexp_extract(url, "^https?://([^/:]+)", 1);
        Column rawPath = regexp_extract(url, "^https?://[^/]+([^?#]*)", 1);
        Column firstDirectory = regexp_extract(rawPath, "^(/[^/]+/?)", 1);
        Column path = when(length(rawPath).equalTo(0), lit("/"))
                .when(rawPath.equalTo("/"), lit("/"))
                .when(regexp_extract(rawPath, "^/([^/]*\\.[^/]+)$", 1).notEqual(""), lit("/"))
                .when(length(firstDirectory).equalTo(0), lit("/"))
                .otherwise(firstDirectory);
        Dataset<Row> result = input
                .withColumn("site_url", url)
                .withColumn("site_title", title)
                .withColumn("site_sub_category", category)
                .withColumn("site_domain", domain)
                .withColumn("site_path", path)
                .withColumn("official", lit(1))
                .filter(category.isNotNull())
                .filter(domain.rlike("(^|\\.)gov\\.cn$|(^|\\.)gov\\.com\\.cn$"));
        Column now = current_timestamp().cast("long");
        return result.select(
                coalesce(nullif(trim(titleValue), lit("")), col("site_domain")).alias("name"),
                col("site_domain").alias("domain"),
                col("site_path").alias("path"),
                col("site_url").alias("url"),
                when(col("site_path").equalTo("/"), lit("portal")).otherwise(lit("subdir")).alias("entry_type"),
                lit("government").alias("category"),
                col("site_sub_category").alias("sub_category"),
                lit(0).alias("parent_id"),
                lit(0).alias("level"),
                col("official"),
                lit("fs_spider_index").alias("collection"),
                lit("utf-8").alias("charset"),
                lit("spark").alias("source"),
                lit("fs_spider_index").alias("source_collection"),
                lit(0L).alias("last_sync_time"),
                lit(0).alias("sort"),
                lit(1).alias("status"),
                lit("").alias("description"),
                now.alias("created_time"),
                lit(0).alias("created_uid"),
                now.alias("updated_time"),
                lit(0).alias("updated_uid")
        ).filter(col("url").isNotNull().and(length(col("url")).gt(0)));
    }

}
