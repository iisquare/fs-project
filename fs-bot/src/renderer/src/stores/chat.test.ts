import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { cancelAgentRuns } from '@renderer/core/AgentRuntime'
import {
  persistedMessage,
  recoverInterrupted,
  useChatStore,
  type ChatMessage,
  type ChatRound
} from './chat'

vi.mock('@renderer/core/AgentRuntime', () => ({
  beginAgentParentRun: vi.fn(() => 'agent-run-token'),
  cancelAgentRuns: vi.fn(),
  resetAgentRuntime: vi.fn()
}))

function assistant(): ChatMessage {
  const round: ChatRound = {
    content: 'partial answer',
    reasoning: 'checked',
    calls: [
      {
        id: 'call-1',
        name: 'run_command',
        args: '{"command":"npm test"}',
        status: 'running',
        result: '',
        duration: 0,
        scope: 'local',
        label: '执行命令'
      }
    ]
  }
  return {
    id: 'reply-1',
    parentId: 'question-1',
    role: 'assistant',
    content: 'partial answer',
    reasoning: 'checked',
    status: 'running',
    createdAt: '2026-11-04T00:00:00.000Z',
    rounds: [round],
    mentions: []
  }
}

describe('chat checkpoints', () => {
  it('serializes the complete execution chain into the message checkpoint', () => {
    const row = persistedMessage(assistant())
    expect(JSON.parse(row.reference)).toMatchObject({
      rounds: [
        {
          content: 'partial answer',
          reasoning: 'checked',
          calls: [{ id: 'call-1', status: 'running', result: '' }]
        }
      ]
    })
  })

  it('marks a checkpoint left running by a previous process as interrupted', () => {
    const message = assistant()
    expect(recoverInterrupted([message])).toBe(true)
    expect(message.status).toBe('stopped')
    expect(message.rounds[0].calls[0]).toMatchObject({
      status: 'stopped',
      result: '客户端上次退出时该调用尚未完成'
    })
    expect(recoverInterrupted([message])).toBe(false)
  })
})

describe('chat stop', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    setActivePinia(createPinia())
    vi.stubGlobal('window', {
      ipc: {
        workspace: {
          cancel: vi.fn().mockResolvedValue(undefined)
        }
      }
    })
  })

  afterEach(() => vi.unstubAllGlobals())

  it('stops a running session once and cancels its local work', () => {
    const store = useChatStore()
    const session = store.ensure('conversation-a')
    session.running = true
    session.agentRunToken = 'agent-run-token'

    expect(store.stop('conversation-a')).toBe(true)
    expect(session.stopping).toBe(true)
    expect(store.stop('conversation-a')).toBe(false)
    expect(cancelAgentRuns).toHaveBeenCalledTimes(1)
    expect(cancelAgentRuns).toHaveBeenCalledWith('conversation-a', 'agent-run-token', true)
    expect(window.ipc.workspace.cancel).toHaveBeenCalledTimes(1)
    expect(window.ipc.workspace.cancel).toHaveBeenCalledWith('conversation-a')
  })

  it('does not add a stop request for an idle session', () => {
    const store = useChatStore()
    const session = store.ensure('conversation-b')

    expect(store.stop('conversation-b')).toBe(false)
    expect(session.stopping).toBe(false)
    expect(window.ipc.workspace.cancel).not.toHaveBeenCalled()
  })
})
