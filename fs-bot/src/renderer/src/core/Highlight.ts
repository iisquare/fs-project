/**
 * 代码块语法着色：highlight.js 的常用语言子集（按需注册，不把整本语法表打进来）。
 * 配色沿用 fs-admin（Vditor 内置）那一套 github 主题，样式表在 main.ts 里引入
 * 的 highlight.js/styles/github.css，两边的代码块颜色一致。
 */
import type { LanguageFn } from 'highlight.js'
import hljs from 'highlight.js/lib/core'
import bash from 'highlight.js/lib/languages/bash'
import c from 'highlight.js/lib/languages/c'
import cpp from 'highlight.js/lib/languages/cpp'
import csharp from 'highlight.js/lib/languages/csharp'
import css from 'highlight.js/lib/languages/css'
import diff from 'highlight.js/lib/languages/diff'
import dockerfile from 'highlight.js/lib/languages/dockerfile'
import go from 'highlight.js/lib/languages/go'
import graphql from 'highlight.js/lib/languages/graphql'
import ini from 'highlight.js/lib/languages/ini'
import java from 'highlight.js/lib/languages/java'
import javascript from 'highlight.js/lib/languages/javascript'
import json from 'highlight.js/lib/languages/json'
import kotlin from 'highlight.js/lib/languages/kotlin'
import markdown from 'highlight.js/lib/languages/markdown'
import nginx from 'highlight.js/lib/languages/nginx'
import php from 'highlight.js/lib/languages/php'
import plaintext from 'highlight.js/lib/languages/plaintext'
import powershell from 'highlight.js/lib/languages/powershell'
import properties from 'highlight.js/lib/languages/properties'
import python from 'highlight.js/lib/languages/python'
import ruby from 'highlight.js/lib/languages/ruby'
import rust from 'highlight.js/lib/languages/rust'
import scss from 'highlight.js/lib/languages/scss'
import shell from 'highlight.js/lib/languages/shell'
import sql from 'highlight.js/lib/languages/sql'
import swift from 'highlight.js/lib/languages/swift'
import typescript from 'highlight.js/lib/languages/typescript'
import xml from 'highlight.js/lib/languages/xml'
import yaml from 'highlight.js/lib/languages/yaml'

const GRAMMARS: Array<[string, LanguageFn]> = [
  ['bash', bash],
  ['c', c],
  ['cpp', cpp],
  ['csharp', csharp],
  ['css', css],
  ['diff', diff],
  ['dockerfile', dockerfile],
  ['go', go],
  ['graphql', graphql],
  ['ini', ini],
  ['java', java],
  ['javascript', javascript],
  ['json', json],
  ['kotlin', kotlin],
  ['markdown', markdown],
  ['nginx', nginx],
  ['php', php],
  ['plaintext', plaintext],
  ['powershell', powershell],
  ['properties', properties],
  ['python', python],
  ['ruby', ruby],
  ['rust', rust],
  ['scss', scss],
  ['shell', shell],
  ['sql', sql],
  ['swift', swift],
  ['typescript', typescript],
  ['xml', xml],
  ['yaml', yaml]
]

GRAMMARS.forEach(([name, grammar]) => hljs.registerLanguage(name, grammar))

/**
 * 围栏里常见的写法归一：hljs 自带一批别名（js / ts / sh / yml / html / c++ / cs …），
 * 这里只补它没覆盖的那几个。
 */
const ALIASES: Record<string, string> = {
  vue: 'xml',
  env: 'ini',
  toml: 'ini',
  conf: 'ini',
  json5: 'json',
  zsh: 'bash',
  fish: 'bash',
  docker: 'dockerfile'
}

/** 围栏信息串取语言名：`js title=foo`、`ts:路径`、`{sql}` 都只算前一段 */
function fenceLanguage(lang: string): string {
  const name = String(lang ?? '')
    .trim()
    .toLowerCase()
    .replace(/^\{|\}$/g, '')
    .split(/[\s:,]+/, 1)[0]
  return ALIASES[name] ?? name
}

/**
 * 把代码转成可插入 DOM 的高亮 HTML（正文由 hljs 自己转义）。
 * 语言没写或认不出来就在已注册的语法里自动识别；识别不了就按纯文本转义。
 */
export function highlightCode(code: string, lang: string): string {
  const value = String(code ?? '')
  if (!value) return ''
  const name = fenceLanguage(lang)
  try {
    if (name && hljs.getLanguage(name)) {
      return hljs.highlight(value, { language: name, ignoreIllegals: true }).value
    }
    const auto = hljs.highlightAuto(value)
    return auto.relevance > 0 ? auto.value : hljs.highlight(value, { language: 'plaintext' }).value
  } catch {
    return hljs.highlight(value, { language: 'plaintext' }).value
  }
}
