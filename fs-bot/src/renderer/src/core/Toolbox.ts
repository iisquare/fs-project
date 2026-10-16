import ApiUtil from '@renderer/utils/ApiUtil'
import BotApi from '@renderer/api/agent/BotApi'
import ConversationDb, { type Conversation, type HistoryHit } from '@renderer/db/ConversationDb'
import type { Project } from '@renderer/db/ProjectDb'
import type { ScheduleTask } from '@renderer/db/TaskDb'
import { isValidCron, nextRun } from '@renderer/utils/CronUtil'
import { missing, textArg } from '@renderer/utils/ArgUtil'
import { applyReplace, applyWrite, type MemoryEdit } from '@renderer/utils/MemoryUtil'
import { useProjectStore } from '@renderer/stores/project'
import { useScheduleStore } from '@renderer/stores/schedule'
import { useUserStore } from '@renderer/stores/user'

/**
 * 工具层：把「模型能调用的能力」归一成一份 function 定义清单，并提供按函数名的执行入口。
 *
 * - 本地工具一共五个：`file_op`（文件与目录：读 / 写 / 列 / 建目录 / 移动 / 删除 / 检索 / 上传）、
 *   `run_command`（本机命令）、`schedule_task`（定时任务）、`memory`（历史对话 + 长期记忆）
 *   与 `local_skill`（本地技能）。文件与命令由主进程按「项目声明的目录权限」与
 *   「工作区外访问策略」放行，渲染进程改不了结论，越界的读取、写入与命令都会作为失败结果回到模型；
 *   定时任务与历史对话走本地库，技能读本地技能目录。
 * - 项目工具：工具方法 / 知识库 / 数据主题 / 知识图谱本体 / 编排应用，凭据与真实调用都在服务端。
 *
 * 「同一实体的一族操作」都收在一个工具里、用 action 参数区分（file_op / schedule_task /
 * memory / local_skill）：定义每轮请求都要带上，一个实体拆成五个 function
 * 光骨架就白占几百 token。代价是 JSON Schema 表达不了「哪个 action 要哪些参数」，
 * 所以 schema 里只把 action 标成必填（带 enum），条件必填与取值校验放到执行期
 * （见 actionOf 与各 invoke 函数），错误信息照旧作为调用失败回到模型、由模型补齐后重试。
 *
 * 调用循环由客户端驱动（每轮请求都带上完整的工具调用链），所以定义、执行与函数名的对应关系都在这里，
 * 与服务端只约定一件事：`/bot/tools` 给定义、`/bot/tool` 执行一次。
 */

export type ToolScope = 'local' | 'server'

export interface ToolInfo {
  name: string
  description: string
  scope: ToolScope
  /** 展示用短标签：本地工具即函数名，项目工具按类型给出中文名 */
  label: string
}

export interface ToolSet {
  /** 模型可见的 function 定义清单 */
  definitions: any[]
  /** 函数名 → 展示信息 */
  catalog: Record<string, ToolInfo>
  /**
   * 执行一次调用：本地工具走主进程，服务端工具走 /bot/tool。
   * conversationId 是发起这次调用（这次对话）的标识：本地工具需要审批时，申请据此交回那段对话。
   */
  invoke(name: string, args: any, projectId: string, conversationId: string): Promise<string>
}

/** 项目工具的展示名：与服务端编排里大语言模型节点的工具种类一致 */
const SERVER_KINDS: Record<string, string> = {
  method: '工具方法',
  knowledge: '知识库',
  theme: '数据主题',
  ontology: '知识图谱本体',
  agentic: '编排应用'
}

/** 本地工具的一行：function 定义 + 展示信息（本地工具的展示名就是函数名） */
function row(
  name: string,
  description: string,
  properties: any,
  required: string[] = []
): { tool: any; info: ToolInfo } {
  const tool = {
    type: 'function',
    function: {
      name,
      description,
      parameters: { type: 'object', properties, ...(required.length ? { required } : {}) }
    }
  }
  return { tool, info: { name, description, scope: 'local' as ToolScope, label: name } }
}

const text = (description: string) => ({ type: 'string', description })

/**
 * 本机文件与命令：文件与目录的那一族操作合成 file_op（用 action 区分），
 * 执行命令单列 run_command —— 命令的作用范围不限于工作目录、每次都要过访问策略，
 * 与「落在授权目录内就放行」的文件操作不是一套口径。
 */
function fileTools(): Array<{ tool: any; info: ToolInfo }> {
  return [
    row(
      'file_op',
      '对本地文件与目录做事：路径可以是绝对路径，也可以是工作区目录下的相对路径' +
        '（相对路径按主目录优先的顺序解析）。用 action 选操作：' +
        'read = 读文本文件（需要 path，offset / limit 选行范围，默认读前 2000 行）；' +
        'write = 写文件，覆盖原内容、父目录不存在会自动创建（需要 path 与 content，content 给空串即清空文件）；' +
        'replace = 改文件里的一小段（需要 path、find、replace）：把 find 换成新文，其余内容与行尾、' +
        '编码都原样保留——find 与替换内容都可以带换行与特殊字符（按字面量匹配，正则元字符不当语法用）；' +
        'start_line / end_line 限定替换范围（1 起、闭区间，省略即整个文件），' +
        '给 all = true 才是把范围内全部命中都替换，默认要求范围内恰好一处命中，' +
        '0 处或多处都失败并把位置回报（免得改错地方）；只处理 UTF-8 文本，二进制与非 UTF-8 文件会被拒绝；' +
        'find 与原文的空白不一致时会按空白分段命中，这时把两段之间的空白（含缩进）也一起替换掉——' +
        '替换内容要自带缩进，返回里会用 \\n / \\t 把替掉的原文显示出来，照它核对；' +
        'list = 列出目录下的文件与子目录（需要 path，用来了解工作区结构）；' +
        'mkdir = 创建目录（需要 path，已存在不报错）；' +
        'move = 移动或重命名文件 / 目录（需要 from 与 to）；' +
        'delete = 删除文件或目录（需要 path，目录递归删除、不可恢复）；' +
        'search = 在工作区内按关键词做全文检索（需要 keyword，返回命中的文件、行号与所在行）；' +
        '关键词按空白切成几段、各段按原顺序出现即算命中：整段粘多行片段也可以（换行、缩进与' +
        '\\r\\n / \\n 的差别都不影响，跨行的命中给出行号范围），单个词就是按行匹配；' +
        '关键词按字面量匹配，正则元字符（. * ( ) [ ] 这些）都按原字符查；' +
        'upload = 把本机文件上传到文件服务（图床，需要 path），返回文件标识（id）与名称、类型、大小——' +
        '要让服务端的工具方法分析本机文件时（如图片 / 视频 / 音频交给 multimodalAnalyze），' +
        '先 upload 拿到 id，再把 id 交给那个工具方法（files 里给 { id }）；' +
        '服务端读不到本机磁盘，直接把本机路径传给服务端工具方法会被拒绝。' +
        '所有操作都由主进程按项目声明的目录权限放行，工作区之外按本地访问策略申请或拒绝。',
      {
        action: {
          type: 'string',
          enum: ['read', 'write', 'replace', 'list', 'mkdir', 'move', 'delete', 'search', 'upload'],
          description: '要做的操作'
        },
        path: text(
          '文件或目录路径（read / write / replace / list / mkdir / delete / upload 需要）'
        ),
        from: text('原路径（move 需要）'),
        to: text('新路径（move 需要）'),
        content: text('要写入的完整内容（write 需要；空串 = 清空文件）'),
        find: text('要被替换掉的原文（replace 需要；可含换行与特殊字符，按字面量匹配）'),
        replace: text('替换成的内容（replace 需要；可含换行，给空串就是删掉 find 那一段）'),
        start_line: {
          type: 'integer',
          description: '替换范围的起始行（replace 选填，从 1 开始；省略即从文件开头）'
        },
        end_line: {
          type: 'integer',
          description: '替换范围的结束行（replace 选填，含这一行；省略即到文件末尾）'
        },
        all: {
          type: 'boolean',
          description: '替换范围内全部命中（replace 选填；默认 false = 要求范围内恰好一处命中）'
        },
        keyword: text('检索关键词（search 需要）'),
        offset: { type: 'integer', description: 'read 的起始行号（从 1 开始，选填）' },
        limit: { type: 'integer', description: 'read 最多返回多少行（选填，默认 2000）' }
      },
      ['action']
    ),
    row(
      'run_command',
      '在本机执行一条命令（系统 shell），返回退出码与输出。工作目录默认取项目的主目录，' +
        '每次执行都要过一遍本地访问策略——命令的作用范围不限于工作目录，' +
        '不能用它绕过工作区外的文件访问限制，未获批准时这一步会失败；' +
        '适合跑构建、测试、查看仓库状态等一次性命令，' +
        '不要跑需要交互或长时间驻留的命令（如 dev server）。',
      {
        command: text('要执行的命令，例如 pnpm install、git status、dir'),
        cwd: text('工作目录（选填，默认取项目的主目录）'),
        timeout: { type: 'integer', description: '超时秒数（选填，默认 60，最长 600）' }
      },
      ['command']
    )
  ]
}

/**
 * 定时任务：与「定时任务」页面共用同一份本地数据（都走 schedule store），
 * 表达式一律先按页面那套口径校验（5 段：分 时 日 月 周）再落库。
 *
 * 一组同实体的操作合成一个工具、由 action 区分：schema 表达不了「哪个 action 要哪些参数」，
 * 所以条件必填放到执行期校验（见 invokeSchedule），缺参数时的提示会点名该补什么。
 */
function scheduleTools(): Array<{ tool: any; info: ToolInfo }> {
  return [
    row(
      'schedule_task',
      '管理本机的定时任务（到点按 cron 在本机自动发起一轮对话，用任务绑定的项目）。用 action 选操作：' +
        'list = 列出全部任务（名称 / id / 项目 / cron / 下次执行 / 启用状态 / 最近一次结果）；' +
        'create = 新建，需要 name、cron、prompt；' +
        'update = 修改，需要 id，其余字段给哪个改哪个（含用 enabled 停用 / 启用）；' +
        'delete = 删除，需要 id（只删任务本身，它触发出来的会话不受影响）；' +
        'run = 立刻跑一次，需要 id（会真的发起一轮后台对话、花掉一次模型调用，只用来验证配置能不能用）。' +
        '改、删、跑之前先用 list 拿 id。cron 是 5 段「分 时 日 月 周」（0 9 * * * = 每天 9 点），日与周同时写具体值时按「或」判断；' +
        'prompt 是触发时发给模型的完整指令，要写清楚要做的事，因为它不带这一轮的上下文；' +
        'project_id 省略时用当前对话所属的项目。定时任务在客户端运行：应用保持登录且进程在跑才会触发。',
      {
        action: {
          type: 'string',
          enum: ['list', 'create', 'update', 'delete', 'run'],
          description: '要做的操作'
        },
        id: text('任务标识（update / delete / run 需要，list 给出）'),
        name: text('任务名称（create 需要，例如「每日简报」；update 选填）'),
        cron: text('cron 表达式（create 需要；update 选填）'),
        prompt: text('触发时发送的指令全文（create 需要；update 选填）'),
        project_id: text('项目标识（选填，省略时用当前对话所属的项目）'),
        enabled: { type: 'boolean', description: '是否启用（选填）' }
      },
      ['action']
    )
  ]
}

/**
 * 记忆（action 分派：list / search / read / write / replace）：
 *
 * list / search / read 翻的是本机落库的历史对话，`list` 与 `search` 默认收在当前对话所属的项目里
 * （scope 给 all 才跨项目），这样模型翻旧账时不会把别的项目的对话顺手拖进来。
 *
 * write 与 replace 改的是当前项目的「长期记忆」（`projects.memory`，每轮随请求下发的那一格）：
 * write 整段写（append / replace，空串配 replace 即清空），replace 只改其中一段；
 * 与历史对话不是同一份数据，但都是「记忆」，收在同一个工具里模型更容易挑对。
 * 编辑口径（清空、唯一性、字面量匹配）都在 utils/MemoryUtil 里算，这里只管落库与回执。
 */
function memoryTools(): Array<{ tool: any; info: ToolInfo }> {
  return [
    row(
      'memory',
      '本机的记忆：过去那些对话都留在本机，用这个工具翻；长期有用的结论也可以写进当前项目的记忆。用 action 选操作：' +
        'list = 按时间倒序列出最近的会话（标题、时间与 id，用来先看看有哪些对话）；' +
        'search = 按关键词查会话标题与消息正文，返回命中的会话与消息片段（需要 keyword；中文按字面匹配，英文不区分大小写；' +
        '整段粘多行片段也可以——整段对不上时按空白切成几段、各段按原顺序出现即算命中，换行与缩进对不上不影响）；' +
        'read = 按会话 id 读某个会话的消息正文（需要 conversation_id；按时间正序、带角色与时间，一次读一段，' +
        '返回里会说明还剩多少条；命中里给的「第 n 条」可以直接当 offset 用——offset 给 n - 1 就从那条附近读起）。' +
        'list 与 search 默认只覆盖当前对话所属的项目，跨项目找旧事把 scope 给 all；' +
        '命中后用 read 回看上下文；当前这一轮的对话已经在上下文里，不必再检索。' +
        'write = 把长期有用的事写进当前项目的长期记忆（需要 content，mode 选 append（默认，追加）或 replace（整段替换）；' +
        'content 给空串且 mode=replace 就是清空记忆，content 没给（缺参数）才会报错），' +
        '写进去的内容从下一轮起随每轮请求下发、本项目的所有会话共用：' +
        '只写长期成立的事实、用户偏好与已定的约定（一行一条最好），别把一次性的任务或整段对话抄进去；' +
        '写之前先用 read / search 看看上下文里有没有旧的判断，重复或过时的条目用 replace 顺手整理掉。' +
        'replace = 只改当前项目记忆里的一段，不动其余内容：把 find 换成 content（需要 find 与 content；' +
        'content 给空串就是删掉那一段）——两者都可以带换行与特殊字符，按字面量匹配；' +
        '默认要求记忆里恰好一处命中，命中多处而确实要全换才给 all。' +
        '记忆每轮都随请求下发、占上下文，所以它要保持精简：过时或重复的条目该删就删，' +
        '要整体重写就用 write 的 mode=replace，要连同内容一起清掉就用 content 空串。',
      {
        action: {
          type: 'string',
          enum: ['list', 'search', 'read', 'write', 'replace'],
          description: '要做的操作'
        },
        keyword: text('检索关键词（search 需要）'),
        conversation_id: text('会话标识（read 需要，list / search 给出）'),
        scope: {
          type: 'string',
          enum: ['project', 'all'],
          description: '覆盖范围（选填：project = 当前项目，默认；all = 全部项目）'
        },
        offset: { type: 'integer', description: 'read 从第几条开始读（从 0 开始，选填，默认 0）' },
        limit: {
          type: 'integer',
          description: '条数上限（选填：list / search 默认 10、最多 30；read 默认 20、最多 100）'
        },
        content: text(
          '要写进长期记忆的内容（write / replace 需要，可以是一行一条的多行文本；' +
            '配 mode=replace 时给空串即清空记忆，replace 动作里给空串即删掉 find 那一段）'
        ),
        mode: {
          type: 'string',
          enum: ['append', 'replace'],
          description: 'write 的写法（选填：append = 追加到已有记忆后面，默认；replace = 整段替换）'
        },
        find: text('记忆里要被替换掉的原文（replace 需要；按字面量匹配，可含换行与特殊字符）'),
        all: {
          type: 'boolean',
          description: 'replace 选填：true = 记忆里全部命中都替换（默认 false = 要求恰好一处命中）'
        }
      },
      ['action']
    )
  ]
}

/**
 * 本地技能（action 分派：list / files / read）：
 * 技能包是「按说明做事」的能力包（SKILL.md 是主入口，脚本与参考文档在同一个目录里）。
 * 清单与文件内容都由主进程读技能目录——相对路径必须在技能目录内，越界的读取直接拒绝。
 */
function skillTools(): Array<{ tool: any; info: ToolInfo }> {
  return [
    row(
      'local_skill',
      '用本机已安装的技能（服务端技能包解压到本地 skills 目录的那份）。用 action 选操作：' +
        'list = 列出已安装技能（名称、版本、文件数与大小、安装时间）；' +
        'files = 列出某个技能包里的全部文件（需要 name；给相对路径与大小，说明、脚本、参考文档都在其中）；' +
        'read = 读技能包里的文件（需要 name；path 省略时读技能说明 SKILL.md——技能的主入口，先读它再按它的指示做事，' +
        '脚本与参考文档按 files 给的相对路径读）。技能是「按说明做事」的能力包，里面的脚本要跑就用 run_command' +
        '（技能目录通常在工作区之外，会照本地访问策略申请）。',
      {
        action: { type: 'string', enum: ['list', 'files', 'read'], description: '要做的操作' },
        name: text('技能名称（files / read 需要，list 给出）'),
        path: text('技能包内的相对路径（read 选填，省略时读 SKILL.md）')
      },
      ['action']
    )
  ]
}

/** 本地工具清单：文件与命令、定时任务、记忆（历史对话 + 长期记忆）、本地技能 */
function localTools(): Array<{ tool: any; info: ToolInfo }> {
  return [...fileTools(), ...scheduleTools(), ...memoryTools(), ...skillTools()]
}

/**
 * 本地工具的执行：文件与命令一律经主进程，权限与访问策略都在那边判定，
 * 失败直接把原因作为工具返回交给模型（模型可据此改路径或先向用户说明）。
 * 定时任务、历史对话与本地技能走本地库 / 技能目录，不经服务端。
 */
async function invokeLocal(
  name: string,
  args: any,
  projectId: string,
  conversationId: string
): Promise<string> {
  if ('file_op' === name) return invokeFile(args, projectId, conversationId)
  if ('run_command' === name) return invokeCommand(args, projectId, conversationId)
  if ('schedule_task' === name) return invokeSchedule(args, projectId)
  if ('memory' === name) return invokeMemory(args, projectId, conversationId)
  if ('local_skill' === name) return invokeSkill(args)
  throw new Error(`未知的本地工具：${name}`)
}

/**
 * 文件与目录（action 分派）：每个操作都由主进程按项目声明的目录权限放行，
 * 工作区之外按访问策略申请或拒绝——渲染进程改不了结论，失败原因照旧回到模型。
 */
async function invokeFile(args: any, projectId: string, conversationId: string): Promise<string> {
  const action = actionOf(args, FILE_ACTIONS)
  /** 这个 action 必填的字符串参数：缺了就说清该补什么（schema 里只有 action 是必填） */
  const need = (key: string, hint: string): string => {
    const value = String(args?.[key] ?? '').trim()
    if (!value) throw new Error(`${action} 需要 ${key}（${hint}）`)
    return value
  }

  if ('read' === action) {
    const target = need('path', '文件路径')
    const result = await window.ipc.workspace.read(projectId, target, conversationId)
    const lines = String(result.content ?? '').split(/\r?\n/)
    const offset = Math.max(1, Number(args?.offset ?? 1) || 1)
    const limit = Math.max(1, Number(args?.limit ?? 2000) || 2000)
    if (offset > lines.length) return `（文件共 ${lines.length} 行，起始行超出范围：${target}）`
    const slice = lines.slice(offset - 1, offset - 1 + limit)
    const body = slice.map((line, index) => `${offset + index}|${line}`).join('\n')
    const more =
      offset - 1 + slice.length < lines.length ? '\n（已截断，可加大 offset 继续读取）' : ''
    const head = offset > 1 ? `（第 ${offset} 行起，文件共 ${lines.length} 行）\n` : ''
    return `${head}${body}${more}`
  }
  if ('write' === action) {
    const target = need('path', '文件路径')
    // content 允许空串（= 清空文件），所以只认「没给」（含 null）这一种缺参数
    if (missing(args?.content)) {
      throw new Error('write 需要 content（要写入的完整内容；要清空文件就给空串）')
    }
    const result = await window.ipc.workspace.write(
      projectId,
      target,
      textArg(args.content),
      conversationId
    )
    return `已写入 ${result.path}（${result.size} 字节）`
  }
  if ('replace' === action) {
    const target = need('path', '文件路径')
    // find 拿原文、原样匹配：首尾空白也可能是要找的内容（例如删掉一个空行就是 find = "\n\n"），
    // 所以只把「没给」和空串当作缺参数
    if (missing(args?.find) || '' === textArg(args.find)) {
      throw new Error('replace 需要 find（要被替换掉的原文）')
    }
    // replace 允许空串（= 删掉 find 那一段），所以只认「没给」（含 null）这一种缺参数
    if (missing(args?.replace)) {
      throw new Error('replace 需要 replace（替换成的内容；要删掉 find 那一段就给空串）')
    }
    const result = await window.ipc.workspace.replace(
      projectId,
      target,
      textArg(args.find),
      textArg(args.replace),
      {
        ...(undefined === args?.start_line
          ? {}
          : { startLine: Math.max(1, Math.floor(Number(args.start_line) || 1)) }),
        ...(undefined === args?.end_line
          ? {}
          : { endLine: Math.max(1, Math.floor(Number(args.end_line) || 1)) }),
        all: true === args?.all || 'true' === String(args?.all)
      },
      conversationId
    )
    const at =
      result.startLine === result.endLine
        ? `第 ${result.startLine} 行`
        : `第 ${result.startLine}-${result.endLine} 行`
    return [
      `已替换 ${result.path} ${at}的 ${result.count} 处（${
        'segments' === result.match ? '按空白分段匹配（原文的空白与 find 不同）' : '按字面量匹配'
      }）：`,
      `- 原文：${result.before}`,
      `+ 新文：${result.after || '（空，这一段被删掉了）'}`,
      `行尾 ${'crlf' === result.eol ? 'CRLF' : 'LF'}${result.bom ? '、带 BOM' : ''} 原样保留，` +
        `现在 ${result.size} 字节`
    ].join('\n')
  }
  if ('list' === action) {
    const target = need('path', '目录路径')
    const rows = await window.ipc.workspace.list(projectId, target, conversationId)
    if (!rows.length) return `目录为空或不存在：${target}`
    return rows
      .map((entry) =>
        entry.dir ? `[目录] ${entry.name}` : `[文件] ${entry.name}（${entry.size} 字节）`
      )
      .join('\n')
  }
  if ('mkdir' === action) {
    const result = await window.ipc.workspace.mkdir(
      projectId,
      need('path', '目录路径'),
      conversationId
    )
    return `已创建目录 ${result.path}`
  }
  if ('move' === action) {
    const result = await window.ipc.workspace.rename(
      projectId,
      need('from', '原路径'),
      need('to', '新路径'),
      conversationId
    )
    return `已移动为 ${result.path}`
  }
  if ('delete' === action) {
    const result = await window.ipc.workspace.remove(
      projectId,
      need('path', '路径'),
      conversationId
    )
    return `已删除 ${result.path}`
  }
  if ('search' === action) {
    const keyword = need('keyword', '检索关键词')
    const rows = await window.ipc.workspace.search(projectId, keyword)
    if (!rows.length) return `没有命中「${oneLine(keyword, 80)}」`
    return rows
      .map((row) => {
        const at = row.endLine > row.line ? `${row.line}-${row.endLine}` : `${row.line}`
        return `${row.path}:${at}: ${row.text}`
      })
      .join('\n')
  }
  if ('upload' === action) {
    const result = await window.ipc.workspace.upload(
      projectId,
      need('path', '文件路径（绝对路径，或工作区目录下的相对路径）'),
      { url: BotApi.uploadUrl(), token: useUserStore().info.token },
      conversationId
    )
    return [
      `已上传 ${result.name}`,
      `文件标识：${result.id}`,
      `类型：${result.type || '未知'}，大小：${result.size} 字节`,
      '（服务端工具方法引用这个文件时，把文件标识原样放进对应的文件参数即可）'
    ].join('\n')
  }
  throw new Error(`文件操作不支持：${action}`)
}

/** 本机命令：工作目录决定权限口径，越界时主进程推申请、等答复 */
async function invokeCommand(
  args: any,
  projectId: string,
  conversationId: string
): Promise<string> {
  const command = String(args?.command ?? '').trim()
  if (!command) throw new Error('run_command 需要 command（要执行的命令）')
  const timeout = Math.max(0, Number(args?.timeout ?? 0) || 0)
  const result = await window.ipc.workspace.exec(
    projectId,
    command,
    String(args?.cwd ?? ''),
    timeout,
    conversationId
  )
  const text = commandResult(result)
  // 用户按了停止：进程树已被主进程收掉，这一步就此打住（调用循环随后按「已停止」收尾）
  if (result.cancelled) throw new Error(`${text}\n（命令已被停止）`)
  // 非 0 退出码与超时都按失败抛出：模型拿到完整输出继续修正，界面上这一格也是红的
  if (result.timedOut) {
    throw new Error(`${text}\n（命令超过 ${timeout > 0 ? timeout : 60} 秒仍未结束，已终止）`)
  }
  if (null !== result.code && 0 !== result.code) throw new Error(text)
  return text
}

/**
 * 「同实体一组操作」的工具都用一个 action 参数区分操作，这里是与定义里的 enum 对齐的清单
 * （file_op 与下面三组）。执行前一律先核一遍（模型偶尔会漏给或写错 action），
 * 错误信息里带上可选项便于模型自纠。
 */
const FILE_ACTIONS = [
  'read',
  'write',
  'replace',
  'list',
  'mkdir',
  'move',
  'delete',
  'search',
  'upload'
]
const SCHEDULE_ACTIONS = ['list', 'create', 'update', 'delete', 'run']
const MEMORY_ACTIONS = ['list', 'search', 'read', 'write', 'replace']
const SKILL_ACTIONS = ['list', 'files', 'read']
/** 长期记忆的提醒线：写到这个长度就在调用结果里提醒精简（记忆每轮都占上下文，但不硬拦） */
const MEMORY_HINT_SIZE = 8000

function actionOf(args: any, allowed: string[]): string {
  const action = String(args?.action ?? '')
    .trim()
    .toLowerCase()
  if (!allowed.includes(action)) {
    throw new Error(`未知的操作：${action || '（action 为空）'}（可选：${allowed.join(' / ')}）`)
  }
  return action
}

/**
 * 定时任务（action 分派）：走 schedule store，改完页面立刻能看到（列表与表单都读同一份数据）。
 * 各 action 需要的参数在这里补齐校验（schema 里只有一个 action 是必填），
 * 表达式先按页面那套口径校验再落库；参数缺了、表达式错了都把原因作为失败回到模型。
 */
async function invokeSchedule(args: any, projectId: string): Promise<string> {
  const action = actionOf(args, SCHEDULE_ACTIONS)
  const store = useScheduleStore()
  const projects = useProjectStore()
  const tasks = await store.fetchTasks()
  const wanted = String(args?.id ?? '').trim()
  const find = (): ScheduleTask => {
    const task = tasks.find((item) => item.id === wanted)
    if (!task) {
      throw new Error(`找不到定时任务：${wanted || '（id 为空）'}（先用 action=list 看有哪些任务）`)
    }
    return task
  }
  const projectName = (id: string): string => projects.find(id)?.name ?? '（项目已删除）'

  if ('list' === action) {
    if (!tasks.length) return '本机没有定时任务。'
    const lines = [`共 ${tasks.length} 个定时任务：`]
    tasks.forEach((task, index) => {
      const next = task.enabled ? nextRun(task.cron) : null
      const when = next
        ? `，下次执行：${next.toLocaleString('zh-CN')}`
        : task.enabled
          ? '（表达式无效或一年内不触发）'
          : ''
      lines.push(
        `${index + 1}. ${task.name}（${task.enabled ? '启用' : '停用'}）`,
        `   id：${task.id}`,
        `   cron：${task.cron}${when}`,
        `   项目：${projectName(task.projectId)}`,
        `   最近一次：${
          task.lastRunAt
            ? `${task.lastStatus || '已触发'} · ${momentText(task.lastRunAt)}${
                task.lastMessage ? ` · ${task.lastMessage}` : ''
              }`
            : '未执行'
        }`,
        `   提示词：${oneLine(task.prompt, 200) || '（空）'}`
      )
    })
    return lines.join('\n')
  }

  if ('create' === action) {
    const taskName = String(args?.name ?? '').trim()
    if (!taskName) throw new Error('create 需要 name（任务名称）')
    const prompt = String(args?.prompt ?? '').trim()
    if (!prompt) throw new Error('create 需要 prompt（触发时发送的指令全文）')
    const cron = String(args?.cron ?? '').trim()
    if (!isValidCron(cron)) {
      throw new Error(`cron 表达式无效（需要 5 段：分 时 日 月 周）：${cron || '（空）'}`)
    }
    const target = String(args?.project_id ?? '').trim() || projectId
    const project = projects.find(target)
    if (!project) {
      throw new Error(
        target ? `找不到项目：${target}` : '没有指定项目，当前对话也没有所属项目，无法创建定时任务'
      )
    }
    const created: any = await store.create({
      name: taskName,
      projectId: project.id,
      cron,
      prompt,
      enabled: false !== args?.enabled && 'false' !== String(args?.enabled)
    })
    const id = String(ApiUtil.data(created)?.id ?? '')
    const next = nextRun(cron)
    return [
      `已创建定时任务「${taskName}」${id ? `（id：${id}）` : ''}`,
      `cron：${cron}${next ? `，下次执行：${next.toLocaleString('zh-CN')}` : ''}`,
      `项目：${project.name}`,
      `提示词：${oneLine(prompt, 200) || '（空）'}`,
      '（定时任务在客户端运行：应用保持登录且进程在跑才会触发）'
    ].join('\n')
  }

  if ('update' === action) {
    const task = find()
    const patch: Partial<ScheduleTask> = {}
    if (undefined !== args?.name) patch.name = String(args.name).trim() || task.name
    if (undefined !== args?.cron) {
      const cron = String(args.cron).trim()
      if (!isValidCron(cron)) {
        throw new Error(`cron 表达式无效（需要 5 段：分 时 日 月 周）：${cron || '（空）'}`)
      }
      patch.cron = cron
    }
    if (undefined !== args?.prompt) patch.prompt = String(args.prompt)
    const nextProject = String(args?.project_id ?? '').trim()
    if (nextProject) {
      const project = projects.find(nextProject)
      if (!project) throw new Error(`找不到项目：${nextProject}`)
      patch.projectId = project.id
    }
    if (undefined !== args?.enabled) {
      patch.enabled = true === args.enabled || 'true' === String(args.enabled)
    }
    if (!Object.keys(patch).length) {
      throw new Error(
        'update 没有给出要修改的字段（name / cron / prompt / project_id / enabled 至少给一个）'
      )
    }
    await store.update(task.id, patch)
    const updated = store.tasks.find((item) => item.id === task.id) ?? task
    const next = updated.enabled ? nextRun(updated.cron) : null
    return [
      `已更新定时任务「${updated.name}」（id：${updated.id}）`,
      `cron：${updated.cron}${next ? `，下次执行：${next.toLocaleString('zh-CN')}` : ''}，${updated.enabled ? '启用' : '停用'}`,
      `项目：${projectName(updated.projectId)}`,
      `提示词：${oneLine(updated.prompt, 200) || '（空）'}`
    ].join('\n')
  }

  if ('delete' === action) {
    const task = find()
    await store.remove(task.id)
    return `已删除定时任务「${task.name}」（它之前触发出来的会话不受影响）。`
  }

  if ('run' === action) {
    const task = find()
    const ok = await store.run(task)
    if (!ok) {
      throw new Error(
        `「${task.name}」触发失败：它绑定的项目不存在或已删除（可用 action=update 改绑项目）`
      )
    }
    return `已按「${task.name}」发起一轮后台对话（项目：${projectName(task.projectId)}），结果会作为新会话进历史列表。`
  }
  throw new Error(`定时任务不支持的操作：${action}`)
}

/**
 * 记忆（action 分派）：list / search 默认只在当前对话所属的项目里翻，
 * 跨项目找旧事时 scope 给 all。检索走本地库的关键词查询（会话标题 + 消息正文），
 * 回看走会话的消息记录；write 写的是当前项目的长期记忆（`projects.memory`，走项目 store，
 * 与项目编辑器是同一份数据——编辑器打开就是最新内容，本项目的会话从下一轮起随请求下发）。
 */
async function invokeMemory(args: any, projectId: string, conversationId: string): Promise<string> {
  const action = actionOf(args, MEMORY_ACTIONS)
  const scope = String(args?.scope ?? '')
  const explicitAll = 'all' === scope.trim().toLowerCase()
  // 会话没有所属项目（项目已删除、或老数据）时「当前项目」无从谈起：退回全部项目，并在结果里写明
  const all = explicitAll || !projectId
  const scopeText = all
    ? !explicitAll && !projectId
      ? '全部项目（当前对话没有所属项目）'
      : '全部项目'
    : '当前项目'
  const size = (fallback: number, max: number): number =>
    Math.max(1, Math.min(max, Math.floor(Number(args?.limit ?? 0) || fallback)))

  if ('list' === action) {
    const listed: any = await ConversationDb.list(all ? '' : projectId)
    const rows = ApiUtil.succeed(listed) ? (ApiUtil.data(listed) as Conversation[]) : []
    if (!rows.length) return all ? '本机还没有历史对话。' : '当前项目还没有历史对话。'
    const projects = useProjectStore()
    const count = Math.min(size(10, 30), rows.length)
    const lines = [`最近的 ${count} 个会话（范围：${scopeText}，按最后更新排序）：`]
    rows.slice(0, count).forEach((item, index) => {
      const project = projects.find(item.projectId)?.name ?? item.projectId
      lines.push(
        `${index + 1}. 《${item.title || '（无标题）'}》${project ? `（项目：${project}）` : ''}${
          item.id === conversationId ? '（就是当前这段对话）' : ''
        }`,
        `   会话 id：${item.id}，最后更新：${momentText(item.updatedAt)}`
      )
    })
    lines.push('（用 action=search 按关键词检索内容，用 action=read 读某个会话的正文）')
    return lines.join('\n')
  }

  if ('search' === action) {
    const keyword = String(args?.keyword ?? '').trim()
    if (!keyword) throw new Error('search 需要 keyword（检索关键词）')
    // 关键词可能是整段粘来的多行文本：提示文案里折成一行，别把结果撑成好几行
    const shown = oneLine(keyword, 80)
    const count = size(10, 30)
    const result: any = await ConversationDb.search(keyword, all ? '' : projectId, count)
    const hits = ApiUtil.succeed(result) ? (ApiUtil.data(result) as HistoryHit[]) : []
    if (!hits.length) {
      const where = all ? '本机' : '当前项目'
      const hint = all ? '' : '（要跨项目找可以再把 scope 设为 all）'
      return `${where}的历史对话里没有命中「${shown}」${hint}。`
    }
    const projects = useProjectStore()
    const lines = [`命中 ${hits.length} 条（关键词「${shown}」，范围：${scopeText}）：`]
    hits.forEach((hit, index) => {
      const project = projects.find(hit.projectId)?.name ?? hit.projectId
      const at = hit.messageIndex ? ` · 第 ${hit.messageIndex}/${hit.messageTotal} 条` : ''
      lines.push(
        `${index + 1}. 会话《${hit.conversationTitle || '（无标题）'}》${
          project ? `（项目：${project}）` : ''
        }${hit.conversationId === conversationId ? '（就是当前这段对话）' : ''}`,
        `   会话 id：${hit.conversationId}`,
        'title' === hit.target
          ? `   命中位置：会话标题（最后更新：${momentText(hit.updatedAt)}）`
          : `   命中位置：${roleText(hit.role)}消息 · ${momentText(hit.createdAt)}${at}`,
        'title' === hit.target ? '' : `   片段：${snippet(hit.content, keyword)}`
      )
    })
    lines.push(
      '（要回看上下文，用 action=read 读对应的会话 id；命中给了「第 n 条」时，offset 给 n - 1 就从那条附近读起）'
    )
    return lines.filter((line) => '' !== line).join('\n')
  }

  if ('read' === action) {
    const id = String(args?.conversation_id ?? '').trim()
    if (!id) throw new Error('read 需要 conversation_id（先用 list / search 找到要读的会话）')
    const messages = await ConversationDb.messages(id)
    if (!messages.length) throw new Error(`这个会话没有本地消息记录：${id}`)
    const offset = Math.max(0, Math.floor(Number(args?.offset ?? 0) || 0))
    const limit = size(20, 100)
    const page = messages.slice(offset, offset + limit)
    if (!page.length) {
      return `这个会话共 ${messages.length} 条消息，offset 超出范围：${offset}`
    }
    const conversation = await ConversationDb.find(id)
    const project = conversation ? useProjectStore().find(conversation.projectId)?.name : ''
    const lines = [
      `会话《${conversation?.title || id}》${project ? `（项目：${project}）` : ''}：` +
        `共 ${messages.length} 条消息，以下是第 ${offset + 1}-${offset + page.length} 条：`
    ]
    // 编辑提问 / 重新生成会留下旧分支：这些消息也在库里，按时间顺序一并给出，先说清免得模型当成一条线
    if (branched(messages)) {
      lines.push('（这个会话编辑过提问或重新生成过回复，旧分支的消息也在其中，按时间顺序一并给出）')
    }
    page.forEach((message, index) => {
      const calls = toolCallCount(message.reference)
      lines.push(
        `${offset + index + 1}. [${roleText(message.role)}${
          message.createdAt ? ` ${momentText(message.createdAt)}` : ''
        }${calls ? `，${calls} 次工具调用` : ''}]`,
        oneLine(message.content, 2000) || '（这条没有正文）'
      )
    })
    if (offset + page.length < messages.length) {
      lines.push(`（还有 ${messages.length - offset - page.length} 条，可加大 offset 继续读）`)
    }
    return lines.join('\n')
  }
  if ('write' === action || 'replace' === action) {
    // 记忆跟着项目走：没有所属项目的对话（项目已删除等）没有可写的地方
    if (!projectId) {
      throw new Error('当前对话没有所属项目，长期记忆没有可写的地方（记忆按项目存）')
    }
    const projects = useProjectStore()
    const project = projects.find(projectId)
    if (!project) throw new Error(`找不到当前对话所属的项目：${projectId}`)
    const edit: MemoryEdit =
      'write' === action ? applyWrite(project.memory, args) : applyReplace(project.memory, args)
    // 内容没变就不写库：白改一次「最后更新」没有意义
    if (edit.changed) await projects.update(project.id, { memory: edit.memory })
    const state = edit.cleared
      ? `记忆已清空（原来 ${edit.before} 行）`
      : `现在共 ${edit.after} 行、${edit.memory.length} 字`
    const tail = [
      edit.cleared
        ? '（从下一轮起不再下发任何记忆内容）'
        : '（从下一轮起随每轮请求下发，本项目的所有会话共用）',
      '要接着改：局部改就用 replace（find / content），整体重写用 write 的 mode=replace，' +
        '清空用 write 的 mode=replace + content 空串，也可以在项目编辑器的「记忆」里手工改。'
    ]
    const lines: string[] = []
    if ('write' === action) {
      if (!edit.changed) return `项目「${project.name}」的长期记忆与要写入的内容一致，没有改动。`
      lines.push(
        edit.cleared
          ? `已清空项目「${project.name}」的长期记忆（原来 ${edit.before} 行）`
          : `已写入项目「${project.name}」的长期记忆` +
              `（${'append' === edit.mode ? '追加' : '整段替换'}，${state}）：`
      )
      if (!edit.cleared) lines.push(oneLine(edit.content, 200))
    } else {
      if (!edit.changed) return `项目「${project.name}」的长期记忆没有需要改动的地方。`
      const at =
        edit.line === edit.endLine ? `第 ${edit.line} 行` : `第 ${edit.line}-${edit.endLine} 行`
      const newText = edit.content
        ? oneLine(edit.content, 200) || '（只有空白）'
        : '（空，这一段被删掉了）'
      lines.push(
        `已替换项目「${project.name}」的长期记忆里${at}的 ${edit.count} 处（${state}）：`,
        `- 原文：${oneLine(edit.find, 200)}`,
        `+ 新文：${newText}`
      )
      if (edit.adjusted) {
        lines.push(
          '（find 末尾的换行在记忆里不存在——记忆首尾不留空白，这次按去掉结尾换行后的原文替换）'
        )
      }
    }
    lines.push(...tail)
    // 记忆每轮都占上下文：写得太长时点一句，让模型自己精简（不硬拦，重写本来就由模型发起）
    if (edit.memory.length > MEMORY_HINT_SIZE) {
      lines.push(
        `（提醒：记忆已到 ${edit.memory.length} 字，它每轮都占上下文——把过时或重复的条目合并掉，` +
          '用 write 的 mode=replace 整段重写一遍会更省。）'
      )
    }
    return lines.join('\n')
  }
  throw new Error(`记忆不支持的操作：${action}`)
}

/**
 * 本地技能（action 分派）：清单与文件都由主进程读技能目录
 * （相对路径由主进程限制在技能目录内，非文本文件与超长内容在那边处理）。
 */
async function invokeSkill(args: any): Promise<string> {
  const action = actionOf(args, SKILL_ACTIONS)
  if ('list' === action) {
    const skills = await window.ipc.skill.list()
    if (!skills.length) {
      const root = await window.ipc.skill.path()
      return `本机没有已安装的技能（技能目录：${root}），可在「技能管理」页安装服务端技能。`
    }
    const lines = [`共 ${skills.length} 个已安装技能：`]
    skills.forEach((skill, index) => {
      const version = skill.meta
        ? `（版本 ${skill.meta.versionName || skill.meta.versionId}）`
        : '（手工放入，无版本信息）'
      const installed = skill.meta ? `，安装于 ${momentText(skill.meta.installedAt)}` : ''
      lines.push(
        `${index + 1}. ${skill.name}${version}`,
        `   文件 ${skill.fileCount} 个，共 ${sizeText(skill.size)}${installed}`,
        skill.summary ? `   摘要：${skill.summary}` : ''
      )
    })
    lines.push(
      '（技能是「按说明做事」的能力包：用 action=read 读它的说明，再照说明用本机工具做事）'
    )
    return lines.filter((line) => '' !== line).join('\n')
  }

  const skillName = String(args?.name ?? '').trim()
  if (!skillName) throw new Error(`${action} 需要 name（技能名称；先用 action=list 看有哪些技能）`)
  if ('files' === action) {
    const result = await window.ipc.skill.files(skillName)
    if (!result.files.length) return `技能「${skillName}」里没有文件。`
    return [
      `技能「${skillName}」的文件清单（共 ${result.files.length} 个，路径相对技能目录）：`,
      ...result.files.map((file) => `- ${file.path}（${sizeText(file.size)}）`)
    ].join('\n')
  }
  if ('read' === action) {
    const relative = String(args?.path ?? '').trim()
    const file = await window.ipc.skill.file(skillName, relative)
    return [
      `【技能 ${skillName} · ${relative || 'SKILL.md'}】（${sizeText(file.size)}${
        file.truncated ? '，已按上限截断' : ''
      }）`,
      file.content
    ].join('\n')
  }
  throw new Error(`技能不支持的操作：${action}`)
}

/** 时间文案：给模型看的时间一律带日期（ISO 文本或时间戳都可以） */
function momentText(value: string | number | Date): string {
  const date = value instanceof Date ? value : new Date(value)
  if (Number.isNaN(date.getTime())) return '（时间未知）'
  return date.toLocaleString('zh-CN', { hour12: false })
}

/**
 * 折叠空白：换行与连续空白折成一个空格并去首尾。
 * 命中的片段、关键词与摘要都可能跨行（模型整段粘过来的多行文本），
 * 先折成单行再处理，工具返回的文本才经得起一行行读。
 */
function collapse(value: unknown): string {
  return String(value ?? '')
    .replace(/\s+/g, ' ')
    .trim()
}

/** 单行摘要：折叠空白后按长度截断 */
function oneLine(value: unknown, limit: number): string {
  const body = collapse(value)
  return body.length > limit ? `${body.slice(0, limit)}…` : body
}

/**
 * 命中片段：以关键词为中心取一小段上下文（找不到关键词就给开头一段）。
 * 关键词与正文都按折叠空白后的形态找位置——关键词是整段粘来的多行文本时，
 * 整段对不上（库里的换行与缩进未必一样）就退到它的第一段，片段仍能围着命中处取。
 */
function snippet(content: unknown, keyword: string, span = 80): string {
  const body = collapse(content)
  if (!body) return '（空）'
  const word = collapse(keyword)
  let index = -1
  let length = word.length
  for (const head of word ? [word, ...word.split(' ')] : []) {
    const at = body.toLowerCase().indexOf(head.toLowerCase())
    if (at >= 0 && (index < 0 || at < index)) {
      index = at
      length = head.length
    }
  }
  if (index < 0) return oneLine(body, span * 2)
  const start = Math.max(0, index - span)
  const end = Math.min(body.length, index + length + span)
  return `${start > 0 ? '…' : ''}${body.slice(start, end)}${end < body.length ? '…' : ''}`
}

/** 角色文案：会话库里的 role 转成给模型看的说法 */
function roleText(role: string): string {
  if ('user' === role) return '用户'
  if ('assistant' === role) return '助手'
  if ('system' === role) return '系统'
  return role || '未知'
}

/** 会话里有没有分支：同一个父消息下挂了多条子消息，就是编辑提问 / 重新生成留下的旧分支 */
function branched(messages: Array<{ parentId: string }>): boolean {
  const children = new Map<string, number>()
  for (const message of messages) {
    const key = message.parentId || ''
    children.set(key, (children.get(key) ?? 0) + 1)
  }
  for (const count of children.values()) {
    if (count > 1) return true
  }
  return false
}

/** 这条消息带了几次工具调用：调用链存在 reference 里（JSON 文本） */
function toolCallCount(reference: string): number {
  const value = String(reference ?? '').trim()
  if (!value) return 0
  try {
    const rounds = JSON.parse(value)?.rounds
    if (!Array.isArray(rounds)) return 0
    return rounds.reduce(
      (total: number, round: any) => total + (Array.isArray(round?.calls) ? round.calls.length : 0),
      0
    )
  } catch {
    return 0
  }
}

/** 字节数转可读大小 */
function sizeText(size: number): string {
  const value = Math.max(0, Number(size) || 0)
  if (value < 1024) return `${value} 字节`
  if (value < 1024 * 1024) return `${(value / 1024).toFixed(1)} KB`
  return `${(value / (1024 * 1024)).toFixed(1)} MB`
}

/** 命令结果的文字呈现：先讲清这次跑的是什么、结果如何，再给两路输出 */
function commandResult(result: {
  command: string
  cwd: string
  code: number | null
  signal: string
  duration: number
  stdout: string
  stderr: string
}): string {
  const lines = [
    `命令：${result.command}`,
    `目录：${result.cwd}`,
    `退出码：${null === result.code ? '无（已终止或未能启动）' : result.code}${
      result.signal ? `（信号 ${result.signal}）` : ''
    }`,
    `耗时：${result.duration}ms`
  ]
  if (result.stdout) lines.push(`--- 标准输出 ---\n${result.stdout}`)
  if (result.stderr) lines.push(`--- 错误输出 ---\n${result.stderr}`)
  if (!result.stdout && !result.stderr) lines.push('（没有任何输出）')
  return lines.join('\n')
}

/**
 * 组装本次对话可用的工具：本地工具（文件与命令）+ 项目里启用的项目工具。
 * 函数名可能撞车（项目工具的展示名归一后也许等于本地工具名），这里统一改名并记下对应关系。
 * 同一份工具清单按引用内容缓存：每轮都取一次定义没有意义，改了工具清单才会重新取。
 */
export async function buildTools(project: Project | null): Promise<ToolSet> {
  const refs = (project?.tools ?? []).filter((item) => false !== item?.enabled)
  const cacheKey = JSON.stringify(refs)
  const cached = cache.get(cacheKey)
  if (cached) return cached
  const definitions: any[] = []
  const catalog: Record<string, ToolInfo> = {}
  const local = localTools()
  for (const item of local) {
    definitions.push(item.tool)
    catalog[item.info.name] = item.info
  }
  const taken = new Set(Object.keys(catalog))
  /** 服务端工具：归一后的函数名 → 调用时原样带回的 ref */
  const remote = new Map<string, { ref: any; info: ToolInfo }>()
  /** 服务端定义是否取全：没取全就不进缓存，下一轮再试一次 */
  let complete = true
  if (refs.length) {
    try {
      const result: any = await BotApi.tools(refs, { error: false, warning: false })
      for (const row of ApiUtil.succeed(result) ? (ApiUtil.data(result)?.rows ?? []) : []) {
        const base = String(row?.function?.name ?? '').trim()
        if (!base) continue
        let name = base
        let index = 2
        while (taken.has(name)) {
          name = `${base}_${index}`
          index++
        }
        taken.add(name)
        const kind = String(row?.ref?.kind ?? 'method')
        const info: ToolInfo = {
          name,
          description: String(row?.function?.description ?? ''),
          scope: 'server',
          label: `${SERVER_KINDS[kind] ?? kind}：${row?.function?.description || base}`
        }
        definitions.push({ type: 'function', function: { ...row.function, name } })
        catalog[name] = info
        remote.set(name, { ref: row.ref, info })
      }
    } catch {
      // 项目工具取不到（接口不通、权限不足）时不影响本地工具继续可用
      complete = false
    }
  }

  async function invoke(
    name: string,
    args: any,
    projectId: string,
    conversationId: string
  ): Promise<string> {
    const target = remote.get(name)
    if (!target) return invokeLocal(name, args, projectId, conversationId)
    const result: any = await BotApi.tool(target.ref, args ?? {}, { error: false, warning: false })
    if (!ApiUtil.succeed(result)) throw new Error(ApiUtil.message(result) || '工具调用失败')
    const data = ApiUtil.data(result)
    return 'string' === typeof data ? data : JSON.stringify(data)
  }

  const toolset: ToolSet = { definitions, catalog, invoke }
  if (complete) cache.set(cacheKey, toolset)
  return toolset
}

/** 工具清单缓存：键是项目里选中的工具引用，值是按该清单归一好的定义与执行入口 */
const cache = new Map<string, ToolSet>()
