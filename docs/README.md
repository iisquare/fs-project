# 帮助文档

> 环境要求

- JDK8+
- Gradle4+
- Scala-2.11.x

> 项目导入

- cd etl-visual/code
- gradle eclipse
- 打开Eclipse，执行导入(import)
- 在项目上单击右键，选择configure->Convert to Gradle(STS) Project
- 在项目上单击右键，选择Gradle(STS)->Refresh All

> 开发调试

- gradle deployTest
- 执行com.iisquare.jwframe.test.jetty.WebRunner.java

> 远程Spark配置

- 修改code/src/main/resources/spark.properties文件中的master地址
- 执行gradle sparkJar生成提交JAR文件
- 执行com.iisquare.jwframe.test.spark.TestSubmitter.java
