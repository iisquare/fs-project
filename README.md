# fs-project
Full Stack Project

## 项目结构
- docker:项目所需服务的单机测试环境，高可用环境可参考[kubernetes](https://github.com/iisquare/kubernetes)项目。
- docs:开发说明文档。
- java:后端项目代码。
- python:机器学习和神经网络训练模型。
- static:前端项目代码。

## 架构选型
- 后端基于SpringBoot2.x云原生方式进行开发，可快速转换为SpringCloud或K8S等微服务运行模式。
- 前端管理后台基于ant-design-vue进行开发，前台基于收录和兼容性考虑使用纯jQuery方式开发。
- 神经网络模型采用Python编写和训练，推理框架主要为Tensorflow和Pytorch，固化模型后给Java端预测使用。
- 分布式爬虫采用Java+Netty进行开发，选型考虑主要为生态完整、资源调度方便，缺点是占用内存过大。
- 即时通讯采用Java+SpringBoot+Netty进行开发，基于Protobuf制定多端通讯协议，支持大规模数据高并发读写。
- 大数据计算采用有向图和插件化方式开发，目前已从Spark切换为Flink，若Flink计划落实缓慢，后期可能会换回Spark。

## 已完成功能
- 基础RBAC后台管理系统。
- 分布式爬虫和自动打码系统。
- 大数据计算和在线作业管理。

## 特性实验室
- 人脸检测和识别。
- 图像语义分割和分类。
- 图片和视频去水印。

## 开发计划
- IM分表分库支持。
- 自定义工作流。
- 表单设计器。
- 表格设计器。
- 代码生成器。
- 报表设计和生成器。
- 大屏设计器。
- 分布式定时器。
