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

describe('chat view table mode', () => {
  it('defaults to the existing nowrap layout', () => {
    expect(useChatViewStore().tableMode).toBe('nowrap')
  })

  it('persists the selected table layout', () => {
    const store = useChatViewStore()
    store.setTableMode('wrap')

    expect(store.tableMode).toBe('wrap')
    expect(values.get('fs_chat_table_mode')).toBe('wrap')

    setActivePinia(createPinia())
    expect(useChatViewStore().tableMode).toBe('wrap')
  })

  it('falls back to nowrap for unknown values', () => {
    values.set('fs_chat_table_mode', 'wrap')
    setActivePinia(createPinia())
    const store = useChatViewStore()
    expect(store.tableMode).toBe('wrap')

    store.setTableMode('unknown')

    expect(store.tableMode).toBe('nowrap')
    expect(values.get('fs_chat_table_mode')).toBe('nowrap')
  })
})
