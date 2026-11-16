import { describe, expect, it } from 'vitest'
import { agentToolDisplay } from './AgentToolDisplay'

describe('agent tool display metadata', () => {
  it('keeps the concrete project tool type recorded by the runtime', () => {
    expect(
      agentToolDisplay(
        {
          name: 'theme_1',
          scope: 'server',
          label: '数据主题：销售分析',
          category: '数据主题'
        },
        'project_tools'
      )
    ).toEqual({
      scope: 'server',
      label: '数据主题：销售分析',
      category: '数据主题'
    })
  })

  it('shows project tools rather than sub-agents for legacy interactions', () => {
    expect(agentToolDisplay({ name: 'theme_1' }, 'project_tools')).toEqual({
      scope: 'server',
      label: 'theme_1',
      category: '项目工具'
    })
  })

  it('labels legacy browser and desktop operations by their own type', () => {
    expect(agentToolDisplay({ name: 'browser_op' }, 'browser')).toEqual({
      scope: 'local',
      label: 'browser_op',
      category: '浏览器操作'
    })
    expect(agentToolDisplay({ name: 'desktop_op' }, 'desktop')).toEqual({
      scope: 'local',
      label: 'desktop_op',
      category: '桌面操作'
    })
  })
})
