import { format } from 'sql-formatter'

const SqlUtil = {
  format (sqlText: string, language: any = 'sql') {
    if (!sqlText) return ''
    return format(sqlText, { language })
  },
}

export default SqlUtil
