import base from './Api'

/** 技能与技能版本：列表只给基础信息，包体由服务端下发 */
export default {
  /** 可用技能：已启用 + 授权命中，不含配置内容 */
  available(tips = {}) {
    return base.post('/skill/available', {}, tips)
  },
  /** 可用版本：某个技能下已启用的版本清单 */
  versions(skillId: number, tips = {}) {
    return base.post('/skillVersion/available', { skillId }, tips)
  }
}
