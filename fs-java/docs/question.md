# question and answer
常见问题及解决方案

- Flink提交作业

    CliFronted客户端不要与主Jar放在一起，不然会导致类查找异常。
    - Cli：支持-C(--classpath)参数提交依赖Jar，实现类为CliFronted。
    - Rest：通过API接口先提交Jar文件，之后通过/jars/:jarid/run运行作业，不支持附加依赖。

- java.sql.SQLException: Value '0000-00-00' can not be represented as java.sql.Date
  - Java在处理MySQL中类型为Date，值为'0000-00-00'的字段时，会抛出异常。此时，可在jdbc连接url后面添加zeroDateTimeBehavior=convertToNull
  