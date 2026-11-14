import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useChatViewStore } from './chatView'

const values = new Map<string, string>()

beforeEach(() => {
  values.clear()
  vi.stubGlobal('localStorage', {
    getItem: (key: string) => values.get(key) ?? null,
    setItem: (key: string, value: string) => values.set(key, value)
  })
  setActivePinia(createPinia())
})

describe('chat view display modes', () => {
  it('defaults tables and code blocks to the existing nowrap layout', () => {
    const store = useChatViewStore()
    expect(store.tableMode).toBe('nowrap')
    expect(store.codeMode).toBe('nowrap')
  })

  it('persists the selected table and code layouts', () => {
    const store = useChatViewStore()
    store.setTableMode('wrap')
    store.setCodeMode('wrap')

    expect(store.tableMode).toBe('wrap')
    expect(store.codeMode).toBe('wrap')
    expect(values.get('fs_chat_table_mode')).toBe('wrap')
    expect(values.get('fs_chat_code_mode')).toBe('wrap')

    setActivePinia(createPinia())
    const restored = useChatViewStore()
    expect(restored.tableMode).toBe('wrap')
    expect(restored.codeMode).toBe('wrap')
  })

  it('falls back to nowrap for unknown values', () => {
    values.set('fs_chat_table_mode', 'wrap')
    values.set('fs_chat_code_mode', 'wrap')
    setActivePinia(createPinia())
    const store = useChatViewStore()
    expect(store.tableMode).toBe('wrap')
    expect(store.codeMode).toBe('wrap')

    store.setTableMode('unknown')
    store.setCodeMode('unknown')

    expect(store.tableMode).toBe('nowrap')
    expect(store.codeMode).toBe('nowrap')
    expect(values.get('fs_chat_table_mode')).toBe('nowrap')
    expect(values.get('fs_chat_code_mode')).toBe('nowrap')
  })
})
