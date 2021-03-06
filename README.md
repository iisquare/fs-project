# fs-project
Full Stack Project：企业级快速开发平台 + 数据聚合分析平台 + 智能报表设计。

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

### 自定义表单
- 默认自动托管数据，支持定制化筛选和高级搜索。
- 系统不提供数据库设计器（太鸡肋），可手动关联已存在的物理表。
- 集成脚手架，可逆向生成项目代码。
- 支持子表单、数据字典、组织架构等自定义组件。

### 自定义工作流
- 支持与自定义表单关联，可逆向生成项目代码。
- 集成流程设计器，可定制化表单读写权限。

### 数据分析和智能报表
- 集成大数据DAG计算平台，支持自定义数据模型、多数据源聚合、多维度实时分析。
- 支持个性化数据报表，通过数据矩阵精确匹配数据统计项。

### 大屏设计器和组态可视化
- 提供DataV基础设计器面板，集成自定义报表组件。
- 组态可视化，支持2D和3D组件交互。

## 已完成功能
- 基础RBAC后台管理系统。
- 分布式爬虫和自动打码系统。
- 大数据计算和在线作业管理。
- 表单设计器和数据托管。
- 工作流和节点权限绑定。

## 特性实验室
- 人脸检测和识别。
- 图像语义分割和分类。
- 图片和视频去水印。

## 开发计划
- 大屏设计器和组态可视化。
- 智能报表与DAG计算集成。
- 数据同步平台化开发。
- 代码生成器功能拓展。
- Ceph文件管理和图库开发。
- 知识图谱和智能问答系统。
- 图像超分辨率和去水印。
- 分布式定时器。
- 线上演示系统。

## 开源协议

- 个人可无偿使用本项目下的所有源码，但需保留本项目的来源和标识。
- 商业或企业团体使用需取得本项目的授权，最终解释权归本项目所有。
- 获得授权仅代表可商业使用本项目下的全部源代码，但不包含售后和问答支持。
- 本项目提供有偿问答和定制化服务，可[联系作者](mailto:iisquare@163.com)商讨具体事宜。
- 本项目接收个人或商业赞助，赞助者的提案或反馈将优先被采纳和处理。

## 赞助方式

- 支付宝或微信（Ouyang）

![Ouyang](./static/resources/images/sponsor/alipay-and-wechat.jpg)
