import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { beginAgentParentRun } from './AgentRunRegistry'
import {
  cancelAgentRuns,
  onAgentRuntimeEvent,
  resetAgentRuntime,
  runAgentTask
} from './AgentRuntime'
import type { ToolSet } from './Toolbox'

vi.mock('@renderer/db/AgentDb', () => {
  const session = {
    id: 'child-1',
    projectId: 'project-1',
    parentConversationId: 'parent-1',
    agent: 'project_tools',
    taskKey: '',
    sessionMode: 'new',
    reused: false,
    title: '测试任务',
    messages: [],
    summary: '',
    trace: [],
    status: 'completed',
    error: '',
    createdAt: '2026-11-04T00:00:00.000Z',
    updatedAt: '2026-11-04T00:00:00.000Z'
  }
  return {
    default: {
      get: vi.fn(async () => ({ ...session })),
      update: vi.fn(async () => undefined),
      saveInteraction: vi.fn(async () => undefined),
      experiences: vi.fn(async () => []),
      touch: vi.fn(async () => undefined)
    }
  }
})

vi.mock('./ModelChat', () => ({
  default: class {
    aborted = false

    start(params: {
      onDelta: (delta: {
        content: string
        reasoning: string
        finishReason: string
        toolCalls: unknown[]
      }) => void
      onClose: (result: { aborted: boolean }) => void
    }): void {
      queueMicrotask(() => {
        params.onDelta({
          content: '',
          reasoning: '',
          finishReason: '',
          toolCalls: [
            {
              index: 0,
              id: 'tool-call-1',
              function: { name: 'webFetch', arguments: '{}' }
            }
          ]
        })
        params.onClose({ aborted: false })
      })
    }

    stop(): void {
      this.aborted = true
    }
  }
}))

describe('AgentRuntime cancellation', () => {
  beforeEach(() => {
    vi.stubGlobal('window', {
      ipc: {
        workspace: {
          cancel: vi.fn().mockResolvedValue(undefined)
        }
      }
    })
  })

  afterEach(() => {
    resetAgentRuntime()
    vi.unstubAllGlobals()
  })

  it('ends a child run even when its tool ignores the abort signal', async () => {
    let startedTool!: () => void
    const toolStarted = new Promise<void>((resolve) => {
      startedTool = resolve
    })
    const tools: ToolSet = {
      definitions: [],
      catalog: {},
      invoke: vi.fn(() => {
        startedTool()
        return new Promise<string>(() => undefined)
      })
    }
    const events: Array<{ type: string; status: string; message: string }> = []
    const unsubscribe = onAgentRuntimeEvent((event) => {
      events.push({ type: event.type, status: event.status, message: event.message })
    })
    const parentRunToken = beginAgentParentRun('parent-1')
    const task = runAgentTask({
      agent: 'project_tools',
      objective: '查询项目名称',
      sessionId: 'child-1',
      projectId: 'project-1',
      parentConversationId: 'parent-1',
      parentRunToken,
      model: 'test-model',
      tools
    })

    await toolStarted
    cancelAgentRuns('parent-1', parentRunToken, true)

    let timer: ReturnType<typeof setTimeout> | undefined
    const outcome = await Promise.race([
      task.then(
        () => 'finished',
        (error: Error) => error.message
      ),
      new Promise<string>((resolve) => {
        timer = setTimeout(() => resolve('timed out'), 1000)
      })
    ])
    if (timer) clearTimeout(timer)
    unsubscribe()

    expect(outcome).toContain('子 Agent 已被停止')
    expect(events).toContainEqual({ type: 'canceled', status: 'canceled', message: '已停止' })
    expect(window.ipc.workspace.cancel).toHaveBeenCalledWith('child-1')
  })
})
