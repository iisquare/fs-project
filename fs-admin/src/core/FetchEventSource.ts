import api from '@/core/Api';
import { fetchEventSource } from '@microsoft/fetch-event-source';

class FetchEventSource {
  
  protected app: string
  protected uri: string
  protected method: string = 'POST'
  protected headers: any = {
    'Content-Type': 'application/json'
  }
  protected ctrl: any = null
  public running: boolean
  protected openCallback: Function = () => {} // 每次连接无论是否异常都会触发
  protected messageCallback: Function = (message: any) => {}
  protected errorCallback: Function = (error: any) => {}
  protected closeCallback: Function = () => {} // 每次结束无论是否异常都会触发

  constructor (app: string, uri: string) {
    this.app = app
    this.uri = uri
    this.running = false
  }

  setMethod(method: string): FetchEventSource {
    this.method = method
    return this
  }

  setHeaders(headers: Object): FetchEventSource {
    this.headers = headers
    return this
  }

  addHeaders(headers: Object): FetchEventSource {
    Object.assign(this.headers, headers)
    return this
  }

  onOpen(callback: Function): FetchEventSource {
    this.openCallback = callback
    return this
  }

  onMessage(callback: Function): FetchEventSource {
    this.messageCallback = callback
    return this
  }

  onError(callback: Function): FetchEventSource {
    this.errorCallback = callback
    return this
  }

  onClose(callback: Function): FetchEventSource {
    this.closeCallback = callback
    return this
  }

  abort(): FetchEventSource {
    this.ctrl && this.ctrl.abort('abort manually') // 手动结束
    return this
  }

  send(data: Object): boolean {
    if (this.running) return false
    const url = `${api.$axios.defaults.baseURL}/proxy/${this.method.toLowerCase()}SSE`
    this.ctrl = new AbortController()
    const _this = this
    fetchEventSource(url, {
      method: this.method,
      headers: this.headers,
      body: JSON.stringify({
        app: this.app,
        uri: this.uri,
        data: data
      }),
      signal: this.ctrl.signal,
      openWhenHidden: true,
      async onopen (response) {
        // 网络异常时不会触发，服务端异常时会触发但response.ok == false
        if (!response.ok) {
          _this.errorCallback(response) // 服务端执行异常
        }
      },
      onmessage (msg) {
        if (!msg.data || '[DONE]' === msg.data) return
        _this.messageCallback(msg)
      },
      onclose () {
        // 只有正常执行完成才会触发，手动结束不会触发
      },
      onerror (err) {
        throw err // 抛出异常，避免自动重新连接
      }
    }).then(r => {
      // 在open后只要没有执行异常都会触发
    }).catch(e => {
      // 网络异常时会触发，服务端异常时不会触发，捕获到执行异常时会触发
      this.errorCallback(e) // 网络连接异常或执行异常
    }).finally(() => {
      // 始终会触发
      this.ctrl = null
      this.closeCallback()
    })
    this.openCallback()
    return true
  }
}

export default FetchEventSource
