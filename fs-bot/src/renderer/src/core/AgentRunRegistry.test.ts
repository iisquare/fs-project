import { beforeEach, describe, expect, it } from 'vitest'
import {
  beginAgentParentRun,
  cancelAgentParentRun,
  isAgentParentRunCanceled,
  resetAgentParentRuns
} from './AgentRunRegistry'

describe('AgentRunRegistry', () => {
  beforeEach(() => resetAgentParentRuns())

  it('keeps a live token active until explicitly canceled', () => {
    const token = beginAgentParentRun('conversation-a')
    expect(token).toBeTruthy()
    expect(isAgentParentRunCanceled('conversation-a', token)).toBe(false)
    cancelAgentParentRun('conversation-a', token)
    expect(isAgentParentRunCanceled('conversation-a', token)).toBe(true)
  })

  it('invalidates an old token when a new parent run starts', () => {
    const oldToken = beginAgentParentRun('conversation-a')
    const newToken = beginAgentParentRun('conversation-a')
    expect(newToken).not.toBe(oldToken)
    expect(isAgentParentRunCanceled('conversation-a', oldToken)).toBe(true)
    expect(isAgentParentRunCanceled('conversation-a', newToken)).toBe(false)
  })

  it('does not let a stale task cancel the current run', () => {
    const oldToken = beginAgentParentRun('conversation-a')
    const currentToken = beginAgentParentRun('conversation-a')
    cancelAgentParentRun('conversation-a', oldToken)
    expect(isAgentParentRunCanceled('conversation-a', currentToken)).toBe(false)
  })

  it('cancels the current run when no token is supplied', () => {
    const token = beginAgentParentRun('conversation-a')
    cancelAgentParentRun('conversation-a')
    expect(isAgentParentRunCanceled('conversation-a', token)).toBe(true)
  })
})
