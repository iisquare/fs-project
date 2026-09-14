# fs-project，Full Stack Project

技术中台、数据中台、AI中台、业务中台一体化服务平台，通过组合各模块合成最终系统。

## 项目支撑
- 企业级低代码开发平台，集成基础服务组件，含表单设计、工作流程设计、智能报表、大屏设计、应用设计、脚手架等功能；可有效节省项目开发成本，提高代码产出效率，保障项目开发质量，积累优质解决方案。在线设计完成的应用可直接发布使用，也可生成代码进行二次开发。
- 大数据分析和治理平台，包含数据交换、定时任务、元数据管理、数据标准、数据质量、数据安全、数据资产、生命周期管理等模块。
- 大模型和智能体编排：包含知识库、工具、集成、知识图谱、模型网关、安全围栏等模块。

## 项目结构
- fs-docker:项目所需服务的单机测试环境，参考[docker](https://github.com/iisquare/fs-docker)项目。
- fs-kubernetes:高可用环境可参考[kubernetes](https://github.com/iisquare/fs-kubernetes)项目。
- docs:开发说明文档。
- fs-java:后端项目代码。
- fs-admin:新版前端项目代码。
- fs-python:机器学习和神经网络训练模型，后期将按照功能独立拆分项目。
- static:旧版前端项目代码，springboot2.x+ant-design-vue。

## 架构选型
- 后端基于SpringBoot3.x云原生方式进行开发，可快速转换为SpringCloud或K8S等微服务运行模式。
- 前端管理后台基于Vue3 Element Plus进行开发，前台基于收录需求使用Nuxt方式开发。
- 神经网络模型采用Python编写和训练，推理框架主要为Pytorch，固化模型后给Java端预测使用。
- 分布式爬虫采用Java进行开发，选型考虑主要为生态完整、资源调度方便，缺点是占用内存过大。
- 即时通讯采用Java+SpringBoot+Netty进行开发，基于Protobuf制定多端通讯协议，支持大规模数据高并发读写。
- 大数据计算采用有向图方式开发，支持Spark、Flink多种计算框架，可根据实际业务需求自由切换。

## 项目演示

### 模块说明

- 用户中心：帐号、角色、资源、菜单、配置。
- 网页爬虫：通用采集、定向采集、任务分发、自动打码。
- 商业智能：数据集成、数据加工、数据服务、数据可视化。
- 人脸识别：人脸检测、人脸识别、检索对比。
- 搜索引擎：词典管理、索引示例、服务重载。
- 消息队列：工作节点、资源管理、队列分配。
- 定时任务：任务编排、业务RPC集成、作业管理、流程调度。
- 在线办公：表单设计、流程设计、在线审批。
- 演示实例：基础组件、功能演示、示例代码。
- 文件存储：文件存储、图库图床、对象存储。
- 项目管理：脚手架、页面设计、应用设计。
- 数据治理：元数据、血缘关系、数据标准、数据质量。
- 知识图谱：本体建模、知识抽取、知识融合、知识评估、知识检索。
- 大模型网关：安全围栏、供应商管理、积分授权、速率限制。
- 智能体平台：对比验证、知识库、工具插件、技能插件、智能体编排。

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
- 集成大数据流批一体化DAG设计平台，支持自定义数据模型、多数据源聚合、多维度实时分析。
- 支持个性化数据报表，通过数据矩阵精确匹配数据统计项。
- 跨数据源即席查询，支持API接口、Excel文件、关系型数据库、文档型数据库、数据湖等实时连表查询。

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
- 数据可视化，自定义报表、大屏。
- 打印模板设计器开发（分文档型、组件型，可套打、续打）。

## 开源协议

- 个人可无偿使用本项目下的所有源码，但需保留本项目的来源和标识。
- 商业或企业团体使用需取得本项目的授权，最终解释权归本项目所有。
- 获得授权仅代表可商业使用本项目下的全部源代码，但不包含售后和问答支持。
- 本项目提供有偿问答和定制化服务，可[联系作者](mailto:iisquare@163.com)商讨具体事宜。
- 本项目接收个人或商业赞助，赞助者的提案或反馈将优先被采纳和处理。
