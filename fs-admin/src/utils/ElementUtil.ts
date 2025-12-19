import { ElMessageBox } from "element-plus"

const ElementUtil = {
  async confirm (message: string, title = '操作提示', type: any = 'warning') {
    return ElMessageBox.confirm(message, title, { type })
  },
}

export default ElementUtil
