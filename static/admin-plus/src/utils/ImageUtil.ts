const ImageUtil = {
  load(url: any) {
    return new Promise((resolve, reject) => {
      const image = new Image()
      image.crossOrigin = 'Anonymous'
      image.onload = function () {
        resolve(image)
      }
      image.onerror = function (e: any) {
        reject(e)
      }
      image.src = url
    })
  },
  fileReader(file: any) {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = function (event: any) {
        resolve(event)
      }
      reader.onerror = function (e: any) {
        reject(e)
      }
      reader.readAsDataURL(file)
    })
  },
  download(name: string, base64: string) {
      var byteCharacters = atob(
        base64.replace(/^data:image\/(png|jpeg|jpg);base64,/, "")
      );
      var subffix = base64.substring(0, base64.indexOf(';base64,')).replace('data:image/', '')
      var byteNumbers = new Array(byteCharacters.length);
      for (var i = 0; i < byteCharacters.length; i++) {
        byteNumbers[i] = byteCharacters.charCodeAt(i);
      }
      var byteArray = new Uint8Array(byteNumbers);
      var blob = new Blob([byteArray], {
        type: undefined,
      });
      var aLink = document.createElement("a");
      aLink.download = name + '.' + subffix;
      aLink.href = URL.createObjectURL(blob);
      aLink.click();
  },
  async loading (img: any, loading: any, type = 'image/jpeg') {
    const icon: any = await this.load(loading)
    const canvas = document.createElement('canvas')
    canvas.width = img.width
    canvas.height = img.height
    const ctx: any = canvas.getContext('2d')
    ctx.drawImage(img, 0, 0)
    if (icon.width <= canvas.width && icon.height <= canvas.height) {
      ctx.drawImage(icon, (canvas.width - icon.width) / 2, (canvas.height - icon.height) / 2)
    }
    const base64 = canvas.toDataURL(type)
    return base64
  },
  img2base64 (img: any, type = 'image/jpeg') {
    const canvas = document.createElement('canvas')
    canvas.width = img.width
    canvas.height = img.height
    const ctx: any = canvas.getContext('2d')
    ctx.drawImage(img, 0, 0)
    const base64 = canvas.toDataURL(type)
    return base64
  },
  url2base64 (url: any) {
    return new Promise((resolve, reject) => {
      if (url.startsWith('data:')) {
        resolve(url)
      } else {
        const xhr = new XMLHttpRequest()
        xhr.onload = function() {
          const reader = new FileReader()
          reader.onloadend = function() {
            resolve(reader.result);
          }
          reader.onerror = function (e: any) {
            reject(e)
          }
          reader.readAsDataURL(xhr.response)
        }
        xhr.onerror = function (e) {
          reject(e)
        }
        xhr.open('GET', url)
        xhr.setRequestHeader('Access-Control-Allow-Origin', '*')
        xhr.responseType = 'blob'
        xhr.send()
      }
    })
  },
  base2blob (base64: any, type = 'image/jpeg') {
    const bytes = window.atob(base64.split(',')[1])
    var ab = new ArrayBuffer(bytes.length);
    var ia = new Uint8Array(ab)
    for (var i = 0; i < bytes.length; i++) {
        ia[i] = bytes.charCodeAt(i)
    }
    return new Blob([ab], { type })
  }
}

export default ImageUtil
