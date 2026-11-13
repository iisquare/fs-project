/** 系统提示词快速注入项：标题同时用于识别是否已经注入过 */
export interface PromptInjection {
  id: string
  label: string
  heading: string
  text: string
}

export const PROMPT_INJECTIONS: PromptInjection[] = [
  {
    id: 'known-info',
    label: '已知信息',
    heading: '## 已知信息',
    text: '\n## 已知信息\n- 当前日期：{{#sys.date#}}',
  },
  {
    id: 'echarts',
    label: 'ECharts 图表',
    heading: '## 可视化输出',
    text: `
## 可视化输出
数据适合用图表表达时，可在正文中插入一个或多个 \`\`\`echarts 代码块。每块只包含一个合法 JSON ECharts option，数据必须来自上下文。无合适数据或文字已足够时不输出图表，禁止函数、注释和外部脚本。

示例：
\`\`\`echarts
{"title":{"text":"各部门人数"},"xAxis":{"type":"category","data":["研发","市场","运营"]},"yAxis":{"type":"value"},"series":[{"type":"bar","data":[18,12,9]}]}
\`\`\``,
  },
]

/** 追加提示词；已存在同名标题时保持不变，避免重复点击产生重复规则 */
export function appendPromptInjection(current: string, injection: PromptInjection): string {
  const source = String(current ?? '')
  if (source.includes(injection.heading)) return source
  return source + injection.text
}
