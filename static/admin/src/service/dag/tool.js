import base from './base'

export default {
  field (tips = {}) {
    return base.post('/tool/field', {}, tips)
  }
}
