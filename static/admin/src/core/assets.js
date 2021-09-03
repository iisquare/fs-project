const Assets = {
  staticUrl: process.env.VUE_APP_STATIC_URL,
  httpPrefix: /^(https?:)?\/\//,
  css (uri) {
    return this.staticUrl + uri
  },
  js (uri) {
    return this.staticUrl + uri
  },
  url (uri) {
    if (uri.match(this.httpPrefix)) return uri
    return this.staticUrl + uri
  },
  components: {
    css: {
      props: {
        href: { type: String, required: true }
      },
      render (createElement) {
        return createElement('link', { attrs: { rel: 'stylesheet', href: Assets.url(this.href) } })
      }
    },
    js: {
      props: {
        src: { type: String, required: true }
      },
      render (createElement) {
        return createElement('script', { attrs: { type: 'text/javascript', src: Assets.url(this.src) } })
      }
    }
  }
}

Assets.install = (Vue, options) => {
  Vue.prototype.$assets = Assets
}

export default Assets
