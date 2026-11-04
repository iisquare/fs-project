const DocxUtil = {
  base64ToArrayBuffer(base64: string): ArrayBuffer {
    const binary = window.atob(base64)
    const bytes = new Uint8Array(binary.length)
    for (let index = 0; index < binary.length; index++) {
      bytes[index] = binary.charCodeAt(index)
    }
    return bytes.buffer
  },
  base64ToBlob(base64: string, mime: string): Blob {
    return new Blob([this.base64ToArrayBuffer(base64)], { type: mime })
  },
  download(blob: Blob, fileName: string) {
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = fileName
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.setTimeout(() => URL.revokeObjectURL(url), 1000)
  },
}

export default DocxUtil
