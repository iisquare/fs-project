import DesignUtil from '@/utils/DesignUtil'

const config = {
}

const CanvasOptions = () => {
  return {}
}

const EdgeOptions = () => {
  return { fields: [] }
}

const NodeOptions = () => {
  return { fields: [] }
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
  toolbars: [{
    type: 'hand', label: '拖动', icon: 'action.hand', selectable: true, selected: true, callback (toolbar: any, flow: any, event: any) { flow.panning() }
  }, {
    type: 'lasso', label: '框选', icon: 'action.lasso', selectable: true, callback (toolbar: any, flow: any, event: any) { flow.selecting() }
  }, {
    type: 'fit', label: '适合', icon: 'action.fit', callback (toolbar: any, flow: any, event: any) { flow.fitting() }
  }, {
    type: 'divider'
  }, {
    type: 'clean', label: '清空', icon: 'action.clean', callback (toolbar: any, flow: any, event: any) {
      flow.fromJSON()
      flow.options.onBlankClick()
    }
  }],
  fieldTypes: ['Integer', 'Float', 'String', 'List'],
  status: [], // 由后台服务补齐
})
