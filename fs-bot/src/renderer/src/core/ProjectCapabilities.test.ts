import { describe, expect, it } from 'vitest'
import {
  ECHARTS_CAPABILITY_PROMPT,
  PROJECT_CAPABILITY,
  capabilityPrompt,
  normalizeCapabilities
} from './ProjectCapabilities'

describe('project capabilities', () => {
  it('converts legacy sub-agent flags and keeps unknown future capabilities', () => {
    expect(
      normalizeCapabilities({
        capabilities: ['output.future', PROJECT_CAPABILITY.BROWSER, PROJECT_CAPABILITY.BROWSER],
        projectToolAgentEnabled: true,
        desktopAgentEnabled: 1
      })
    ).toEqual([
      'output.future',
      PROJECT_CAPABILITY.BROWSER,
      PROJECT_CAPABILITY.PROJECT_TOOLS,
      PROJECT_CAPABILITY.DESKTOP
    ])
  })

  it('adds the ECharts prompt only when the capability is enabled', () => {
    expect(capabilityPrompt({ capabilities: [] })).toBe('')
    expect(capabilityPrompt({ capabilities: [PROJECT_CAPABILITY.ECHARTS] })).toBe(
      ECHARTS_CAPABILITY_PROMPT
    )
    expect(ECHARTS_CAPABILITY_PROMPT).toContain('"subtext":"共 39 人"')
  })
})
