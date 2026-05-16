package com.iisquare.fs.app.spark.mongo;

import com.iisquare.fs.base.core.util.DPUtil;
import com.mongodb.spark.sql.connector.config.MongoConfig;
import org.apache.spark.sql.connector.catalog.Table;
import org.apache.spark.sql.connector.catalog.TableProvider;
import org.apache.spark.sql.connector.expressions.Transform;
import org.apache.spark.sql.sources.DataSourceRegister;
import org.apache.spark.sql.types.StructType;
import org.apache.spark.sql.util.CaseInsensitiveStringMap;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * 定制化Mongo加载器
 * 用于Session长连接，适合固定结构的小批量数据读取
 * 特性：
 *  - 结构固定，pipeline仅用于配置筛选条件，不支持谓词下推
 *  - 通过离线方式缓存集合结构，保持连接句柄，需手动进行释放
 * 官方优缺点：
 *  - 支持动态下推，需通过采样确定数据结构，若采样不足可能丢失部分结构信息
 *  - 支持分区加载，但采样和分区过程准备时间较长，处理结构信息耗时较大
 *  使用说明：
 *      Properties properties = PropertiesUtil.load(getClass().getClassLoader(), "ddl.properties");
 *      FSMongoTableProvider.registerDDL(uri, properties);
 *      Dataset<Row> dataset = session.read().format(FSMongoTableProvider.class.getName()).options(config).load();
 *      dataset.show();
 *      FSMongoTableProvider.release();
 */
public class FSMongoTableProvider implements TableProvider, DataSourceRegister {

    public static Map<String, Properties> uriDDL = new LinkedHashMap<>();

    @Override
    public StructType inferSchema(CaseInsensitiveStringMap options) {
        String uri = options.get("spark.mongodb.read.connection.uri");
        String database = options.get("spark.mongodb.read.database");
        String collection = options.get("spark.mongodb.read.collection");
        Properties properties = uriDDL.get(uri);
        if (null == properties) {
            throw new RuntimeException(String.format("ddl missing for %s.%s", database, collection));
        }
        String ddl = properties.getProperty(String.format("%s.%s", database, collection));
        if (DPUtil.empty(ddl)) {
            throw new RuntimeException(String.format("ddl empty for %s.%s", database, collection));
        }
        return StructType.fromDDL(ddl);
    }

    @Override
    public Table getTable(StructType schema, Transform[] partitioning, Map<String, String> properties) {
        return new FSMongoTable(schema, partitioning, MongoConfig.createConfig(properties));
    }

    @Override
    public String shortName() {
        return "fs-mongodb";
    }

    @Override
    public boolean supportsExternalMetadata() {
        return true;
    }

    public static Properties registerDDL(String uri, Properties properties) {
        return uriDDL.put(uri, properties);
    }

    public static void release() {

    }
}
