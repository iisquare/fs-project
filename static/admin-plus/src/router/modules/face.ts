import { layout } from '../config'

export const blanks = []

export const layouts = [{
  path: '/face',
  meta: { title: '人脸识别' },
  children: [{
    path: '/face/index/index',
    meta: { title: '工作面板' },
    component: layout.default
  }]
}]

export default { blanks, layouts }
