# scripts

开发期脚本（不参与应用运行）：

| 脚本              | 作用                                                                          | 运行方式                |
| ----------------- | ----------------------------------------------------------------------------- | ----------------------- |
| `postinstall.mjs` | 安装依赖后用 electron-builder 重编原生模块（better-sqlite3-multiple-ciphers） | `pnpm install` 自动触发 |
| `schema.mjs`      | 用户库 schema / 密钥工具：查看版本、导出 DDL、推导加密口令                    | `pnpm schema <command>` |

`schema.mjs` 通过 `electron` 运行（见 `package.json` 的 `schema` 脚本），因为原生 SQLite 模块是 Electron ABI 编译的，用普通 node 会报 `NODE_MODULE_VERSION` 不匹配。

约定：脚本的终端输出保持 ASCII/英文（`->` 而不是 `→`），因为 Windows 控制台默认 GBK 代码页会把 UTF-8 中文显示成乱码；文档本身仍用中文。

## 用户库位置

`<运行目录>/userdata/<userId>/fs-bot.db`（SQLCipher 加密 + WAL）。`<userId>` 是登录接口返回的用户 ID，例如 `userdata/1/fs-bot.db`；加密口令由账号名（serial）+ `VITE_DB_SECRET` 派生。

运行目录（见 `src/main/index.ts`）：开发模式是项目根目录，生产环境是**用户主目录下的 `.fs-bot`**（如 `C:\Users\<用户>\.fs-bot\userdata\1\fs-bot.db`）。下面的命令在开发模式按项目根目录给相对路径即可，生产环境把路径换成绝对路径。

## 常用操作（只读）

### 查看版本与状态（升级前后都建议跑一次）

```bash
pnpm schema version userdata/1/fs-bot.db --ident admin
```

输出库内版本、当前代码期望的版本（取 `src/main/database/sql/schema-v*.sql` 的最大版本号）、表清单、已存完整性哈希，并提示应用启动时会做什么：不迁移 / 依次执行哪些 `migrations-vN.sql` / 因降级而拒绝启动。

### 推导密钥（排查用）

```bash
pnpm schema key --ident admin
```

打印 SQLCipher 口令、HMAC key 和可直接使用的 `PRAGMA key` 语句；`--db-secret` 省略时取 `.env.development` / `.env.production` 里的 `VITE_DB_SECRET`。

### 导出某个库的真实 DDL

```bash
pnpm schema export userdata/1/fs-bot.db --ident admin
```

从 `sqlite_master` 导出建表/建索引语句，用于确认某个库的真实结构（正常情况下应与 `src/main/database/sql/schema-vN.sql` 一致）。

## 升级 schema（新增一次变更）

以「v2：给 apps 加 tags 列」为例：

1. 复制 `src/main/database/sql/schema-v1.sql` 为 `schema-v2.sql`，在完整 schema 里加上 `tags TEXT DEFAULT ''`（该文件始终表示这一版本的全部业务表与索引）。
2. 新建 `src/main/database/sql/migrations-v2.sql`，只写增量语句：

   ```sql
   ALTER TABLE apps ADD COLUMN tags TEXT DEFAULT '';
   ```

3. 在 `src/main/database/schema.ts` 注册完整 schema：`import v2 from './sql/schema-v2.sql?raw'`，并加进 `USER_SCHEMAS`（`2: v2`）。
4. 在 `src/main/database/migrations.ts` 注册升级脚本：`import v2 from './sql/migrations-v2.sql?raw'`，并加进 `USER_MIGRATIONS`（`2: v2`）。
5. `pnpm typecheck`（SQL 通过 `?raw` 内联进主进程产物，无需额外打包配置），然后 `pnpm dev` 观察日志：
   - 已有库：`Running user migration v2/2`
   - 全新库：`Creating user schema v2`
6. 复核：`pnpm schema version userdata/1/fs-bot.db --ident admin`，确认版本与结构符合预期。

`USER_SCHEMA_VERSION` 由 `USER_SCHEMAS` 的最大键推导，不需要手工维护。

## 规则（容易踩的坑）

- 完整 schema 一律用 `CREATE TABLE IF NOT EXISTS` / `CREATE INDEX IF NOT EXISTS`，这样早于版本管理的老库也能靠它补齐缺失表。
- 升级脚本只写增量语句，不要写 `BEGIN` / `COMMIT`；运行器已用 `SAVEPOINT` 包住所有待执行步骤，任一步失败整体回滚。
- 已发布过的版本文件不要再改，要改就新增版本号，否则已升级的库与新库会分叉。
- 版本号必须连续：库内 v1 要升到 v3 时，缺 `migrations-v2.sql` 或 `migrations-v3.sql` 会在启动时直接报错（宁可报错，也不静默跳步）。
- 升级脚本里的表/列定义必须与同版本完整 schema 一致。
- 完整性哈希（`_integrity`）在应用启动时校验：不一致只打印警告并自动重算，不阻塞启动。写入后不会逐条重算哈希——空闲 1.5s 合并算一次，退出前落盘；被强杀可能留下过期哈希，下次启动打印一次警告即可自愈。

## 排错与恢复

| 现象                                                    | 处理                                                                            |
| ------------------------------------------------------- | ------------------------------------------------------------------------------- |
| 启动报 `[DB] Missing user migration script for vN`      | 补 `migrations-vN.sql` 并在 `USER_MIGRATIONS` 登记                              |
| 启动提示「客户端版本过低」                              | 库由更高版本应用创建；弹窗显示数据版本与客户端支持版本，确认后退出应用，换新版应用打开 |
| 启动日志 `Integrity check FAILED — hash mismatch`       | 上次强杀未落盘或库被外部改动；确认无误后重启一次会自动重算                      |
| 需要回滚某次升级                                        | 还原备份的 `*.db`（`-wal` / `-shm` 一起还原或一起删除），并回退到对应版本的应用 |
| 想查看系统级配置                                        | 已从 `system.db` 迁到 `userdata/config.toml`（TOML，明文，本工具不处理）        |

文件命名与完整 schema 的编写约定见 [`../src/main/database/sql/README.md`](../src/main/database/sql/README.md)，数据层整体说明见仓库根目录 `CLAUDE.md`。
