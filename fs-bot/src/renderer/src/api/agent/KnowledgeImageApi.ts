import base from './Api'

/**
 * 知识库图片：图片本体由服务端文件服务输出，这里只按图片标识换取带时效的展示地址。
 * 地址不落库，每次渲染重新签发，因此知识库授权变更后历史消息里的图片同步不可见。
 */
export default {
  /**
   * 批量签发原图地址：{ ids: [...] } → { 标识: 地址 }
   * 不传 knowledgeId：由服务端按图片自身归属的知识库逐个判权（聊天历史没有知识库上下文）；
   * 无权限 / 不存在 / 不属于该知识库的图片不会出现在返回值中，由调用方用默认图兜底。
   */
  url(param: any, tips = {}) {
    return base.post('/knowledgeImage/url', param, tips)
  }
}
