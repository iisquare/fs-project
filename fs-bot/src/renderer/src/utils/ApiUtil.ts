const FIELD_CODE = 'code'
const FIELD_MSG = 'message'
const FIELD_DATA = 'data'

const ApiUtil = {
  FIELD_CODE,
  FIELD_MSG,
  FIELD_DATA,

  result(code = 0, message: any = null, data: any = null) {
    if (message === null) {
      switch (code) {
        case 0:
          message = 'Succeed!'
          break
        case 403:
        case 9403:
          message = 'Forbidden!'
          break
        case 404:
        case 9404:
          message = 'Not Found!'
          break
        case 500:
        case 9500:
          message = 'Failed!'
          break
      }
    }
    return { [FIELD_CODE]: code, [FIELD_MSG]: message, [FIELD_DATA]: data }
  },

  failed(result: any) {
    if (result === null) return false
    return result instanceof Object ? result[FIELD_CODE] !== 0 : true
  },

  succeed(result: any) {
    return !ApiUtil.failed(result)
  },

  code(result: any) {
    return result[FIELD_CODE]
  },

  message(result: any) {
    return result[FIELD_MSG]
  },

  data(result: any) {
    return result[FIELD_DATA]
  }
}

export default ApiUtil
