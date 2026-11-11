import { mkdtempSync, rmSync } from 'fs'
import { createServer, type IncomingHttpHeaders, type RequestListener, type Server } from 'http'
import type { AddressInfo } from 'net'
import { tmpdir } from 'os'
import { join } from 'path'
import { afterEach, describe, expect, it } from 'vitest'
import { ClientUpgradeRequiredError, type ClientMeta } from '../shared/client'
import { SkillManager } from './skills'

const client: ClientMeta = {
  clientVersion: '1.2.3',
  platform: 'win32',
  architecture: 'x64',
  clientId: 'client-1',
  channel: 'stable'
}

const roots: string[] = []
const servers: Server[] = []

afterEach(async () => {
  for (const root of roots.splice(0)) rmSync(root, { recursive: true, force: true })
  await Promise.all(
    servers.splice(0).map(
      (server) =>
        new Promise<void>((resolve) => {
          server.closeAllConnections()
          server.close(() => resolve())
        })
    )
  )
})

async function startServer(
  handler: RequestListener
): Promise<{ baseUrl: string; requests: IncomingHttpHeaders[] }> {
  const requests: IncomingHttpHeaders[] = []
  const server = createServer((request, response) => {
    requests.push(request.headers)
    handler(request, response)
  })
  servers.push(server)
  await new Promise<void>((resolve) => server.listen(0, '127.0.0.1', resolve))
  const address = server.address() as AddressInfo
  return { baseUrl: `http://127.0.0.1:${address.port}`, requests }
}

function managerForTest(): SkillManager {
  const root = mkdtempSync(join(tmpdir(), 'fs-bot-skill-download-'))
  roots.push(root)
  const manager = new SkillManager()
  manager.init(root)
  return manager
}

function install(manager: SkillManager, baseUrl: string): Promise<unknown> {
  return manager.install({
    baseUrl,
    token: 'token-1',
    client,
    skillId: 1,
    skillName: 'demo',
    versionId: 2,
    versionName: 'v2'
  })
}

describe('SkillManager.install download', () => {
  it('sends client context and turns 426 into an upgrade error', async () => {
    const { baseUrl, requests } = await startServer((_request, response) => {
      response.writeHead(426, { 'Content-Type': 'application/json' })
      response.end(JSON.stringify({ code: 90426, message: '客户端版本过低，必须升级' }))
    })

    await expect(install(managerForTest(), baseUrl)).rejects.toBeInstanceOf(
      ClientUpgradeRequiredError
    )
    expect(requests[0]).toMatchObject({
      authorization: 'Bearer token-1',
      'x-client-version': '1.2.3',
      'x-client-platform': 'win32',
      'x-client-arch': 'x64',
      'x-client-id': 'client-1',
      'x-client-channel': 'stable'
    })
  })

  it('keeps credentials on the API origin and drops them after cross-origin redirects', async () => {
    const fileServer = await startServer((_request, response) => {
      response.writeHead(200, { 'Content-Type': 'application/octet-stream' })
      response.end('not an archive')
    })
    const apiServer = await startServer((_request, response) => {
      response.writeHead(302, { Location: `${fileServer.baseUrl}/skill.zip` })
      response.end()
    })

    await expect(install(managerForTest(), apiServer.baseUrl)).rejects.toThrow('不支持的技能包格式')
    expect(apiServer.requests[0]).toMatchObject({
      authorization: 'Bearer token-1',
      'x-client-version': '1.2.3',
      'x-client-id': 'client-1'
    })
    expect(fileServer.requests[0]?.authorization).toBeUndefined()
    expect(fileServer.requests[0]?.['x-client-version']).toBeUndefined()
    expect(fileServer.requests[0]?.['x-client-id']).toBeUndefined()
  })
})
