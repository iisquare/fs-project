export const formatNumber = (value: any) => {
  const number = Number(value)
  if (!Number.isFinite(number)) return '--'
  if (Math.abs(number) >= 100000000) return `${(number / 100000000).toFixed(2)}亿`
  if (Math.abs(number) >= 10000) return `${(number / 10000).toFixed(2)}万`
  if (Math.abs(number) >= 1000) return number.toLocaleString('zh-CN', { maximumFractionDigits: 2 })
  return number.toLocaleString('zh-CN', { maximumFractionDigits: 2 })
}
