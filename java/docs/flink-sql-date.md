# flink-sql-date
由于Flink对Date的支持暂不完善，所以在查询数据后可以转换成字符串进行处理。

- 表结构
```
CREATE TABLE `t_data` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `date` date DEFAULT NULL,
  `datetime` datetime DEFAULT NULL,
  `time` time DEFAULT NULL,
  `timestamp` timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `uint` int(10) unsigned DEFAULT NULL,
  `bigint` bigint(20) DEFAULT NULL,
  `ubigint` bigint(20) unsigned DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8;
```
- 测试数据
```
INSERT INTO `t_data` VALUES ('1', '2019-03-28', '2019-03-28 17:44:05', '17:44:08', '2019-03-29 09:06:26', '1', '2', '3');
INSERT INTO `t_data` VALUES ('2', '2019-03-29', '2019-03-28 17:44:38', '17:44:42', '2019-03-29 18:11:28', '1', '5', '6');
```
- 对应类型
```
[
    {"field": "id","classname": "java.lang.Integer"},
    {field": "date","classname": "java.sql.Date"},
    {"field": "datetime","classname": "java.sql.Timestamp"},
    {"field": "time","classname": "java.sql.Time"},
    {"field": "timestamp","classname": "java.sql.Timestamp"},
    {field": "uint","classname": "java.lang.Long"},
    {field": "bigint",classname": "java.lang.Long"},
    {field": "ubigint",classname": "java.math.BigInteger"}
]
```
- 查询语句
```
select * from `t_data` where `t_data`.`date`='2019-03-28'
```
- 对应异常
```
java.lang.Exception: The user defined 'open(Configuration)' method in class org.apache.flink.table.runtime.FlatMapRunner caused an exception: Table program cannot be compiled. This is a bug. Please file an issue.
```
- 去除Where条件之后的结果
```
[
    [
        {
            "id": 1,
            "date": 1553702400000,
            "datetime": 1553766245000,
            "time": "17:44:08",
            "timestamp": 1553821586000,
            "uint": 1,
            "bigint": 2,
            "ubigint": 3
        },
        {
            "id": 2,
            "date": 1553702400000,
            "datetime": 1553766278000,
            "time": "17:44:42",
            "timestamp": 1553821590000,
            "uint": 1,
            "bigint": 5,
            "ubigint": 6
        }
    ]
]
```
- 类型兼容
```
[
    {"field": "id","classname": "java.lang.Integer"},
    {field": "date","classname": "java.sql.String"},
    {"field": "datetime","classname": "java.sql.String"},
    {"field": "time","classname": "java.sql.String"},
    {"field": "timestamp","classname": "java.sql.String"},
    {field": "uint","classname": "java.lang.Long"},
    {field": "bigint",classname": "java.lang.Long"},
    {field": "ubigint",classname": "java.math.Long"}
]
```
- 兼容结果
```
[
    [
        {
            "id": 1,
            "date": "2019-03-28",
            "datetime": "2019-03-28 17:44:05.0",
            "time": "17:44:08",
            "timestamp": "2019-03-29 09:06:26.0",
            "uint": 1,
            "bigint": 2,
            "ubigint": 3
        },
        {
            "id": 2,
            "date": "2019-03-29",
            "datetime": "2019-03-28 17:44:38.0",
            "time": "17:44:42",
            "timestamp": "2019-03-29 18:11:28.0",
            "uint": 1,
            "bigint": 5,
            "ubigint": 6
        }
    ]
]
```
