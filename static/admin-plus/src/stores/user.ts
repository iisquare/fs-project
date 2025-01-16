import { defineStore } from 'pinia'
import { ref } from 'vue'
import ApiUtil from '@/utils/ApiUtil'
import RbacApi from '@/api/admin/RbacApi'

export const useUserStore = defineStore('user', () => {
  const USER_DEFAULT_STATE = {
    id: 0,
    serial: '',
    name: '',
    description: '',
    createdIp: '',
    createdTime: 0,
    loginedIp: '',
    loginedTime: 0
  }
  const ready = ref(false)
  const info: any = ref(Object.assign({}, USER_DEFAULT_STATE))
  const menu: any = ref([])
  const resource: any = ref({})
  const apps: any = ref({}) // 用于缓存菜单的处理结果
  
  const reload = async () => {
    const result = await RbacApi.login()
    if (ApiUtil.succeed(result)) {
      ready.value = true
      const data = ApiUtil.data(result)
      Object.assign(info.value, data.info)
      menu.value = data.menu
      resource.value = data.resource
      apps.value = generateApps(menu.value)
    } else {
      ready.value = false
    }
    return { ready: ready.value, info: info.value }
  }

  const reset = (data: any = {}, isReady = true) => {
    ready.value = isReady
    Object.assign(info.value, data.info || USER_DEFAULT_STATE)
    menu.value = data.menu || []
    resource.value = data.resource || {}
    apps.value = generateApps(menu.value)
  }

  const generateApps = (menu: any) => {
    const apps: any = {}
    for (const key in menu) {
      const value: any = menu[key]
      const path = value.url.replace(/(^[/#]*)|([/#]*$)/, '')
      if (path.startsWith('[a-zA-Z]://')) continue
      const paths = path.split('/')
      if (paths.length === 2 || path === '') {
        apps['frame'] = key
      } else if (paths.length === 3) {
        apps[paths[0]] = key
      }
    }
    return apps
  }

  const hasPermit = (value: any) => {
    if (!value) return false
    if (value instanceof String) {
      value = [value]
    }
    for (const index in value) {
      if (resource.value[value[index]]) return true
    }
    return false
  }

  return { ready, info, menu, resource, apps, reload, reset, hasPermit }
})
