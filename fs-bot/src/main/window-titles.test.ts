import { describe, expect, it, vi } from 'vitest'
import { collectWindowTitles, readWindowsWindowTitles, usableWindowTitle } from './window-titles'

describe('Windows window titles', () => {
  it('collects Unicode titles once per valid handle', async () => {
    const titlesByHandle = new Map([
      [42, '设置 - Windows'],
      [84, '待办事项.md - Typora'],
      [126, '测试 😀']
    ])
    const readTitle = vi.fn((handle: number) => titlesByHandle.get(handle) ?? '')
    const titles = await collectWindowTitles([42, 42, 84, 126, -1, 'invalid'], readTitle)

    expect(titles.get(42)).toBe('设置 - Windows')
    expect(titles.get(84)).toBe('待办事项.md - Typora')
    expect(titles.get(126)).toBe('测试 😀')
    expect(readTitle).toHaveBeenCalledTimes(3)
  })

  it('continues when a window disappears while titles are read', async () => {
    const titles = await collectWindowTitles([42, 84], (handle) => {
      if (42 === handle) throw new Error('window closed')
      return '仍可读取'
    })

    expect(titles).toEqual(new Map([[84, '仍可读取']]))
  })

  it('does not load Windows bindings outside Windows', async () => {
    const titles = await readWindowsWindowTitles([42], { platform: 'darwin' })

    expect(titles.size).toBe(0)
  })

  it('replaces irreversibly decoded titles with a readable marker', () => {
    expect(usableWindowTitle('�� Agent ִ������ - fs-bot')).toBe('（窗口标题不可读取）')
    expect(usableWindowTitle('Visual Studio Code')).toBe('Visual Studio Code')
  })
})
