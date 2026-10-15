import { existsSync, readdirSync } from 'fs'
import { resolve, basename, join } from 'path'
import { fileURLToPath } from 'url'
import { createHash } from 'crypto'
import dotenv from 'dotenv'
import Database from 'better-sqlite3-multiple-ciphers'

const mode = process.env.NODE_ENV === 'production' ? 'production' : 'development'
dotenv.config({ path: join(process.cwd(), '.env'), quiet: true })
dotenv.config({ path: join(process.cwd(), `.env.${mode}`), quiet: true })

// ── Key derivation (must match src/main/database/encryption.ts) ──

function deriveKey(ident, dbSecret, purpose = '') {
  return createHash('sha256').update(ident + ':' + dbSecret + ':' + purpose).digest()
}

function derivePassphrase(ident, dbSecret) {
  return deriveKey(ident, dbSecret, '').toString('hex')
}

function deriveHmacKey(ident, dbSecret) {
  return deriveKey(ident, dbSecret, 'hmac')
}

// ── CLI ──────────────────────────────────────────────────────────

function printHelp() {
  console.log(`Usage: pnpm run schema <command> [options]

Commands:
  key                    Print derived encryption keys (passphrase + HMAC)
  version <db-path>      Show schema version, tables and integrity hash of a database
  export <db-path>       Dump schema from a database and output as TypeScript

Options:
  --passphrase <hex>     Hex-encoded encryption passphrase (for SQLCipher)
  --db-secret <secret>   Derive passphrase from secret + identity
  --ident <ident>        Identity for key derivation (use with --db-secret)
  -h, --help             Show this help

Description:
  The 'key' command derives the SQLCipher passphrase and HMAC key from a
  database secret and identity string. These keys are used to encrypt/decrypt
  SQLite databases with SQLCipher.

  The 'export' command reads a database, extracts the schema (CREATE TABLE
  and CREATE INDEX statements from sqlite_master), and outputs them as a
  TypeScript module — a constant array of SQL strings plus a getter function.

  The 'version' command reports the schema version recorded in a user
  database, the version the current code expects (from
  src/main/database/sql/schema-vN.sql), the tables it holds and the integrity
  hash stored by the app. See scripts/README.md for the operation and upgrade
  runbook.

  If --db-secret is omitted, the script falls back to the VITE_DB_SECRET
  environment variable (loaded from .env / .env.development / .env.production).

Examples:
  # Print keys for a specific account
  pnpm run schema key --ident admin --db-secret mysecret

  # Inspect a user database (before/after an upgrade)
  pnpm run schema version userdata/1/fs-bot.db --ident admin

  # Export user data table schemas
  pnpm run schema export userdata/1/fs-bot.db --ident admin
`)
  process.exit(0)
}

function parseArgs(argv) {
  const opts = {
    command: '',
    passphrase: '',
    dbSecret: '',
    ident: ''
  }
  const positional = []

  for (let i = 0; i < argv.length; i++) {
    if (argv[i] === '--help' || argv[i] === '-h') printHelp()
    else if (argv[i] === '--passphrase' && i + 1 < argv.length) opts.passphrase = argv[++i]
    else if (argv[i] === '--db-secret' && i + 1 < argv.length) opts.dbSecret = argv[++i]
    else if (argv[i] === '--ident' && i + 1 < argv.length) opts.ident = argv[++i]
    else if (!argv[i].startsWith('--') && !argv[i].startsWith('-')) positional.push(argv[i])
  }

  if (positional[0] === 'export' || positional[0] === 'key' || positional[0] === 'version') {
    opts.command = positional[0]
    positional.shift()
  }

  return { opts, positional }
}

// ── DB helpers ───────────────────────────────────────────────────

function getPassphrase(opts) {
  if (opts.passphrase) return opts.passphrase
  if (opts.dbSecret && opts.ident) {
    return derivePassphrase(opts.ident, opts.dbSecret)
  }
  return null
}

function openDb(dbPath, passphrase) {
  const absPath = resolve(dbPath)
  if (!existsSync(absPath)) {
    console.error(`Error: Database not found: ${absPath}`)
    process.exit(1)
  }

  if (passphrase) {
    const db = new Database(absPath)
    db.pragma("cipher='sqlcipher'")
    db.pragma('legacy=4')
    db.pragma(`key='${passphrase}'`)
    try {
      db.prepare("SELECT COUNT(*) FROM sqlite_master").get()
    } catch (e) {
    console.error('Error: Failed to open encrypted database - wrong passphrase or corrupted file')
      console.error(e.message)
      process.exit(1)
    }
    return db
  }

  return new Database(absPath)
}

function dumpSchema(db) {
  const stmts = []

  const tableRows = db.prepare(
    "SELECT sql FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' AND sql IS NOT NULL ORDER BY name"
  ).all()

  for (const row of tableRows) {
    stmts.push(row.sql)
  }

  const indexRows = db.prepare(
    "SELECT sql FROM sqlite_master WHERE type='index' AND sql IS NOT NULL AND name NOT LIKE 'sqlite_%' ORDER BY name"
  ).all()

  for (const row of indexRows) {
    stmts.push(row.sql)
  }

  return stmts
}

// ── Schema export ────────────────────────────────────────────────

function formatAsTsType(statement) {
  const escaped = statement.replace(/`/g, '\\`')
  return '  `' + escaped + '`'
}

function exportSchema(dbPath, passphrase) {
  const db = openDb(dbPath, passphrase)
  const stmts = dumpSchema(db)
  db.close()

  const dbName = basename(dbPath, '.db')
  const varName = dbName.toUpperCase().replace(/[^A-Z0-9]+/g, '_') + '_TABLES'
  const lines = []

  lines.push(`const ${varName} = [`)
  for (const stmt of stmts) {
    lines.push(formatAsTsType(stmt) + ',')
  }
  lines.push(']')

  console.log(lines.join('\n'))
}

// ── Key command ───────────────────────────────────────────────────

function cmdKey(ident, dbSecret) {
  const passphrase = derivePassphrase(ident, dbSecret)
  const hmacKey = deriveHmacKey(ident, dbSecret)

  console.log(`Identity:   ${ident}`)
  console.log(`DB Secret:  ${dbSecret}`)
  console.log('-- Keys (hex) --')
  console.log(`Passphrase: ${passphrase}`)
  console.log(`HMAC Key:   ${hmacKey.toString('hex')}`)
  console.log('')
  console.log('-- SQLCipher pragma --')
  console.log(`PRAGMA key = "x'${passphrase}'";`)
}

// ── Version command ───────────────────────────────────────────────
//
// 用户库的 schema 版本由 src/main/database/sql/schema-vN.sql（完整 schema）与
// migrations-vN.sql（升级脚本）共同定义，这里只做只读检查，不改动数据库。

const SQL_DIR = fileURLToPath(new URL('../src/main/database/sql/', import.meta.url))

function sqlFileNames() {
  try {
    return readdirSync(SQL_DIR)
  } catch {
    return []
  }
}

/** 当前代码期望的版本：取 schema-vN.sql 中最大的 N */
function expectedSchemaVersion() {
  const versions = sqlFileNames()
    .map((name) => /^schema-v(\d+)\.sql$/.exec(name))
    .filter(Boolean)
    .map((match) => Number(match[1]))
  return versions.length > 0 ? Math.max(...versions) : 0
}

/** 库内版本落后时需要执行的升级脚本 */
function pendingMigrations(version) {
  if (version === null) return []
  return sqlFileNames()
    .map((name) => /^migrations-v(\d+)\.sql$/.exec(name))
    .filter(Boolean)
    .map((match) => ({ version: Number(match[1]), name: match[0] }))
    .filter((item) => item.version > version)
    .sort((a, b) => a.version - b.version)
    .map((item) => item.name)
}

function readSchemaVersion(db) {
  try {
    const row = db.prepare('SELECT version FROM schema_version ORDER BY version DESC LIMIT 1').get()
    return row ? row.version : 0
  } catch {
    return null // 早于版本管理的库，没有 schema_version 表
  }
}

function cmdVersion(dbPath, passphrase) {
  const db = openDb(dbPath, passphrase)
  const version = readSchemaVersion(db)
  const tables = db
    .prepare(
      "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' ORDER BY name"
    )
    .all()
    .map((row) => row.name)
  let integrity = null
  try {
    integrity = db.prepare('SELECT hash, updated_at FROM _integrity WHERE id = 1').get() ?? null
  } catch {
    integrity = null
  }
  db.close()

  const expected = expectedSchemaVersion()
  const pending = pendingMigrations(version)

  console.log(`Database:        ${resolve(dbPath)}`)
  console.log(
    `Schema version:  ${version === null ? '(schema_version table not found)' : `v${version}`}`
  )
  console.log(`Repo expects:    v${expected}  (src/main/database/sql/schema-v*.sql)`)
  console.log(`Tables:          ${tables.join(', ')}`)
  console.log(
    `Integrity hash:  ${
      integrity ? `${integrity.hash}  (updated_at: ${integrity.updated_at})` : '(缺失)'
    }`
  )

  // 终端输出保持 ASCII，避免 Windows GBK 控制台出现乱码
  if (version === null) {
    console.log('Note: this database predates schema versioning; the app will apply the latest schema on start.')
  } else if (version < expected) {
    console.log(
      `Note: database is behind; the app will run: ${
        pending.length ? pending.join(', ') : '(missing migration script - startup will fail)'
      }`
    )
  } else if (version > expected) {
    console.log('Note: database is newer than this code; the app refuses to downgrade.')
  } else {
    console.log('Note: versions match; no migration will run.')
  }
  console.log('Note: the integrity hash is verified by the app at startup; this shows the stored value.')
}

// ── Main ─────────────────────────────────────────────────────────

const { opts, positional } = parseArgs(process.argv.slice(2))

if (!opts.command) {
  console.error('Error: Unknown command. Use --help for usage.')
  process.exit(1)
}

// Resolve dbSecret from env
if (!opts.dbSecret && process.env['VITE_DB_SECRET']) {
  opts.dbSecret = process.env['VITE_DB_SECRET']
}

// Key command — no database needed
if (opts.command === 'key') {
  if (!opts.dbSecret) {
    console.error('Error: --db-secret is required (or set VITE_DB_SECRET env var)')
    process.exit(1)
  }
  if (!opts.ident) {
    console.error('Error: --ident is required')
    process.exit(1)
  }
  cmdKey(opts.ident, opts.dbSecret)
  process.exit(0)
}

// Version command
if (opts.command === 'version') {
  if (positional.length < 1) {
    console.error('Usage: pnpm run schema version <db-path> [options]')
    process.exit(1)
  }

  const passphrase = getPassphrase(opts)
  if (!passphrase) {
    console.error('Warning: No passphrase specified. If the database is encrypted, this will fail.')
    console.error('Use --passphrase <hex>, or --db-secret + --ident.')
  }

  cmdVersion(resolve(positional[0]), passphrase)
  process.exit(0)
}

// Export command
if (opts.command === 'export') {
  if (positional.length < 1) {
    console.error('Usage: pnpm run schema export <db-path> [options]')
    process.exit(1)
  }

  const passphrase = getPassphrase(opts)
  if (!passphrase) {
    console.error(
      'Warning: No passphrase specified. If the database is encrypted, this will fail.'
    )
    console.error(
      'Use --passphrase <hex>, or --db-secret + --ident.'
    )
  }

  exportSchema(resolve(positional[0]), passphrase)
  process.exit(0)
}
