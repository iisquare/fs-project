import { mkdtempSync, readFileSync, rmSync } from 'fs'
import { tmpdir } from 'os'
import { join } from 'path'
import { afterEach, describe, expect, it } from 'vitest'
import { ConfigStore } from './config'

const tempDirs: string[] = []

afterEach(() => {
  for (const dir of tempDirs.splice(0)) rmSync(dir, { recursive: true, force: true })
})

describe('ConfigStore desktop approval policy', () => {
  it('defaults to asking, persists auto, and falls back from invalid values to ask', () => {
    const dir = mkdtempSync(join(tmpdir(), 'fs-bot-config-'))
    tempDirs.push(dir)
    const config = ConfigStore.getInstance()

    config.init(dir)
    expect(config.getSettings()['desktopApprovalPolicy']).toBe('ask')

    config.updateSetting('desktopApprovalPolicy', 'auto')
    expect(config.getSettings()['desktopApprovalPolicy']).toBe('auto')
    expect(readFileSync(join(dir, 'userdata', 'config.toml'), 'utf8')).toContain(
      'desktopApprovalPolicy = "auto"'
    )

    config.init(dir)
    expect(config.getSettings()['desktopApprovalPolicy']).toBe('auto')

    config.updateSetting('desktopApprovalPolicy', 'invalid')
    expect(config.getSettings()['desktopApprovalPolicy']).toBe('ask')
  })
})
