import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useFilesStore } from './files'

describe('files store', () => {
  const setFilesPanel = vi.fn()

  beforeEach(() => {
    setFilesPanel.mockReset()
    const values = new Map<string, string>()
    vi.stubGlobal('localStorage', {
      getItem: (key: string) => values.get(key) ?? null,
      setItem: (key: string, value: string) => values.set(key, value)
    })
    vi.stubGlobal('window', { ipc: { setFilesPanel } })
    setActivePinia(createPinia())
  })

  it('does not resize the window while dragging the side panel divider', () => {
    const files = useFilesStore()
    files.setVisible(true)
    setFilesPanel.mockClear()

    files.setWidth(320)

    expect(files.width).toBe(320)
    expect(setFilesPanel).not.toHaveBeenCalled()
  })
})
