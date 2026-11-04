import { describe, expect, it } from 'vitest'
import { markdownToPlainText } from './MarkdownText'

describe('markdownToPlainText', () => {
  it('removes markdown structure while preserving technical identifiers', () => {
    const text = markdownToPlainText(
      '## 任务结果\n\n读取 `created_uid` 和 user_id，再检查 https://example.com/#section。'
    )

    expect(text).toBe('任务结果 读取 created_uid 和 user_id，再检查 https://example.com/#section。')
    expect(text).not.toContain('##')
  })

  it('flattens lists, links and tables into one line', () => {
    const text = markdownToPlainText(
      '- 查看 [接口文档](https://example.com/docs)\n- 字段 task_key\n\n| 名称 | 状态 |\n| --- | --- |\n| 数据 | 完成 |'
    )

    expect(text).toContain('查看 接口文档')
    expect(text).toContain('字段 task_key')
    expect(text).toContain('名称 状态 数据 完成')
    expect(text).not.toContain('\n')
  })
})
