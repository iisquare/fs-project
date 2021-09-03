import CodeUtil from '@/utils/code'

const ADMIN_URL = process.env.VUE_APP_API_BASE_URL

export default {
  baseUrl: ADMIN_URL,
  blank (app, uri, data = {}) {
    const arg = { app, uri, data: CodeUtil.encodeBase64(JSON.stringify(data)) }
    const param = Object.keys(arg).map(key => key + '=' + encodeURIComponent(arg[key]))
    const url = this.baseUrl + '/proxy/getResponse?' + param.join('&')
    return url
  },
  ueditorAction (arg = {}) {
    const param = Object.keys(arg).map(key => key + '=' + encodeURIComponent(arg[key]))
    return ADMIN_URL + '/file/ueditor' + (param.length > 0 ? '?' : '') + param.join('&')
  },
  uploadAction (arg = {}) {
    const param = Object.keys(arg).map(key => key + '=' + encodeURIComponent(arg[key]))
    return ADMIN_URL + '/proxy/upload' + (param.length > 0 ? '?' : '') + param.join('&')
  },
  uploadData (app, uri, data = {}) {
    return { app, uri, data: CodeUtil.encodeBase64(JSON.stringify(data)) }
  },
  uploadChange (viewer, uploader, callback, loadingKey = 'uploading') {
    switch (uploader.file.status) {
      case 'uploading':
        viewer[loadingKey] = true
        break
      case 'done':
        viewer[loadingKey] = false
        const result = uploader.file.response
        if (result.code === 0) {
          viewer.$notification.success({ message: '状态：' + result.code, description: '消息:' + result.message })
          if (callback) callback(result)
        } else {
          viewer.$notification.warning({ message: '状态：' + result.code, description: '消息:' + result.message })
        }
        break
      case 'error':
        viewer[loadingKey] = false
        viewer.$notification.error({ message: '请求异常', description: `${uploader.file.name} file upload failed.` })
        break
    }
  },
  imageUrl (archive, file) {
    return new Promise((resolve, reject) => {
      const reader = new FileReader()
      const image = new Image()
      image.onload = () => resolve(`${archive.fileUrl}/image/${archive.id}-w_${image.width},h_${image.height}${archive.suffix}`)
      image.onerror = error => reject(error)
      reader.onload = () => { image.src = reader.result }
      reader.onerror = error => reject(error)
      reader.readAsDataURL(file.originFileObj)
    })
  }
}
