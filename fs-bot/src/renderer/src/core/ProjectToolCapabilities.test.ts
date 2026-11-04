import { describe, expect, it } from 'vitest'
import type { ProjectTool } from '@renderer/db/ProjectDb'
import { projectToolCapabilitySummary } from './ProjectToolCapabilities'

function projectTool(overrides: Partial<ProjectTool>): ProjectTool {
  return {
    kind: 'method',
    id: 1,
    name: '测试工具',
    enabled: true,
    ...overrides
  }
}

describe('projectToolCapabilitySummary', () => {
  it('only declares project tool types that are actually enabled', () => {
    const summary = projectToolCapabilitySummary([
      projectTool({
        kind: 'theme',
        name: '销售分析',
        description: '按区域汇总销售数据'
      }),
      projectTool({ kind: 'knowledge', name: '停用知识库', enabled: false }),
      projectTool({ kind: 'skill', name: '本地技能' })
    ])

    expect(summary).toContain('类型：数据主题（1 个）')
    expect(summary).toContain('代表能力：销售分析（按区域汇总销售数据）')
    expect(summary).not.toContain('工具方法')
    expect(summary).not.toContain('知识库')
    expect(summary).not.toContain('本地技能')
  })

  it('keeps the summary bounded while preserving every enabled type', () => {
    const methods = Array.from({ length: 10 }, (_, index) =>
      projectTool({
        id: index + 1,
        name: `方法 ${index + 1}`,
        description: `方法说明 ${index + 1}`
      })
    )
    const summary = projectToolCapabilitySummary([
      ...methods,
      projectTool({ kind: 'theme', id: 11, name: '销售分析' })
    ])

    expect(summary).toContain('工具方法（10 个）、数据主题（1 个）')
    expect(summary).toContain('另有 3 个未展开')
    expect(summary.match(/方法 \d+/g)).toHaveLength(8)
  })

  it('returns an empty summary when no service tool is enabled', () => {
    expect(
      projectToolCapabilitySummary([
        projectTool({ kind: 'knowledge', name: '停用知识库', enabled: false }),
        projectTool({ kind: 'skill', name: '本地技能' })
      ])
    ).toBe('')
  })
})
