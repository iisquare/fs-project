import { defineStore } from 'pinia'

class Item {

  key: any;
  loader: Function;
  ttl: number;
  date: Date;
  value: any;

  constructor (key: any, loader: Function, ttl = 0) {
    this.key = key
    this.loader = loader
    this.ttl = ttl
    this.date = new Date()
    this.value = null
  }

  isExpired () {
    if (this.ttl === 0) return false
    return new Date().getTime() - this.date.getTime() > this.ttl
  }

  load (withExpired: boolean) {
    if (withExpired && !this.isExpired()) return Promise.resolve(this.value)
    return this.loader().then((result: any) => {
      this.date = new Date()
      this.value = result
      return result
    })
  }

  static key (key: any, loader: any) {
    return key || loader
  }
}

export const useCacheStore = defineStore('cache', () => {
  const cache: any = {}
  /**
   * 对异步加载函数进行缓存
   * 提示：key=this或loader=() => {}用于构造唯一标识
   * useCacheStore().load(this, func()).then(result => {})
   * useCacheStore().load(null, () => func()).then(result => {})
   */
  const load = (key: any, loader: Function, ttl = 0) => {
    key = Item.key(key, loader)
    let item = cache[key]
    if (item) return item.load(true)
    item = cache[key] = new Item(key, loader, ttl)
    return item.load(false)
  }
  /**
   * 获取缓存
   * useCacheStore().get(this)
   */
  const get = (key: any, withExpired = true) => {
    const item = cache[key]
    return item ? item.load(withExpired) : null
  }
  /**
   * 设置缓存
   * useCacheStore().set(this, 'value')
   */
  const set = (key: any, value: any) => {
    const item = cache[key]
    if (!item) return false
    item.date = new Date()
    item.value = value
    return true
  }

  return { load, get, set }
})
