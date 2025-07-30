import DesignUtil from '@/utils/DesignUtil'

const config = {
}

const CanvasOptions = () => {
  return {}
}

const EdgeOptions = () => {
  return {}
}

const NodeOptions = () => {
  return {}
}

export default Object.assign(config, {
  canvas: {
    options: CanvasOptions, property: () => import('./CanvasProperty.vue')
  },
  edge: { options: EdgeOptions, property: () => import('./EdgeProperty.vue') },
  widgets: DesignUtil.widgets([{
    name: '默认分组',
    children: [{
      type: 'Node', label: '实体节点', title: '实体节点', icon: 'Notification',
      shape: 'kg-node', options: NodeOptions, property: () => import('./NodeProperty.vue')
    }]
  }]),
})
