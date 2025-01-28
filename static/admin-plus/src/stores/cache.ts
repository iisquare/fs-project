import { ref } from 'vue'
import { defineStore } from 'pinia'

class Item {

  key: any;
  loader: any;
  ttl: any;
  date: any;
  value: any;

  constructor (key: any, loader: any, ttl = 0) {
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

  load (withExpired: any) {
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
  const cache: any = ref({})
  /**
   * 对异步加载函数进行缓存
   * 提示：key=this或loader=() => {}用于构造唯一标识
   * useCacheStore().load(this, func()).then(result => {})
   * useCacheStore().load(null, () => func()).then(result => {})
   */
  const load = (key: any, loader: any, ttl: any = 0) => {
    key = Item.key(key, loader)
    let item = cache.value[key]
    if (item) return item.load(true)
    item = cache.value[key] = new Item(key, loader, ttl)
    return item.load(false)
  }
  /**
   * 获取缓存
   * useCacheStore().get(this)
   */
  const get = (key: any) => {
    const item = cache.value[key]
    if (!item) return null
    if (item.isExpired()) return false
    return item.value
  }
  /**
   * 设置缓存
   * useCacheStore().set(this, 'value')
   */
  const set = (key: any, value: any, ttl: any = 0) => {
    const item = new Item(key, null, ttl)
    item.value = value
    cache.value[key] = item
    return item
  }

  const reload = (key: any, loader: any, ttl: any = 0) => {
    key = Item.key(key, loader)
    const item = new Item(key, loader, ttl)
    cache.value[key] = item
  }

  return { load, get, set, reload }
})
