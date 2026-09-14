# kg(Knowledge Graph,知识图谱)


## 功能模块

### 功能简介

- 本体管理，定义实体、关系、属性，仅用于与图数据库建立关联关系。
- 知识抽取，基于本体管理挂接关系数据库，导入到图数据库。
- 知识管理，基于本体管理，管理图数据库中的数据。
- 知识融合，基于图数据现有的数据，进行消歧去重。
- 知识检索，基于本体管理，实现图数据库中的数据分析查询。

## 本体建模

本体定义保存在 MySQL 的 `fs_kg_ontology` 表中，`content` 字段存放定义内容，图数据库仅存放数据本身。
本体定义同时支持以下两种格式，结构化格式保存时会自动转换为画布格式存储，便于设计器直接打开。

### 画布格式（前端设计器产物）

```json
{
  "cells": [{
    "id": "node_0",
    "shape": "kg-node",
    "data": {
      "name": "人员",
      "label": "Person",
      "description": "自然人",
      "primaryField": "id",
      "captionField": "name",
      "fields": [{ "name": "id", "title": "标识", "type": "String", "comment": "" }]
    }
  }, {
    "id": "edge_0",
    "shape": "flow-edge",
    "data": { "name": "任职", "label": "WORKS_FOR", "fields": [] },
    "source": { "cell": "node_0", "port": "right" },
    "target": { "cell": "node_1", "port": "left" }
  }]
}
```

### 结构化格式

```json
{
  "entities": [{
    "id": "person",
    "name": "人员",
    "label": "Person",
    "primaryField": "id",
    "captionField": "name",
    "fields": [{ "name": "id", "title": "标识", "type": "String" }]
  }],
  "relationships": [{
    "id": "works",
    "name": "任职",
    "label": "WORKS_FOR",
    "source": "person",
    "target": "company",
    "fields": [{ "name": "startYear", "title": "入职年份", "type": "Integer" }]
  }]
}
```

### 建模约定

- 实体标签对应节点标签，关系标签对应关系类型，字段名对应属性名，三者均只能由字母、数字、下划线组成且以字母开头。
- 实体支持多个标签：第一个为主标签，决定实体身份与数据管理范围；其余为附加标签，用于角色与分类标记。
- 实体必须设置主键字段，且主键字段必须在字段列表中定义；主键用于数据管理时的定位与合并。
- 标题字段选填，用于默认展示；关键字检索默认匹配字符串类型的字段。
- 字段可标记"必填"：仅用于数据保存时校验。索引与约束不通过本体定义，统一在索引管理、约束管理中独立创建与维护。
- 字段可标记"画布展示"：作为图探索中节点卡片的默认展示字段；用户在查看节点时可随时调整自己的展示字段，不影响本体定义。
- 实体可开启"允许扩展属性"，开启后数据管理可保存本体未声明的属性。
- 实体可开启"允许扩展标签"，开启后数据管理可为单条数据追加本体未声明的标签。
- 关系可设置"关系键"，用于区分同一对实体之间的多条同类关系；可开启"级联删除"，供数据管理删除实体时参考。
- 实体标签不可重复，关系标签不可重复；关系两端的实体必须存在。
- 校验结果通过 `/ontology/model`、`/ontology/save` 返回的 `issues` 字段给出，存在问题不影响保存设计稿，但会影响数据管理。

## 本体定义存储

实体、关系、属性的定义以规范化表保存，画布布局保留在本体的 `content` 字段，二者分离：

| 表 | 说明 |
| --- | --- |
| `fs_kg_ontology` | 本体基本信息、布局内容、定义版本与定义时间 |
| `fs_kg_ontology_entity` | 实体定义，含标签、主键、标题字段、是否允许扩展属性 |
| `fs_kg_ontology_entity_label` | 实体标签集合，首个为主标签，支持多标签节点 |
| `fs_kg_ontology_entity_field` | 实体属性定义，含类型与必填、唯一、索引标记 |
| `fs_kg_ontology_relationship` | 关系定义，含两端实体、关系键、级联删除策略 |
| `fs_kg_ontology_relationship_field` | 关系属性定义 |

- 保存本体时，画布或结构化定义会被解析并写入规范化表，同时保留画布布局；实体与关系的主键标识保持稳定，被移除的定义同步清理。
- 历史数据无需手工迁移：首次读取本体时若规范化表为空且 `content` 中存在定义，会自动迁移并落库。
- 定义读取结果按本体更新时间缓存，保存后立即失效，避免数据管理逐次解析 JSON。
- 保存接口支持传入 `version` 做乐观锁校验，版本不一致时返回 1007 并提示刷新；返回结果中同时给出校验问题 `issues`。
- 定义版本与增量编译见 `docs/fs_project_kg.sql`，其中包含新表建表语句与本体表的 `version`、`definition_time` 字段。

## 数据管理

数据管理以本体定义作为元数据，对图数据库中的数据进行增删改查与路径检索，所有字段均按本体定义校验。

| 接口 | 说明 | 关键参数 |
| --- | --- | --- |
| POST `/ontology/model` | 获取本体模型 | id |
| POST `/graph/summary` | 实体、关系数据量统计 | ontologyId |
| POST `/graph/search` | 实体数据分页检索 | ontologyId、entity、filters、keyword、page、pageSize |
| POST `/graph/info` | 实体数据详情 | ontologyId、entity、id |
| POST `/graph/save` | 实体数据保存，按主键合并 | ontologyId、entity、properties |
| POST `/graph/batch` | 批量保存实体或关系数据 | ontologyId、entity/relationship、items |
| POST `/graph/export` | 按当前筛选条件导出数据 | 与检索一致，另支持 limit（默认10000，最大50000） |
| POST `/graph/exportExcel` | 导出 xlsx（内容以 Base64 返回） | 与检索一致 |
| POST `/graph/importExcel` | 解析上传的 xlsx，返回行数据供确认 | multipart file |
| POST `/graph/importTemplate` | 生成实体/关系导入模板 | ontologyId、entity/relationship、scope（current/all）、format（xlsx/csv） |
| POST `/graph/remove` | 实体数据删除 | ontologyId、entity、ids、detach |
| POST `/graph/aggregate` | 按字段分组统计 | ontologyId、entity/relationship、field、filters |
| POST `/graph/path` | 最短路径检索 | ontologyId、fromEntity、fromId、toEntity、toId、relationships、maxDepth |
| POST `/graph/paths` | 路径推理：两点之间的全部路径 | ontologyId、fromEntity、fromId、toEntity、toId、relationships、direction、maxDepth、limit |
| POST `/graph/inspect` | 数据标签巡检 | ontologyId |
| POST `/graph/relationshipSearch` | 关系数据分页检索 | ontologyId、relationship、source、target、filters |
| POST `/graph/relationshipSave` | 关系数据保存 | ontologyId、relationship、source、target、properties |
| POST `/graph/relationshipRemove` | 关系数据删除 | ids（关系 elementId 列表） |
| POST `/graph/traverse` | 以实体数据为起点进行路径检索 | ontologyId、entity、id、direction、relationships、depth、limit |
| POST `/graph/log` | 数据变更记录查询 | ontologyId、label、kind、uid、page、pageSize |
| POST `/graph/logClean` | 清理指定时间之前的变更记录 | beforeTime |
| POST `/graph/queryList`、`/graph/querySave`、`/graph/queryDelete` | 检索方案保存与复用 | ontologyId、kind、label、name、params |

### 知识抽取

单一工作台完成全流程：维护数据源（文本内容）→ 编辑原文 → 预览抽取（实体、关系、属性）
→ 人工采纳 → 按主键 MERGE 入图。抽取只做预览、不落库，只有入图才写图数据库，
且仅写入人工采纳的候选，不在图上写任何业务标记（入图过程记录在 `fs_kg_data_log`）。

| 接口 | 说明 | 主要参数 |
| --- | --- | --- |
| POST `/extract/sourceList` | 数据源列表 | keyword |
| POST `/extract/sourceInfo` | 数据源详情（含文本） | id |
| POST `/extract/sourceSave` | 新增或编辑数据源 | id、name、type、filename、content |
| POST `/extract/sourceDelete` | 删除数据源 | ids |
| POST `/extract/preview` | 预览抽取，返回候选不落库 | ontologyId、content、threshold、entityLabel、options |
| POST `/extract/apply` | 已采纳候选入图 | ontologyId、entities[{label,properties}]、relationships[{label,source,target,properties}] |

抽取方式为「规则 + 词典」：词典取自本体实体的标题字段与主键字段在图数据中已有的取值
（按本体缓存 60 秒，编辑区实时预览不重复取数），也可通过 `options.dictionary` 传扩展词典；
实体按最长匹配识别，关系按同句共现 + 本体关系定义生成，属性按字段名关键词与数值/日期/比例/电话/编号规则抽取。
置信度：实体 0.9、属性 0.75、关系 0.7，达到阈值默认采纳。
大模型抽取需要先接入模型服务（前端已预留「大模型」选项）。

### 性能设计

- **本体定义批量读取**：读取定义时按实体、关系 ID 集合一次取回标签与字段，避免逐实体查询（原实现为 2N 次查询）。
- **结构查询直接读取**：`SHOW INDEXES`、`SHOW CONSTRAINTS` 不做缓存，始终反映数据库当前状态；数据概览的一次请求内只查询一次约束并复用结果，不会按实体重复查询。
- **批量导入单事务写入**：使用 `UNWIND $items` 一次写入，替代原来逐条一次事务的方式；导入前先整体校验，校验不通过时不写入任何数据，避免出现半截数据。
- **查询超时保护**：图遍历、最短路径、字段聚合使用带超时的读取事务（30 秒），避免异常数据导致长时间占用资源。
- **结果集保护**：导出默认 10000 行、最大 50000 行；检索默认每页 15 条、最大 500 条；结果超过 1 万条时界面给出提示。

数据删除为**物理删除**，不在节点或关系上写入任何业务标记，与 Neo4j 原生语义一致；
`detach` 不传时依据本体中关系定义的"级联删除"策略：若存在配置为不级联的关系，
接口会返回 1005 要求调用方显式确认。数据管理页的实体列表还会返回主键是否已有唯一约束（`pkUnique`），
缺失时页面给出提示并可跳转到结构对账一键补齐。

保存与删除操作会写入 `fs_kg_data_log` 变更记录（操作人、动作、对象、提交内容、结果）。
删除实体时可通过 `detach` 控制是否级联删除关系；关系保存支持按本体中的关系键匹配，保留多段同类关系。

检索条件 `filters` 支持对象与数组两种写法，数组写法可指定操作符：

```json
{
  "ontologyId": 1,
  "entity": "Person",
  "filters": [
    { "field": "age", "operator": "ge", "value": 18 },
    { "field": "name", "operator": "contains", "value": "张" }
  ],
  "sortField": "name",
  "sortOrder": "asc",
  "page": 1,
  "pageSize": 15
}
```

支持的操作符：`eq`、`ne`、`gt`、`ge`、`lt`、`le`、`contains`、`startsWith`、`endsWith`、`in`、`isNull`、`isNotNull`。

路径检索返回 `nodes`、`relationships`、`paths` 三部分，节点与关系按 elementId 去重，`paths` 中的元素为对应标识，便于前端直接渲染图结构。

## 索引与约束管理

索引与约束的增删查不受本体定义限制，可对图数据库中的任意标签、关系类型、属性进行全量管理。
结构治理由 `SchemaService` 提供完整能力，通过 `fs_kg_schema_item` 登记结构来源，支持创建前预检、
与本体方案对账、按差异增量执行。`/neo4j/*` 为历史接口适配，行为保持兼容。

### 接口

| 接口 | 说明 |
| --- | --- |
| POST `/schema/capabilities` | 数据库版本、版本类型与支持的结构类型 |
| POST `/schema/show` | 索引与约束全量查询，合并登记来源，支持名称、标签、类型、来源过滤 |
| POST `/schema/precheck` | 创建前预检：等价结构、影响数据量、唯一性冲突、存在性缺失、版本能力 |
| POST `/schema/create`、`/schema/drop` | 创建与删除结构，并登记或注销来源，支持 `dryRun` |
| POST `/schema/batch` | 批量创建与删除，逐条返回结果，单条失败不影响其余执行 |
| POST `/schema/plan` | 依据本体定义或导入的结构列表生成期望方案 |
| POST `/schema/diff` | 结构对账：待创建、待删除、已一致、命名冲突、未纳管 |
| POST `/schema/apply` | 按对账结果增量执行，支持 `dryRun` 与 `dropMissing` |
| POST `/schema/scan` | 全库结构漂移扫描，逐个本体列出待创建与命名冲突数量 |
| POST `/schema/attach` | 将数据库中已存在的等价结构登记到指定本体，用于处理命名冲突 |

### 结构定义

```json
{
  "kind": "CONSTRAINT",
  "name": "constraint_node_person_id",
  "ontologyType": "NODE",
  "subType": "UNIQUE",
  "label": "Person",
  "fields": ["id"],
  "propertyType": "",
  "ontologyId": 0
}
```

- 索引类型：RANGE、TEXT、POINT、LOOKUP，支持复合字段与关系索引。
- 约束类型：UNIQUE、NOT_NULL、KEY、RELATIONSHIP_KEY、TYPE。
- 名称为图数据库中的唯一标识，只能由字母、数字、下划线组成且以字母或下划线开头。
- 创建语句自动附加 `IF NOT EXISTS`，删除语句自动附加 `IF EXISTS`。
- 对账以"结构签名"（类型、作用对象、标签、字段、属性类型）比对，与名称无关，因此可以识别
  "等价但不同名"的结构。
- NOT_NULL、KEY、RELATIONSHIP_KEY、TYPE 为企业版特性，社区版会在预检中给出提示。
- 结构登记依赖 `fs_kg_schema_item` 表，建表语句见 `docs/fs_project_kg.sql`；未建表时其余功能仍可用，
  仅登记与来源标识缺失。

### 结构对账（登记 vs 实际）

索引与约束独立于本体定义：在结构管理页创建时自动登记（`fs_kg_schema_item`），删除时注销。
对账以"登记内容代表数据库应当存在的结构"为基准：

- `missing`：已登记但数据库中不存在，可按登记定义一键重建；
- `conflict`：同名结构在数据库中的定义与登记不一致，需要人工确认；
- `matched`：登记与实际一致；
- `unmanaged`：数据库中未登记的结构（手工创建或系统内置），可一键登记纳入管理。

`/schema/scan` 按登记来源分组统计缺失与不一致数量，用于全库漂移巡检。
`/schema/plan` 仅用于校验外部导入的结构定义清单，不再从本体生成结构。

## 演进规划

| 阶段 | 内容 | 状态 |
| --- | --- | --- |
| 一 | 结构治理：登记表、能力探测、创建前预检、批量执行、结构对账与增量应用 | 已完成 |
| 二 | 结构管理界面：索引与约束合并为统一页面，支持来源标识、批量删除、本体推荐、CQL预览、预检、结构对账 | 已完成 |
| 三 | 本体存储改造：定义规范化建表、字段级标记、关系键、定义版本与乐观锁、读取缓存与自动迁移 | 已完成 |
| 四 | 数据管理改造与界面：数据操作读取规范化定义，批量保存与变更审计，数据管理页与图探索页 | 已完成 |
| 五 | 多标签节点：固定标签集合 + 数据扩展标签，数据写入与结构治理适配标签组合 | 已完成 |
| 六 | 结构漂移巡检：定时比对登记方案与实际结构并告警 | 待实施 |
| 七 | 功能完善：数据导出、CSV导入、列排序、本体定义导入导出、本体列表展示版本与校验问题 | 已完成 |
| 八 | 数据安全与治理：级联策略生效、唯一约束提示、权限资源拆分、全库漂移扫描、标签巡检、聚合与最短路径、审计清理、单元测试 | 已完成 |
| 九 | 图探索完善、Excel 导入导出、检索方案保存、性能优化（批量读取、结构缓存、单事务导入、超时保护） | 已完成 |
| 十 | 知识融合（一期）：同本体内实体去重，规则配置、候选生成、人工审核、合并执行与留痕 | 已完成 |
| 十一 | 知识评估：依据本体定义检查图数据质量，输出评分、问题明细与历史记录 | 已完成 |

## 知识评估

检查项全部由本体定义推导，不需要单独配置规则：

| 维度 | 检查项 | 统计方式 |
| --- | --- | --- |
| 完整性 | 主键字段缺失、必填字段缺失、标题字段为空 | 全量精确 |
| 唯一性 | 主键值重复 | 全量精确 |
| 一致性 | 节点实际标签与本体标签集合不一致、关系端点标签与定义的源/目标实体不一致、属性类型与定义不符 | 标签与端点为全量精确，属性类型为抽样 |
| 规范性 | 存在本体未声明的属性（实体开启"允许扩展属性"时跳过） | 抽样 |
| 连通性 | 孤立节点（没有任何关系） | 全量精确 |

评分口径：每个检查项按"通过率 = (总量 - 问题数) / 总量"计分，范围得分取各检查项通过率的平均值，
本体总分取各范围得分的平均值；没有数据的范围不参与计分。

| 接口 | 说明 |
| --- | --- |
| POST `/assess/run` | 执行评估，参数 ontologyId、sample（抽样条数，默认 2000），返回总分、各范围明细与问题样例 |
| POST `/assess/history` | 历史评估记录 |
| POST `/assess/detail` | 查看历史评估明细 |

评估结果按实体、关系两个页签展示：每个范围显示数据量、问题数与得分，展开可看检查项与问题样例，
节点类问题支持一键跳转到数据管理定位；历史记录保留每次评估的总分与问题数，用于观察质量变化趋势。

## 权限

权限统一为 `kg:{模块}:{动作}`，索引与约束归入"本体建模:结构"：

| 权限 | 名称 | 覆盖的接口 |
| --- | --- | --- |
| `kg:ontology:` | 知识图谱:本体建模 | `/ontology/list`、`/ontology/info`、`/ontology/model`、`/ontology/config` |
| `kg:ontology:add` | 知识图谱:本体建模:添加 | `/ontology/save`（新建） |
| `kg:ontology:modify` | 知识图谱:本体建模:修改 | `/ontology/save`（修改） |
| `kg:ontology:delete` | 知识图谱:本体建模:删除 | `/ontology/delete` |
| `kg:ontology:schema` | 知识图谱:本体建模:结构 | `/schema/*` 全部；`/neo4j/*`（历史兼容） |
| `kg:graph:` | 知识图谱:图数据 | `/graph/summary`、`search`、`info`、`export`、`exportExcel`、`aggregate`、`path`、`inspect`、`traverse`、`log`、`queryList`、`querySave`、`queryDelete`、`relationshipSearch` |
| `kg:graph:add` / `kg:graph:modify` | 知识图谱:图数据:添加/修改 | `/graph/save`、`batch`、`importExcel`、`relationshipSave` |
| `kg:graph:delete` | 知识图谱:图数据:删除 | `/graph/remove`、`relationshipRemove`、`logClean` |
| `kg:fusion:` | 知识图谱:知识融合 | `/fusion/ruleList`、`candidateList`、`candidateDetail`、`recordList` |
| `kg:fusion:scan` | 知识图谱:知识融合:扫描 | `/fusion/ruleSave`、`ruleDelete`、`scan` |
| `kg:fusion:merge` | 知识图谱:知识融合:处理 | `/fusion/candidateReject`、`merge` |
| `kg:assess:run` | 知识图谱:知识评估:执行 | `/assess/run` |
| `kg:assess:history` | 知识图谱:知识评估:历史 | `/assess/history`、`detail` |

资源数据见 `docs/fs_project_member_dml.sql` 末尾的"知识图谱权限资源统一调整"段落，可重复执行；
原 `kg:neo4j:` 维护资源已由 `kg:ontology:schema` 取代，确认无其他引用后可删除。

## 知识融合

范围与策略：**限定同一本体、同一实体标签**；候选**一律人工确认**，不做自动合并；
属性冲突**保留非空值**（两侧都有值时保留人工所选的一方）；**不提供撤销**，合并过程留有记录。

| 接口 | 说明 |
| --- | --- |
| POST `/fusion/ruleList`、`/fusion/ruleSave`、`/fusion/ruleDelete` | 融合规则：本体、实体标签、参与字段与权重、相似度阈值、扫描上限 |
| POST `/fusion/scan` | 按规则扫描生成候选，返回扫描节点数与候选数 |
| POST `/fusion/candidateList`、`/fusion/candidateDetail` | 候选列表与详情（左右数据并排 + 字段级相似度） |
| POST `/fusion/candidateReject` | 标记为"不是同一实体" |
| POST `/fusion/merge` | 执行合并（指定保留哪一侧） |
| POST `/fusion/recordList` | 融合记录查询 |

实现要点：

- **候选生成**：字段值先做规范化（去空格与常见分隔符、全角转半角、小写），按字段前缀分块后只在块内两两比较，
  避免全量 O(n²)；字段相似度取"精确匹配 / 包含 / 编辑距离归一化"的最大值，再按权重加权平均；
  低于阈值的、已存在候选对的不再重复生成。
- **合并执行**：单个写事务内完成——属性按非空优先补齐、标签取并集、关系按方向与类型逐条处理
  （合并后自环直接删除；保留方已存在同类型同端点且关系键相同的则合并属性后删除多余边；其余转移过去），
  最后 `DETACH DELETE` 被合并节点，全程不写任何标记属性。
- **留痕**：`fs_kg_fusion_record` 保存双方合并前的快照与关系处理统计，同时写入 `fs_kg_data_log`（动作 FUSION_MERGE / FUSION_SCAN）。
- **页面**：`/kg/fusion/rule`、`/kg/fusion/candidate`、`/kg/fusion/record` 三个入口，对应融合规则、候选审核（左右并排对比并选择保留方）、融合记录。

## 图探索与 Excel

图探索支持三种布局（分层、环形、网格）、按标题定位节点并居中、选中节点时高亮邻居、
聚焦模式只显示选中节点及其邻居、双击节点以它为中心继续展开、导出当前画布为 PNG、
按关系类型着色的图例，以及最短路径查询与"返回上一步"的探索历史。

Excel 支持导出 `.xlsx`（服务端 POI 生成，前端把 Base64 还原为文件下载）与导入
（上传后由服务端解析为行数据，前端展示在待导入 JSON 中，确认后才写入），解析规则与 CSV 完全一致：
表头可用字段名或字段显示名，关系数据用 source/target 两列。

## 数据安全与治理

- **物理删除**：删除直接使用 `DETACH DELETE` / `DELETE`，不在图上写入业务标记，贴近 Neo4j 原生管理与运维习惯；变更过程记录在 MySQL 的 `fs_kg_data_log` 中，不侵入图数据。
- **级联策略生效**：删除实体时若本体中存在配置为不级联删除的关系，接口要求调用方显式传 `detach=true`，避免误删关系。
- **唯一约束提示**：`/graph/summary` 返回每个实体的 `pkUnique`，数据管理页对缺失主键唯一约束的实体给出警示并可跳转结构对账。
- **权限资源拆分**：图数据接口优先匹配 `kg:graph:*`，未配置时回退 `kg:ontology:*`，可平滑迁移到更细的权限粒度。
- **全库结构漂移扫描**：一次比对全部本体的期望结构与数据库实际结构，输出待创建与命名冲突数量。
- **标签巡检**：检查节点实际标签与本体定义是否一致（多标签、缺标签），给出异常数量与样例。
- **聚合与最短路径**：按字段分组统计数量；两个数据之间按关系类型与最大深度查找最短路径，并可在画布中查看。
- **审计清理**：变更记录支持时间范围查询与按时间清理，避免表无限增长。
- **单元测试**：`web:kg` 已开启测试，覆盖本体定义解析/多标签归一化/画布往返与结构语句生成/签名对账。

## 图元素标识约定

统一使用 Neo4j 5 的 `elementId`（字符串），不再使用已废弃的数字 `id` / `identity`：

- **仅用于数据排查与会话内的元素定位**：节点/关系返回体中的 `elementId`、关系的 `startElementId` / `endElementId`，用于列表行标识、画布渲染与连线、单次删除、评估问题样例展示、到图数据管理定位等场景。
- **不作为业务标识**：`elementId` 会随数据库重建、备份还原、跨库复制而变化，禁止写入业务表或参与业务逻辑判断；业务唯一标识一律使用本体定义的主键字段，关系按两端主键与关系键匹配。
- **接口返回**：图数据检索、详情、图探索、评估样例中节点为 `elementId`，关系为 `elementId` + `startElementId` + `endElementId`；关系检索按两端主键过滤，不依赖元素标识。
- **画布元素 id**：前端把 `elementId` 中的特殊字符转义后作为画布元素 id，原始 `elementId` 存在节点/边数据里，点击或双击时按数据回查业务记录。

## 多标签节点

方案为"固定标签集合（A）+ 数据可扩展标签（C）"：

- **主标签**决定实体身份，在本体内唯一；数据检索、遍历、删除都按主标签匹配。Cypher 的 `MATCH (n:Person)` 语义是"包含该标签"，因此天然命中 `Person:Employee` 这类多标签节点，检索逻辑无需特殊处理。
- **附加标签**由本体固定声明，节点创建时自动补齐。写入采用 `MERGE (n:主标签 {主键}) SET n:附加标签, n += $values`，而不是把多个标签写进 `MERGE` 模式——后者在节点缺少某个标签时会判定不匹配并新建重复节点。
- **数据扩展标签**：实体开启"允许扩展标签"后，数据保存可传 `labels` 追加标签；未开启时提交扩展标签会被拒绝。数据管理页的"实际标签"列用颜色区分本体标签与额外标签，可用于发现脏数据；工具栏的"按标签筛选"对应检索参数 `label`。
- **结构治理**：主键唯一约束默认建在主标签上（保证身份唯一），字段级唯一与索引建在完整标签组合上（`FOR (n:\`A\`:\`B\`)`）。结构签名按标签集合排序比对，因此数据库中标签顺序不同也能正确对账；组合标签的约束与索引需要 Neo4j 版本支持，创建前预检会给出提示。
- 校验补充：标签集合内去重、主标签固定首位、标签组合在本体内唯一；附加标签与其他实体的主标签重叠时给出提示，提醒该数据会同时出现在两个实体的列表中。

## 前端页面

`fs-admin` 中的结构管理页面已将索引与约束合并为统一入口，路径与菜单保持不变：

`fs-admin` 的前端页面按菜单项拆分，目录结构与菜单树一致：

```
src/views/kg/
├── modeling/     ontology.vue、er.vue、indexs.vue、constraints.vue、schema.vue
├── extraction/   data.vue
├── fusion/       rule.vue、candidate.vue、record.vue
├── assess/       run.vue、history.vue
├── retrieval/    traverse.vue
└── components/   SchemaManager.vue、SchemaReconcile.vue、FusionAll.vue、AssessAll.vue
```

每个菜单项对应一个独立页面，共享的列表与表单逻辑放在 `components` 下的组件中，避免重复实现。

| 路径 | 说明 |
| --- | --- |
| `/kg/modeling/ontology` | 本体管理 |
| `/kg/modeling/indexs` | 结构管理，默认展示索引页签 |
| `/kg/modeling/constraints` | 结构管理，默认展示约束页签 |
| `/kg/modeling/schema` | 结构管理，默认展示结构对账页签 |
| `/kg/extraction/data` | 图谱数据（数据管理），按本体字段动态生成表格与表单，支持批量导入导出与变更记录 |
| `/kg/retrieval/traverse` | 图谱探索，以实体数据为起点展开关系网络 |
| `/kg/fusion/rule`、`/kg/fusion/candidate`、`/kg/fusion/record` | 融合规则、候选审核、融合记录 |
| `/kg/assess/run`、`/kg/assess/history` | 执行评估、评估历史 |

页面交互要点：列表展示来源标签（本体生成/手工创建/未纳管/系统内置）、支持多选批量删除、
展开行与详情抽屉提供创建语句与复制；新建抽屉可选择本体后一键推荐主键唯一约束与标题字段索引，
实时展示 CQL 预览，并支持创建前预检（重复数据样例、等价结构、影响数据量）；
结构对账页签以步骤引导完成"选择本体 → 生成方案 → 查看差异 → 增量执行"，差异分为
待创建、待删除、已一致、命名冲突、未纳管五类，可勾选执行或一键补齐。

数据管理页左侧列出本体中的实体与关系类型及其数据量，右侧表格按本体字段动态生成列，
支持关键字检索、按字段高级筛选、显示字段勾选、批量导入JSON、变更记录查看；
新增与编辑抽屉按字段类型生成控件，主键在编辑时只读，必填字段先校验；
每行可一键进入图探索，以该数据为起点按方向、关系类型与深度展开关系网络，
点击画布中的节点或连线可查看属性，并支持继续展开、跳转编辑与删除。

数据管理页还支持：点击列头按字段排序（服务端排序）、按当前筛选条件导出 CSV 或 JSON
（CSV 带 BOM，Excel 直接打开不乱码）、在批量导入对话框上传 CSV 自动解析为待导入 JSON
（表头可用字段名或字段显示名，关系数据用 source/target 两列）；本体列表页展示定义版本与
校验问题数量，并支持本体定义的导出与导入（导入会新建本体，不覆盖现有定义）。
