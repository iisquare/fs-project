# Agent S3 存储收敛与迁移

## 目标布局

Agent 服务的新文件统一写入 `fs-agent` 桶，顶层目录只按场景划分：

```text
fs-agent/
  bot-uploads/{yyyyMMdd}/...
  mcp-uploads/{yyyyMMdd}/...             历史 MCP 文件迁移位置
  agentic-uploads/{yyyyMMdd}/...
  client-versions/{versionId}/...
  knowledge-documents/{knowledgeId}/{yyyyMMdd}/...
  knowledge-images/{knowledgeId}/{yyyyMMdd}/...
  skill-versions/{skillId}/...
```

当前代码只改变新对象写入位置，不改变历史对象读取位置。文件读取始终按
`fs_file_archive.bucket + filepath` 定位，所以可以先上线代码，再分批迁移存量对象。

## 读写链路

1. 写入：Agent 服务组装 `bucket`、`filepath` 和 `traceIdentity`，通过
   `StreamFileUploader` 调用文件服务 `/file/upload`。
2. 落库：文件服务上传 S3 成功后写 `fs_file_archive`；业务表只保存 `file_id`，知识库图片额外保存
   `bucket + filepath`。
3. 读取：业务端按 `file_id` 调 `/file/url` 或 `/file/download`，再由 `/file/{id}`、`/raw/{id}`
   从 archive 记录的实际桶读取。
4. 删除：按 `file_id` 找到 archive 记录，再删除记录中的实际 `bucket + filepath`。

因此不要直接改数据库中的 `bucket/filepath`，必须先把对象复制到目标位置，确认可读后再切数据库。

## 存量桶映射

代码历史中可能出现以下来源桶：

| 来源桶 | 原路径 | 目标路径 |
| --- | --- | --- |
| `fs-mcp` | `bot/...` | `bot-uploads/...` |
| `fs-mcp` | `tool/...` | `mcp-uploads/...` |
| `fs-mcp` | `client-versions/...` | `client-versions/...` |
| `fs-agentic` | `agentic/...` | `agentic-uploads/...` |
| `fs-knowledge` | `knowledge-{id}/image/...` | `knowledge-images/{id}/...` |
| `fs-knowledge` | `knowledge-{id}/...` | `knowledge-documents/{id}/...` |
| `fs-skill` | `skill-{id}/versions/...` | `skill-versions/{id}/...` |
| `fs-lm-knowledge` | `knowledge-{id}/image/...` | `knowledge-images/{id}/...` |
| `fs-lm-knowledge` | `knowledge-{id}/...` | `knowledge-documents/{id}/...` |
| `fs-lm-knowledge` | `agentic/...` | `agentic-uploads/...` |
| `fs-lm-skill` | `skill-{id}/versions/...` | `skill-versions/{id}/...` |

## 迁移原则

1. 先创建目标桶 `fs-agent`，并确认文件服务账号有 `PutObject`、`GetObject`、`DeleteObject`
   与分片上传权限。
2. 冻结来源桶的删除操作，仅复制对象；复制期间旧代码或旧记录仍可正常读取。
3. 复制后先做对象数量、大小和抽样 SHA/MD5 校验，再更新数据库定位。
4. 更新后验证文档下载、知识库图片、技能包、安装包、编排附件和聊天附件各一条。
5. 来源桶保留只读观察期，确认没有遗漏后再归档或删除；迁移工具不应在同一事务中复制并删除源对象。

## 数据更新顺序

对象复制完成后，先更新中央文件表，再更新业务展示字段：

```sql
-- 示例：fs-mcp 的聊天附件迁移到 fs-agent
UPDATE fs_file_archive
SET bucket = 'fs-agent',
    filepath = CONCAT('bot-uploads/', SUBSTRING(filepath, 5))
WHERE bucket = 'fs-mcp'
  AND filepath LIKE 'bot/%'
  AND deleted_time = 0;

-- 编排附件
UPDATE fs_file_archive
SET bucket = 'fs-agent',
    filepath = CONCAT('agentic-uploads/', SUBSTRING(filepath, 9))
WHERE bucket IN ('fs-agentic', 'fs-lm-knowledge')
  AND filepath LIKE 'agentic/%'
  AND deleted_time = 0;

-- 技能版本包
UPDATE fs_file_archive
SET bucket = 'fs-agent',
    filepath = REGEXP_REPLACE(filepath,
        '^skill-([^/]+)/versions/', 'skill-versions/$1/')
WHERE bucket IN ('fs-skill', 'fs-lm-skill')
  AND filepath LIKE 'skill-%/versions/%'
  AND deleted_time = 0;

-- 客户端安装包：路径不变，只切换到统一桶
UPDATE fs_file_archive
SET bucket = 'fs-agent'
WHERE bucket = 'fs-mcp'
  AND filepath LIKE 'client-versions/%'
  AND deleted_time = 0;

-- 知识库图片
UPDATE fs_file_archive
SET bucket = 'fs-agent',
    filepath = REGEXP_REPLACE(filepath,
        '^knowledge-([^/]+)/image/', 'knowledge-images/$1/')
WHERE bucket IN ('fs-knowledge', 'fs-lm-knowledge')
  AND filepath LIKE 'knowledge-%/image/%'
  AND deleted_time = 0;

-- 知识库原文档
UPDATE fs_file_archive
SET bucket = 'fs-agent',
    filepath = REGEXP_REPLACE(filepath,
        '^knowledge-([^/]+)/', 'knowledge-documents/$1/')
WHERE bucket IN ('fs-knowledge', 'fs-lm-knowledge')
  AND filepath LIKE 'knowledge-%'
  AND filepath NOT LIKE 'knowledge-%/image/%'
  AND deleted_time = 0;

-- 知识库图片业务表保存了独立副本，需要同步更新
UPDATE fs_agent_knowledge_image
SET bucket = 'fs-agent',
    filepath = REGEXP_REPLACE(filepath,
        '^knowledge-([^/]+)/image/', 'knowledge-images/$1/')
WHERE bucket IN ('fs-knowledge', 'fs-lm-knowledge')
  AND filepath LIKE 'knowledge-%/image/%';
```

技能版本、客户端安装包和知识库文档以 `file_id` 读取文件，不依赖业务表中的 `filepath`；
但为了让管理端展示与实际存储一致，应在对象复制和 `fs_file_archive` 校验完成后同步这些字段。
执行任何 `UPDATE` 前先备份，并把 `SELECT` 结果保存为迁移清单。

## 回滚

数据库切换前回滚只需继续使用旧桶。数据库切换后如目标对象异常，可按迁移清单把
`fs_file_archive` 与业务表的 `bucket/filepath` 恢复为旧值；不要先删除来源桶。
