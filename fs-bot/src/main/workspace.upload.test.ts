import { mkdtempSync, rmSync, writeFileSync } from 'fs'
import { tmpdir } from 'os'
import { join } from 'path'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { ClientUpgradeRequiredError, type ClientMeta } from '../shared/client'
import { WorkspaceManager } from './workspace'

vi.mock('electron', () => ({
  protocol: { registerSchemesAsPrivileged: vi.fn() },
  net: { fetch: vi.fn() },
  shell: {}
}))

const client: ClientMeta = {
  clientVersion: '1.2.3',
  platform: 'win32',
  architecture: 'x64',
  clientId: 'client-1',
  channel: 'stable'
}

const roots: string[] = []

afterEach(() => {
  vi.unstubAllGlobals()
  vi.restoreAllMocks()
  for (const root of roots.splice(0)) rmSync(root, { recursive: true, force: true })
})

function createUploadFixture(): { manager: WorkspaceManager; path: string } {
  const root = mkdtempSync(join(tmpdir(), 'fs-bot-upload-'))
  roots.push(root)
  const path = join(root, 'image.png')
  writeFileSync(path, Buffer.from([0x89, 0x50, 0x4e, 0x47]))
  const manager = new WorkspaceManager()
  manager.setProjectRootsResolver(() => [{ path: root, permission: 'read' }])
  return { manager, path }
}

describe('WorkspaceManager.upload', () => {
  it('sends the complete client context with multipart uploads', async () => {
    const { manager, path } = createUploadFixture()
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(
        JSON.stringify({
          code: 0,
          data: { id: 'file-1', name: 'image.png', type: 'image/png', suffix: 'png', size: 4 }
        }),
        { status: 200, headers: { 'Content-Type': 'application/json' } }
      )
    )
    vi.stubGlobal('fetch', fetchMock)

    const result = await manager.upload(
      'project-1',
      path,
      { url: 'https://example.test/agent/bot/upload', token: 'token-1', client },
      'conversation-1'
    )

    expect(result.id).toBe('file-1')
    const init = fetchMock.mock.calls[0]?.[1] as RequestInit
    expect(init.headers).toMatchObject({
      Accept: 'application/json',
      Authorization: 'Bearer token-1',
      'x-client-version': '1.2.3',
      'x-client-platform': 'win32',
      'x-client-arch': 'x64',
      'x-client-id': 'client-1',
      'x-client-channel': 'stable'
    })
  })

  it('turns a 426 response into an upgrade error', async () => {
    const { manager, path } = createUploadFixture()
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response(JSON.stringify({ code: 90426, message: '客户端版本过低，必须升级' }), {
          status: 426,
          headers: { 'Content-Type': 'application/json' }
        })
      )
    )

    await expect(
      manager.upload('project-1', path, {
        url: 'https://example.test/agent/bot/upload',
        token: 'token-1',
        client
      })
    ).rejects.toBeInstanceOf(ClientUpgradeRequiredError)
  })
})
