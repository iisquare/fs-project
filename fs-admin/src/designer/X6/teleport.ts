import * as X6VueShape from '@antv/x6-vue-shape'

/**
 * X6 Vue 形状渲染宿主。
 *
 * @antv/x6-vue-shape 的 getTeleport() 只在首次调用时返回宿主组件，之后调用返回 null，
 * 且宿主组件卸载时会把库内部引用清空。若宿主挂在设计器页面内，页面切换或热更新时
 * 新页面可能先挂载（拿到 null），旧宿主随后卸载，导致画布节点与拖拽 ghost 的内容
 * 都不再渲染。因此在应用根组件挂载一次，并在模块级缓存宿主组件定义，保证始终可用。
 */
let TeleportContainer: any = null

export const x6Teleport = () => {
  const teleport = X6VueShape.getTeleport()
  if (teleport) TeleportContainer = teleport
  return TeleportContainer
}

export default x6Teleport
