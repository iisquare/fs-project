const FormUtil = {
  copyToClipboard(text: string): Promise<void> {
    if (navigator.clipboard && window.isSecureContext) {
      return navigator.clipboard.writeText(text)
    }
    return new Promise((resolve, reject) => {
      try {
        const textarea = document.createElement('textarea')
        textarea.value = text
        textarea.style.position = 'fixed'
        textarea.style.opacity = '0'
        document.body.appendChild(textarea)
        textarea.select()
        const ok = document.execCommand('copy')
        document.body.removeChild(textarea)
        ok ? resolve() : reject(new Error('execCommand failed'))
      } catch (e) {
        reject(e)
      }
    })
  },
  download(url: string, params: Record<string, any> = {}) {
    const form = document.createElement('form')
    form.method = 'post'
    form.action = url
    form.target = '_blank'
    Object.keys(params).forEach((key) => {
      const input = document.createElement('input')
      input.type = 'hidden'
      input.name = key
      input.value = String(params[key])
      form.appendChild(input)
    })
    document.body.appendChild(form)
    form.submit()
    document.body.removeChild(form)
  },
}

export default FormUtil
