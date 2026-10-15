-- v3 -> v4：项目增加模型（model）
-- 说明：项目可以直接选用服务端模型清单里的模型发起模型对话，与「服务端编排应用」是两条并行的执行路径；
-- 本地只存模型名称，模型清单与调用都在服务端。

ALTER TABLE projects ADD COLUMN model TEXT DEFAULT '';
