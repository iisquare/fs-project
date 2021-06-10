# OA模块

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
