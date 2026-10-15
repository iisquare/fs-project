import KnowledgeImageApi from '@renderer/api/agent/KnowledgeImageApi'
import ApiUtil from '@renderer/utils/ApiUtil'

/**
 * 知识库图片标识解析
 *
 * 正文里的图片以稳定标识保存（形如 ![说明](kb:图片标识)），展示时再按图片自身归属的知识库实时判权并签发地址：
 * - 签发地址带时效校验码且不落库，每次渲染都重新判定，因此知识库授权变更后历史消息里的图片同步不可见；
 * - 授权范围内的图片由服务端返回地址，其余（无权限、不存在、不属于该知识库）一律不返回，由渲染方回落默认图；
 * - 每条正文各自解析（调用方按标识去重），不做跨消息缓存，避免撤权后仍复用旧地址。
 */
const KnowledgeImageUtil = {
  /**
   * 批量解析图片标识为可展示地址
   * @param ids 正文中的图片标识，重复的标识在本地与服务端各自去重
   * @returns 标识 → 地址，缺失的标识由调用方使用默认图兜底
   */
  resolve(ids: string[]): Promise<Record<string, string>> {
    const list = Array.from(new Set((ids ?? []).map((id) => String(id ?? '')).filter((id) => !!id)))
    if (!list.length) return Promise.resolve({})
    // 渲染期的后台请求：失败不弹通知，统一按「未取得地址」回落默认图
    return KnowledgeImageApi.url({ ids: list }, { success: false, warning: false, error: false })
      .then((result: any) => ApiUtil.data(result) || {})
      .catch(() => ({}))
  }
}

export default KnowledgeImageUtil
