import { existsSync, mkdtempSync, rmSync, writeFileSync } from 'fs'
import { tmpdir } from 'os'
import { join } from 'path'
import { setTimeout as delay } from 'timers/promises'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { WorkspaceManager } from './workspace'

vi.mock('electron', () => ({
  protocol: { registerSchemesAsPrivileged: vi.fn() },
  net: { fetch: vi.fn() },
  shell: {}
}))

const roots: string[] = []

afterEach(() => {
  for (const root of roots.splice(0)) rmSync(root, { recursive: true, force: true })
})

async function waitForFile(path: string, timeout = 5_000): Promise<void> {
  const deadline = Date.now() + timeout
  while (!existsSync(path)) {
    if (Date.now() >= deadline) throw new Error(`等待文件超时：${path}`)
    await delay(20)
  }
}

describe('WorkspaceManager.shutdown', () => {
  it('stops running commands and rejects new ones', async () => {
    const root = mkdtempSync(join(tmpdir(), 'fs-bot-shutdown-'))
    roots.push(root)
    const marker = join(root, 'running')
    const script = join(root, 'hold.js')
    writeFileSync(
      script,
      `require('fs').writeFileSync(${JSON.stringify(marker)}, ''); setInterval(() => {}, 1000)`
    )

    const manager = new WorkspaceManager()
    manager.setProjectRootsResolver(() => [{ path: root, permission: 'readwrite' }])
    manager.setPolicy('full')
    const command = `"${process.execPath}" "${script}"`
    const running = manager.exec('project-1', command, root, 60, 'conversation-1')
    await waitForFile(marker)

    await expect(manager.shutdown()).resolves.toBe(1)
    await expect(running).resolves.toMatchObject({
      cancelled: true,
      timedOut: false
    })
    await expect(manager.exec('project-1', command, root, 60, 'conversation-2')).rejects.toThrow(
      '客户端正在退出'
    )
  })
})
