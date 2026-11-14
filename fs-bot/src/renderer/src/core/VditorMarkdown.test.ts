import { describe, expect, it, vi } from 'vitest'

vi.mock('dompurify', () => ({
  default: {
    addHook: vi.fn(),
    sanitize: (html: string) => html
  }
}))

import { parseEChartsOption, removeInlineEventHandlers } from './VditorMarkdown'

describe('Vditor preview security cleanup', () => {
  it('removes every inline event handler exposed by the generated preview', () => {
    const element = {
      attributes: [
        { name: 'class' },
        { name: 'onclick' },
        { name: 'ONMOUSEOVER' },
        { name: 'data-type' }
      ],
      removeAttribute: vi.fn()
    }
    const root = {
      querySelectorAll: vi.fn(() => [element])
    } as unknown as ParentNode

    removeInlineEventHandlers(root)

    expect(element.removeAttribute.mock.calls).toEqual([['onclick'], ['ONMOUSEOVER']])
  })
})

describe('Vditor ECharts options', () => {
  it('accepts common JSON5 formatting without executing JavaScript', () => {
    expect(
      parseEChartsOption(`option = {
        // chart title
        title: { text: '销量' },
        series: [{ type: 'bar', data: [1, 2, 3], }],
      };`)
    ).toEqual({
      title: { text: '销量' },
      series: [{ type: 'bar', data: [1, 2, 3] }]
    })
  })

  it('rejects function expressions', () => {
    expect(() => parseEChartsOption(`{ formatter: function (value) { return value } }`)).toThrow()
  })

  it('drops prototype-related keys recursively', () => {
    expect(parseEChartsOption(`{ "__proto__": { "polluted": true }, ok: 1 }`)).toEqual({
      ok: 1
    })
  })

  it('moves titled pie charts down slightly to clear the subtitle', () => {
    expect(
      parseEChartsOption(`{
        title: { text: '账号状态', subtext: '共 9 个账号' },
        legend: { orient: 'vertical', left: 'left' },
        series: [{ type: 'pie', radius: ['30%', '55%'], data: [] }]
      }`)
    ).toMatchObject({
      legend: { orient: 'vertical', left: 'left', top: 60 },
      series: [{ type: 'pie', radius: ['30%', '55%'], center: ['50%', '53%'] }]
    })
  })

  it('normalizes the common subtitle alias to the ECharts subtext field', () => {
    const option = parseEChartsOption(`{
      title: { text: '各用户资源数量及占比', subtitle: '共 9 个用户 / 350 条资源' },
      legend: { orient: 'vertical', left: 'left' },
      series: [{ type: 'pie', data: [] }]
    }`)

    expect(option).toMatchObject({
      title: { text: '各用户资源数量及占比', subtext: '共 9 个用户 / 350 条资源' },
      legend: { orient: 'vertical', left: 'left', top: 60 }
    })
    expect(option.title).not.toHaveProperty('subtitle')
  })

  it('keeps the standard subtext when both title fields are present', () => {
    expect(
      parseEChartsOption(`{
        title: { text: '账号状态', subtext: '标准副标题', subtitle: '别名副标题' }
      }`)
    ).toEqual({
      title: { text: '账号状态', subtext: '标准副标题' }
    })
  })

  it('moves cartesian grids below the subtitle so the y-axis name stays clear', () => {
    expect(
      parseEChartsOption(`{
        title: { text: '各用户资源数量', subtitle: '共 9 个用户 / 350 条资源' },
        grid: { left: '3%', right: '6%', bottom: '3%', containLabel: true },
        xAxis: { type: 'category', data: ['管理员'] },
        yAxis: { type: 'value', name: '资源数' },
        series: [{ type: 'bar', data: [350] }]
      }`)
    ).toMatchObject({
      title: { text: '各用户资源数量', subtext: '共 9 个用户 / 350 条资源' },
      grid: { top: 96, left: '3%', right: '6%', bottom: '3%', containLabel: true }
    })
  })

  it('keeps a cartesian grid that is already below the title area', () => {
    expect(
      parseEChartsOption(`{
        title: { text: '各用户资源数量', subtext: '共 9 个用户 / 350 条资源' },
        grid: { top: 120, left: 40 },
        xAxis: { type: 'category', data: [] },
        yAxis: { type: 'value' }
      }`)
    ).toMatchObject({
      grid: { top: 120, left: 40 }
    })
  })

  it('reserves title space even when there is no subtitle', () => {
    expect(
      parseEChartsOption(`{
        title: { text: '各用户资源数量及占比' },
        legend: { orient: 'vertical', left: 'left', top: 'middle', data: ['test1'] },
        series: [{ type: 'pie', data: [] }]
      }`)
    ).toMatchObject({
      legend: { orient: 'vertical', left: 'left', top: 36 },
      series: [{ type: 'pie', center: ['50%', '53%'] }]
    })
  })

  it('keeps legends that are deliberately placed below the title', () => {
    expect(
      parseEChartsOption(`{
        title: { text: '账号状态' },
        legend: [
          { left: 'left', top: 120 },
          { right: 12, top: 'middle' },
          { bottom: 0 }
        ]
      }`)
    ).toMatchObject({
      legend: [{ left: 'left', top: 120 }, { right: 12, top: 'middle' }, { bottom: 0 }]
    })
  })

  it('keeps an explicitly configured pie center', () => {
    expect(
      parseEChartsOption(`{
        title: { text: '账号状态' },
        series: { type: 'pie', center: ['45%', '60%'], data: [] }
      }`)
    ).toMatchObject({
      series: { type: 'pie', center: ['45%', '60%'] }
    })
  })
})
