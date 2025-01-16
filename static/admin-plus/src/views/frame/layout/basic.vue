<script setup lang="ts">
import { computed, ref } from 'vue';
import * as ElementPlusIcons from '@element-plus/icons-vue';
import logoSVG from '@/assets/logo.svg';
import { useUserStore } from '@/stores/user';
import icons from '@/assets/icons';
import { ElMessageBox } from 'element-plus';
import RbacApi from '@/api/admin/RbacApi';
import { useRoute } from 'vue-router';

const user = useUserStore()
const route = useRoute()
const menuCollapse = ref(false)
const menus = computed(() => {
  const apps: any = user.apps
  const menus: any = user.menu
  const path = route.path.replace(/(^[/#]*)|([/#]*$)/, '')
  const paths = path.split('/')
  if (paths.length === 3) {
    return apps[paths[0]] ? menus[apps[paths[0]]].children : []
  } else {
    return apps['frame'] ? menus[apps['frame']].children : []
  }
})
const menuActive = computed(() => {
  const url = (function find (menus, path) {
    const map: any = {}
    for (const key in menus) {
      const menu = menus[key]
      map[menu.url] = menu
    }
    const keys = Object.keys(map).sort((a, b) => b.length - a.length)
    for (const key of keys) {
      if (!path.includes(key)) continue
      if (map[key].children && map[key].children.length > 0) {
        return find(map[key].children, path)
      } else {
        return map[key].id + ''
      }
    }
    return ''
  })(menus.value, route.path)
  return url
})
const breadcrumb = computed(() => {
  const result: any = []
  route.matched.forEach(item => {
    result.push(item)
  })
  return result
})
const handleLogout = () => {
  ElMessageBox.confirm('确定要退出登录吗？', '操作提示', { type: 'info', }).then(() => {
    RbacApi.logout().then(() => {
      setTimeout(() => {
        window.location.reload()
      }, 16)
    })
  }).catch(() => {})
}
</script>

<template>
  <el-container class="fs-layout">
    <el-aside :class="['fs-aside', menuCollapse && 'fs-aside-collapse', 'fs-menu-dark']">
      <el-container>
        <el-header class="logo flex-start">
          <router-link :to="{ path: '/' }" class="flex-start">
            <el-space>
              <img :src="logoSVG" />
              <span>FS Project</span>
            </el-space>
          </router-link>
        </el-header>
        <el-main>
          <el-menu popper-class="fs-menu-dark" :collapse="menuCollapse" :default-active="menuActive">
            <LayoutMenuItem :menus="menus" />
          </el-menu>
        </el-main>
      </el-container>
    </el-aside>
    <el-container>
      <el-header class="flex-between fs-header">
        <el-space>
          <el-icon @click="menuCollapse = !menuCollapse" class="fs-pointer">
            <ElementPlusIcons.Expand v-if="menuCollapse" /><ElementPlusIcons.Fold v-else />
          </el-icon>
          <el-breadcrumb :separator-icon="ElementPlusIcons.ArrowRight">
            <el-breadcrumb-item
              v-for="(item, index) in breadcrumb"
              :key="item.name"
              :to="item.meta.to">{{ item.meta.title }}</el-breadcrumb-item>
          </el-breadcrumb>
        </el-space>
        <el-space>
          <el-dropdown placement="bottom-end">
            <el-space class="fs-pointer">
              <el-icon :size="20"><ElementPlusIcons.Avatar /></el-icon>
              <span>{{ user.info.name }}</span>
            </el-space>
            <template #dropdown>
              <el-dropdown-menu class="dropdown">
                <el-dropdown-item>
                  <router-link to="/account/profile">
                    <el-icon><ElementPlusIcons.User /></el-icon>
                    <span>个人信息</span>
                  </router-link>
                </el-dropdown-item>
                <el-dropdown-item>
                  <router-link to="/account/password">
                    <el-icon><ElementPlusIcons.FirstAidKit /></el-icon>
                    <span>修改密码</span>
                  </router-link>
                </el-dropdown-item>
                <el-dropdown-item divided>
                  <a href="https://cn.vuejs.org/guide/introduction.html" target="_blank">
                    <el-icon></el-icon>
                    <span>Vue3</span>
                  </a>
                </el-dropdown-item>
                <el-dropdown-item>
                  <a href="https://pinia.vuejs.org/zh/introduction.html" target="_blank">
                    <el-icon></el-icon>
                    <span>Pinia</span>
                  </a>
                </el-dropdown-item>
                <el-dropdown-item>
                  <a href="https://router.vuejs.org/zh/introduction.html" target="_blank">
                    <el-icon></el-icon>
                    <span>Vue Router</span>
                  </a>
                </el-dropdown-item>
                <el-dropdown-item divided>
                  <a href="https://github.com/iisquare/fs-project" target="_blank">
                    <el-icon v-html="icons.layout.github"></el-icon>
                    <span>GitHub</span>
                  </a>
                </el-dropdown-item>
                <el-dropdown-item>
                  <a href="https://element-plus.org/zh-CN/component/overview.html" target="_blank">
                    <el-icon><ElementPlusIcons.ElementPlus /></el-icon>
                    <span>Element Plus</span>
                  </a>
                </el-dropdown-item>
                <el-dropdown-item divided>
                  <a href="javascript:;" @click="handleLogout">
                    <el-icon><ElementPlusIcons.SwitchButton /></el-icon>
                    <span>退出登录</span>
                  </a>
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </el-space>
      </el-header>
      <el-main :class="['fs-main', route.meta?.fit && 'fs-fit']">
        <RouterView />
      </el-main>
    </el-container>
  </el-container>
</template>

<style lang="scss" scoped>
.fs-aside {
  width: var(--fs-layout-aside-width);
  height: 100vh;
  overflow: hidden;
  transition: width 300ms;
  :deep(.el-main) {
    padding: 0;
    height: calc(100vh - var(--fs-layout-header-height));
    overflow-x: hidden;
    overflow-y: auto;
    .el-menu {
      border-right: none;
    }
  }
}
.fs-aside-collapse {
  width: var(--fs-layout-header-height);
  .logo {
    span {
      display: none;
    }
  }
}
:deep(.el-header) {
  height: var(--fs-layout-header-height);
  padding: 0;
  box-sizing: border-box;
}
.logo {
  padding-left: 16px;
  box-sizing: border-box;
  border-bottom: solid 1px #002140;
  a, a:hover {
    color: #fff;
  }
  img {
    width: 32px;
  }
  span {
    font-size: 20px;
    white-space: nowrap;
  }
}
.fs-header {
  padding: 0 15px;
  border-bottom: solid 1px var(--fs-layout-border-color);
}
.dropdown {
  a, a:visited {
    color: #000;
    width: 100%;
    @include flex-start();
  }
}
.fs-main {
  height: calc(100vh - var(--fs-layout-header-height));
  overflow: auto;
  background-color: rgb(247 250 252);
}
.fs-fit {
  padding: 0;
}
</style>
