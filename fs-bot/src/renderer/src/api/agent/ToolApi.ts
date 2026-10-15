import base from './Api'

/**
 * 工具与方法：桌面端只做「选择与调用」，不下发工具的请求头与查询参数，
 * 调用统一由服务端按已保存配置发起，密钥不落到客户端。
 */
export default {
  /** 方法检索：按关键词分页返回可选方法（含参数明细），不含工具凭据 */
  methodList(param: any, tips = {}) {
    return base.post('/tool/methodList', param, tips)
  },
  /** 方法调用：服务端按工具配置发起真实调用并返回结果 */
  test(param: any, tips = {}) {
    return base.post('/tool/test', param, tips)
  }
}
