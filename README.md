# fs-project，Full Stack Project

## 项目支撑
- 企业级低代码开发平台，集成基础服务组件，含表单设计、工作流程设计、智能报表、大屏设计、应用设计、脚手架等功能；可有效节省项目开发成本，提高代码产出效率，保障项目开发质量，积累优质解决方案。在线设计完成的应用可直接发布使用，也可生成代码进行二次开发。
- 大数据分析和治理平台，包含数据交换、定时任务、元数据管理、数据标准、数据质量、数据安全、数据资产、生命周期管理等模块。

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
- 大数据计算采用有向图方式开发，支持Spark、Flink多种计算框架，可根据实际业务需求自由切换。

## 项目演示

### 模块说明

- 用户中心：帐号、角色、资源、菜单、配置。
- 网页爬虫：节点信息、模板管理、自动打码。
- 商业智能：数据清洗、规则引擎、智能报表。
- 人脸识别：人脸检测、人脸识别、检索对比。
- 搜索引擎：词典管理、索引示例、服务重载。
- 服务管理：消息队列、定时任务。
- 在线办公：表单设计、流程设计、在线审批。
- 演示实例：基础组件、功能演示、示例代码。
- 文件存储：文件存储、图库图床、对象存储。
- 项目管理：脚手架、页面设计、应用设计。
- 数据治理：数据接入、元数据、集成同步。

### 效果示例

| 功能 | 示例 | 描述 |
| :----- | :----- | :----- |
| 规则设计器 | [gif](https://raw.githubusercontent.com/iisquare/fs-project-resource/main/static/demo/bi-dag-diagram.gif)/[mp4](https://raw.githubusercontent.com/iisquare/fs-project-resource/main/static/demo/bi-dag-diagram.mp4) | 流批一体规则编排 |
| 表单设计器 | [gif](https://raw.githubusercontent.com/iisquare/fs-project-resource/main/static/demo/oa-form-design.gif)/[mp4](https://raw.githubusercontent.com/iisquare/fs-project-resource/main/static/demo/oa-form-design.mp4) | 无 |
| 页面设计器 | [gif](https://raw.githubusercontent.com/iisquare/fs-project-resource/main/static/demo/auto-layout-design.gif)/[mp4](https://raw.githubusercontent.com/iisquare/fs-project-resource/main/static/demo/auto-layout-design.mp4) | 无 |
| 流程设计器 | [gif](https://raw.githubusercontent.com/iisquare/fs-project-resource/main/static/demo/oa-flow-design.gif)/[mp4](https://raw.githubusercontent.com/iisquare/fs-project-resource/main/static/demo/oa-flow-design.mp4) | 无 |
| 数据钻取报表 | [gif](https://raw.githubusercontent.com/iisquare/fs-project-resource/main/static/demo/bi-drill-table.gif)/[mp4](https://raw.githubusercontent.com/iisquare/fs-project-resource/main/static/demo/bi-drill-table.mp4) | 支持多维度多度量钻取 |
| 数据分布报表 | [gif](https://raw.githubusercontent.com/iisquare/fs-project-resource/main/static/demo/bi-distribution-table.gif)/[mp4](https://raw.githubusercontent.com/iisquare/fs-project-resource/main/static/demo/bi-distribution-table.mp4) | 支持任意维度分组统计 |
| 元数据血缘分析 | [gif](https://raw.githubusercontent.com/iisquare/fs-project-resource/main/static/demo/govern-meta-blood.gif)/[mp4](https://raw.githubusercontent.com/iisquare/fs-project-resource/main/static/demo/govern-meta-blood.mp4) | 支持元数据检索和关联分析 |

### 自定义表单
- 默认自动托管数据，支持定制化筛选和高级搜索。
- 系统不提供数据库设计器（推荐模型同步），可手动关联已存在的物理表。
- 集成脚手架，可逆向生成项目代码。
- 支持子表单、数据字典、组织架构等自定义组件。

### 自定义工作流
- 支持与自定义表单关联，可逆向生成项目代码。
- 集成流程设计器，可定制化表单读写权限。

### 数据分析和智能报表
- 集成大数据DAG计算平台，支持自定义数据模型、多数据源聚合、多维度实时分析。
- 支持个性化数据报表，通过数据矩阵精确匹配数据统计项。

### 自定义大屏和应用设计器
- 提供DataV基础设计器面板，集成自定义报表组件和装饰效果。
- 组态可视化，支持2D和3D组件交互、钻取、联动、跳转。
- 个性化定制页面内容，所见即所得生成定制化应用。

### 分布式任务调度
- 基于Zookeeper和Quartz开发，支持节点、作业、触发器管理。
- 支持任务编排和日志管理，采用Antv X6开发任务工作流设计。

## 特性实验室
- 人脸检测和识别。
- 图像语义分割和分类。
- 图片和视频去水印。
- 图片隐藏盲水印。
- CSS字体加解密。
- 手势和滑块验证码。

## 开发计划
- 数据质量采用流程编排并生成质检报告。
- 数据交换集成接口管理和数据加密、解密、脱敏等安全规则。
- 文件管理模块集成MinIO对象存储。
- 打印模板设计器开发（分文档型、组件型，可套打、续打）。
- 应用设计器开发（弹性布局，含表单、列表、报表、大屏等）。

## 开源协议

- 个人可无偿使用本项目下的所有源码，但需保留本项目的来源和标识。
- 商业或企业团体使用需取得本项目的授权，最终解释权归本项目所有。
- 获得授权仅代表可商业使用本项目下的全部源代码，但不包含售后和问答支持。
- 本项目提供有偿问答和定制化服务，可[联系作者](mailto:iisquare@163.com)商讨具体事宜。
- 本项目接收个人或商业赞助，赞助者的提案或反馈将优先被采纳和处理。

## 赞助方式

- 支付宝或微信（Ouyang）

![Ouyang](./static/resources/images/sponsor/alipay-and-wechat.jpg)
