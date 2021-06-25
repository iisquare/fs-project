# OA模块

## 交互设计

### 表单权限

- 表单布局设计对全部用户可见。
- 拥有“可见”权限时展示为用户录入内容。
- 拥有“可编辑”权限时可对数据进行修改。
- 无“可见”和“可编辑”时，不展示任何（含默认值）内容。
- 当子表单无“可编辑”权限时，不可增删改子表单内的任何记录。

### 节点描述

- BPMN中支持添加多个描述，本项目中合并为单个。
```
<startEvent id="Event_0ozyigz" name="">
  <documentation>doc1</documentation>
  <documentation>doc2</documentation>
</startEvent>
```

## BPMN

- UserTask
```
<userTask id="parallelTask" name="并行多实例" flowable:assignee="${parallel}">
  <documentation>并行多实例测试</documentation>
  <multiInstanceLoopCharacteristics isSequential="false" flowable:collection="parallelList" flowable:elementVariable="parallel">
    <completionCondition>${nrOfCompletedInstances == 1}</completionCondition>
  </multiInstanceLoopCharacteristics>
</userTask>
```

- moddleExtensions
```
extends - 用于原节点拓展
superClass - 派生新的节点
```

## 参考

- [bpmn-js walkthrough](https://bpmn.io/toolkit/bpmn-js/walkthrough/)
- [全网最详bpmn.js教材目录](https://juejin.cn/post/6844904017567416328)
- [flowable-bpmn-modeler](https://github.com/bzw1204/flowable-bpmn-modeler)
