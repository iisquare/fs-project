# future

## 开发计划

- 分析层：支持State类型、执行计划配置、历史结果导出、[权限管理]
- 报表层：二次计算、NoteBook分享(Tableau)
- 计算优化：模型评估、中间结果管理和复用、查询优化
- 预置模型：机器学习、神经网络（SPSS、Matlab）

## 遗留问题

- Flink1.9+版本调整了Table架构，并引入了Planner，未来打算统一流批处理
- SQL->DataSet->First乱序原因未知
- 插件上传描述乱码
- xlab tensor position全为0时会产生java.lang.IllegalArgumentException: indices[0] = -1 is not in [0, 200)异常
- POI Sheet getRow Returns the logical row (not physical) 0-based
