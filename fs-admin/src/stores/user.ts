import { defineStore } from 'pinia'
import { ref } from 'vue'
import ApiUtil from '@/utils/ApiUtil'
import RbacApi from '@/api/admin/RbacApi'
import DataUtil from '@/utils/DataUtil'

export const useUserStore = defineStore('user', () => {
  const USER_DEFAULT_STATE = {
    id: 0,
    serial: '',
    name: '',
    email: '',
    phone: '',
    description: '',
    createdIp: '',
    createdTime: 0,
    loginIp: '',
    loginTime: 0,
    token: '', // 用户登录标识
  }
  const ready = ref(false)
  const info: any = ref(Object.assign({}, USER_DEFAULT_STATE))
  const menu: any = ref([])
  const resource: any = ref({})
  const apps: any = ref({}) // 用于缓存菜单的处理结果
  
  const reload = async () => {
    await RbacApi.login().then(result => {
      ready.value = true
      const data = ApiUtil.data(result)
      Object.assign(info.value, data.info)
      menu.value = data.menu
      resource.value = data.resource
      apps.value = generateApps(menu.value)
    }).catch(() => {
      ready.value = false
    })
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
    if (!DataUtil.isArray(value)) value = [value]
    for (const index in value) {
      if (resource.value[value[index]]) return true
    }
    return false
  }

  return { ready, info, menu, resource, apps, reload, reset, hasPermit }
})
