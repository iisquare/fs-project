import { layout } from '../config'

export const blanks = [{
  path: '/dag/tool/property',
  meta: { title: '属性编辑器' },
  component: () => import(/* webpackChunkName: 'dag' */ '@/views/dag/tool/property')
}, {
  path: '/dag/tool/field',
  meta: { title: '字段编辑器' },
  component: () => import(/* webpackChunkName: 'dag' */ '@/views/dag/tool/field')
}, {
  path: '/dag/flow/model',
  meta: { title: '流程模型' },
  component: () => import(/* webpackChunkName: 'dag' */ '@/views/dag/flow/model')
}]

export const layouts = [{
  path: '/dag',
  meta: { title: '数据计算' },
  component: layout.route,
  children: [{
    path: '/dag/index/index',
    meta: { title: '工作面板' },
    component: layout.default
  }, {
    path: '/dag/plugin/list',
    meta: { title: '插件列表' },
    component: () => import(/* webpackChunkName: 'dag' */ '@/views/dag/plugin/list')
  }, {
    path: '/dag/node/list',
    meta: { title: '流程节点列表', parents: ['/dag/flow'] },
    component: () => import(/* webpackChunkName: 'dag' */ '@/views/dag/node/list')
  }, {
    path: '/dag/node/tree',
    meta: { title: '树形流程节点', parents: ['/dag/flow'] },
    component: () => import(/* webpackChunkName: 'dag' */ '@/views/dag/node/tree')
  }, {
    path: '/dag/flow/list',
    meta: { title: '流程列表' },
    component: () => import(/* webpackChunkName: 'dag' */ '@/views/dag/flow/list')
  }]
}]

export default { blanks, layouts }
