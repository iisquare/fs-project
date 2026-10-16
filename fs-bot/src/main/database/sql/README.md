# 用户库 schema 与升级脚本

用户库（`<运行目录>/userdata/<userId>/fs-bot.db`，运行目录开发模式是项目根目录、生产环境是用户主目录下的 `.fs-bot`）的 schema 按版本管理，每个版本由两份文件组成，放在同一目录下，用版本号后缀配对：

| 文件 | 作用 | 使用场景 |
| --- | --- | --- |
| `schema-vN.sql` | 该版本的**完整 schema**（所有业务表 + 索引） | 全新建库时执行 |
| `migrations-vN.sql` | `vN-1 → vN` 的**升级脚本**（只写增量变更） | 已有库升级时依次执行 |

例：v1 只有 `schema-v1.sql`（初始 schema 无需升级脚本）；将来出现 v2 变更时，目录里是 `schema-v1.sql`、`schema-v2.sql`、`migrations-v2.sql`。

## 约定

- 完整 schema 一律使用 `CREATE TABLE IF NOT EXISTS` / `CREATE INDEX IF NOT EXISTS`，保证「老库补表」也安全。
- 升级脚本只写增量语句（`ALTER TABLE`、`CREATE INDEX` 等），不要写 `BEGIN` / `COMMIT` 等事务控制语句，运行器会在一个 `SAVEPOINT` 中按版本顺序执行，任一步失败整体回滚。
- 表/列命名、默认值保持与完整 schema 一致，否则新库与升级库会出现结构差异。
- 基础设施表（`schema_version`、`_integrity`）不属于业务 schema，由 `USER_META_TABLES` 维护，不要写进版本文件。
- SQL 文件通过 `?raw` 被打包进主进程产物，打包后无需额外资源文件。

## 新增一次变更（示例：v2 给 apps 加 tags 列）

1. 复制 `schema-v1.sql` 为 `schema-v2.sql`，在完整 schema 里加上 `tags TEXT DEFAULT ''`；
2. 新建 `migrations-v2.sql`，写入增量语句：

   ```sql
   ALTER TABLE apps ADD COLUMN tags TEXT DEFAULT '';
   ```

3. 在 `src/main/database/schema.ts` 的 `USER_SCHEMAS` 里登记 `2: v2Schema`，在 `src/main/database/migrations.ts` 的 `USER_MIGRATIONS` 里登记 `2: v2`。

`USER_SCHEMA_VERSION` 取已登记的最大版本号，无需手工维护。缺版本文件或缺少中间版本的升级脚本时，应用会在启动时直接报错，避免静默跳过变更。

查看某个库当前版本、导出实际 DDL、升级后的复核命令，以及排错与回滚步骤，见 [`../../../../scripts/README.md`](../../../../scripts/README.md)。
