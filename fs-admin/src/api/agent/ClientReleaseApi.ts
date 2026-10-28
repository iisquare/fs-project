import base from './Api'

/**
 * 面向公开展示页的客户端发布信息。
 *
 * 这些接口不要求登录或后台资源权限，只返回已启用、已全量放量的安装包；
 * 管理端的版本维护仍走 ClientVersionApi。
 */
export default {
  releases (param: any = {}, tips = {}) {
    return base.post('/clientUpdate/releases', param, tips)
  },
  /**
   * 兼容尚未部署公开版本列表接口的环境：按系统检查当前最新版。
   * 该接口本身也是公开接口。
   */
  check (param: any, tips = {}) {
    return base.post('/clientUpdate/check', param, tips)
  }
}
