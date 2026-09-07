/**
 * Maintain 维护接口
 *
 * catalog：重新加载 Trino 目录
 * dataset：删除数据集 Schema 后重建所有视图
 * 两者均返回 SSE 接口描述对象，供 FormMaintain 等流式任务组件使用。
 */
export default {
  catalog(params: any = {}) {
    return { app: 'bi', uri: '/maintain/catalog', method: 'POST', params }
  },
  dataset(params: any = {}) {
    return { app: 'bi', uri: '/maintain/dataset', method: 'POST', params }
  },
}
