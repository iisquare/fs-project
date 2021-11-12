/**
 * Custom icon list
 * All icons are loaded here for easy management
 * @see https://vue.ant.design/components/icon/#Custom-Font-Icon
 *
 * 自定义图标加载表
 * 所有图标均从这里加载，方便管理
 */

const icons = {}
const context = require.context('./', true, /\.js$/)
context.keys().forEach(key => {
  if (key === './index.js') return
  const icon = context(key)
  Object.assign(icons, icon.default)
})

// const context = require.context('../assets/icons', false, /\.svg$/)
// const icons = Object.fromEntries(context.keys().map(key => {
//     const strs = key.slice(2, -4).split('-')
//     const str = strs.slice(0, 1).concat(strs.slice(1).map(i => i.slice(0, 1).toUpperCase() + i.slice(1))).join('')
//     const path = context.resolve(key).replace('./src', '@') + '?inline'
//     console.log(str, key, path)
//     return [str, import(path)]
// }))
// console.log(icons)

export default icons
