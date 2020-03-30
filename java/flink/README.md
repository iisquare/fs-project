# flink
计算框架

## 模块说明
- :base:flink：基础依赖，需要将jars放置在libs目录
- :flink:flow：任务主jar，通过:web:admin插件管理功能上传
- :test：开发环境测试程序，不需要打包即可直接执行
- :plugins：流程图插件，通过:web:admin插件管理功能上传

## 部署流程
- 修改build.gradle中flinkVersion版本
- 处理代码兼容并测试
- 在服务器端安装对应版本，导出:base:flink:jars到libs目录
- 提交flink on yarn任务，按照运行端口修改rest配置
- 重新部署:web:flink服务
- 通过:web:admin上传:flink:flow:jar主文件
- 重新打包:flink:plugins:*:zip插件并上传

## 测试流程

- 代码测试
```
add plugin to ./flink/test/build.gradle
run :flink:test:FlowTester.main()
```
- 插件测试
```
gradle :flink:plugins:[name]:zip
upload zip plugin with cloud-rest-web
run web:flink:SubmitTester.main()
```
- 接口测试
```
:web:flink:/swagger-ui.html#!/flow-controller/submitActionUsingPOST
```


## 注意事项
- 数据类型

    在Flink Table中，数据类型必须严格一致。MySQL的bigint(20) unsigned字段对应java.math.BigInteger，在Flink中为Types.BIG_INT。在声明Flink Table表结构的时候，如果设置成Types.LONG等类型会抛出数据类型转换异常，直接使用BIG_INT提示类型不支持。从目前测试情况来看，在DataSet或DataSteam中使用是没有问题的，如果要在Table中使用，可以在流程图的returns中设置字段类型为String。
    