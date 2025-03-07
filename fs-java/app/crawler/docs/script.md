# Script Engine

## Assist - 辅助器
```
(function (context) {
    return {
        type: "辅助器类型",
        property: { 辅助器参数 }
    };
})({
    url: "当前请求地址",
    scheduleId: "任务标识",
    templateKey: "模板标识",
    param: "任务参数",
    content: "页面内容",
    data: "解析结果",
    status: "执行状态",
    exception: "执行异常",
    assist: "请求代理"
});
```

## Mapper - 映射器
```
(function (context) {
    return {
        output: [ 输出结果 ],
        fetch: {
            "页面标识": [ 任务参数 ]
        },
        iterate: "迭代链接地址"
    };
})({
    url: "当前请求地址",
    scheduleId: "任务标识",
    templateKey: "模板标识",
    param: "任务参数",
    content: "页面内容",
    data: "解析结果",
    status: "执行状态",
    exception: "执行异常",
    assist: "辅助器执行结果"
});
```

## Intercept - 拦截器
- Mapper return next - 拦截器中的映射器通过next返回值指定处理方式
  - retry - 重试
  - retryWithTokenHalt - 停顿Token并重试
  - discard - 丢弃
- Status code - 可拦截的状态码
  - HTTP Code
  - Less Than Zero - Process Error
  - Default
    - 404 - discard
    - 200 - parse
    - other - discard
    
## Build In - 内置函数
- Object.assign(Object... params) - 合并对象
- DateUtil - 日期工具类
  - format([Date date], [String pattern]) - 格式化日期
