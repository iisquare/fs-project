/**
 * 智能体编排的通用工具方法
 */
const AgenticUtil = {
  /** 节点类型 → 图标名称：与设计器画布上的节点图标保持一致（LayoutIcon 的名称） */
  nodeIcons: {
    Start: 'flow.startEvent',
    End: 'flow.endEvent',
    LLM: 'ai.model',
    Knowledge: 'algorithm.retrieval',
    QuestionClassifier: 'Guide',
    ParameterExtractor: 'algorithm.extraction',
    SwitchCase: 'flow.exclusiveGateway',
    Iteration: 'flow.iteration',
    Loop: 'flow.loop',
    VariableAggregator: 'Share',
    VariableAssigner: 'flow.config',
    Code: 'flow.script',
    Template: 'flow.transform',
    DocumentExtractor: 'Document',
    ListOperator: 'Operation',
    Time: 'Timer',
    HTTP: 'Link',
  },
  /** 节点图标：未登记的节点类型回落为智能体图标 */
  nodeIcon (type: any): string {
    const icons: any = AgenticUtil.nodeIcons
    return icons[String(type ?? '')] ?? 'ai.robot'
  },
  /** 节点输出：运行日志里以 JSON 字符串记录，解析成结构化结果（解析失败按空对象） */
  stepOutput (step: any): any {
    let output: any = step?.output
    if ('string' === typeof output) {
      try {
        output = JSON.parse(output)
      } catch (error) {
        output = null
      }
    }
    return output && 'object' === typeof output ? output : {}
  },
  /**
   * 工具调用记录：按节点顺序展开每个节点的 ReAct 轮次，只保留真正调用了工具的轮次
   * （最后一轮是模型直出，不作为工具调用列出）。
   */
  stepRounds (steps: any[]): any[] {
    const list: any[] = []
    ;(steps ?? []).forEach((step: any) => {
      const output: any = AgenticUtil.stepOutput(step)
      const rounds: any[] = Array.isArray(output?.rounds) ? output.rounds : []
      const chain: any[] = rounds.length
        ? rounds
        : (Array.isArray(output?.calls) ? [{ round: 1, calls: output.calls }] : [])
      chain.forEach((round: any, roundIndex: number) => {
        const calls: any[] = Array.isArray(round?.calls) ? round.calls : []
        if (calls.length) list.push({ step, round, calls, roundIndex })
      })
    })
    return list
  },
  /** 轮次文案：多轮时带上限（第 2 / 5 轮），单轮只显示第 1 轮 */
  roundLabel (round: any, maxRounds: any): string {
    const index = Number(round ?? 0)
    if (index < 1) return ''
    return Number(maxRounds ?? 0) > 1 ? `第 ${index} / ${maxRounds} 轮` : `第 ${index} 轮`
  },
  /**
   * 开始节点的输入清单：固定输入（query 用户输入、files 文件列表）默认启用，
   * 自定义参数来自 variables，调试运行、对话页参数表单与下游变量引用共用该清单。
   */
  startInputs (data: any): any[] {
    const items: any[] = []
    const query = data?.query ?? {}
    if (false !== query.enabled) {
      items.push({
        name: 'query',
        label: '用户输入',
        type: 'String',
        description: query.description ?? '用户输入内容',
        maxLength: query.maxLength ?? 256,
      })
    }
    // 文件列表为文件信息数组（文件存储服务返回的文件ID、文件名、文件类型等），仍按文件数组类型供下游引用
    const files = data?.files ?? {}
    if (false !== files.enabled) {
      items.push({
        name: 'files',
        label: '文件列表',
        type: 'Array<File>',
        description: files.description ?? '用户上传的文件列表，含文件存储服务返回的文件ID、文件名、文件类型等信息',
        maxCount: files.maxCount ?? 3,
        fileTypes: files.fileTypes ?? [],
      })
    }
    ;(data?.variables ?? []).forEach((item: any) => {
      if (!item?.name) return
      items.push({
        name: item.name,
        // 标题名称仅用于展示，为空时回落到变量名称
        label: item.label || item.name,
        type: item.type ?? 'String',
        description: item.description,
        required: true === item.required,
      })
    })
    return items
  },
  /**
   * 运行结果里的异常清单：
   * 1. 节点执行失败（status=2）；
   * 2. 工具方法调用失败（step.output.calls 里 status=2）——此时节点本身算成功，但同样要提示用户；
   * 3. 兜底：整体失败但没有步骤信息时，取运行结果的 error。
   * 返回可直接展示的文案数组，第一条作为摘要，其余作为详情。
   */
  runFailures (data: any): string[] {
    const list: string[] = []
    ;(data?.steps ?? []).forEach((step: any) => {
      if (2 === step.status) list.push(`节点「${step.name}」执行失败：${step.error || '未知原因'}`)
      ;(step.output?.calls ?? []).forEach((call: any) => {
        if (2 === call.status) list.push(`工具方法「${call.method}」调用失败：${call.error || '未知原因'}`)
      })
    })
    if (!list.length && 2 === (data?.status ?? 1) && data?.error) list.push(data.error)
    return list
  },
  /** 复制用的回复正文：保留完整 Markdown，包含 echarts 代码块 */
  answerText (content: any): string {
    return String(content ?? '').trim()
  },
  /**
   * 实时节点进度：running 追加一条（同一节点在循环里会执行多次），
   * 结束事件更新最近一条该节点的记录；流程对话与调试面板共用同一份写屏口径。
   */
  markStep (list: any[], step: any) {
    if ('running' === step?.state) {
      // 记下开始时间：执行中的节点显示已耗时，长时间没输出时也能看出还在跑
      list.push({ ...step, startedAt: Date.now() })
      return
    }
    for (let index = list.length - 1; index >= 0; index--) {
      if (list[index]?.id === step?.id && 'running' === list[index]?.state) {
        list[index] = { ...list[index], ...step }
        return
      }
    }
    list.push({ ...step })
  },
  /**
   * 实时输出过程：同一节点的同一轮只保留一条，工具方法按 id 更新
   * （后端先推「调用中」，工具返回后再推一次状态与耗时）。
   */
  markRound (list: any[], event: any) {
    const nodeId = String(event?.nodeId ?? '')
    const round = Number(event?.round ?? 0)
    let row = list.find((item: any) => item.nodeId === nodeId && item.round === round)
    if (!row) {
      row = { nodeId, round, maxRounds: 0, state: '', calls: [] }
      list.push(row)
    }
    row.maxRounds = event?.maxRounds ?? row.maxRounds
    row.state = event?.state ?? row.state
    const call = event?.call
    if (!call?.id) return
    const exist = row.calls.find((item: any) => item.id === call.id)
    if (exist) Object.assign(exist, call)
    else row.calls.push({ ...call })
  },
}

export default AgenticUtil
