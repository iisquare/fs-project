<template>
  <div class="user-wrapper">
    <div class="content-box">
      <span class="action"><a-icon type="layout" @click="handleSettingPanel" /></span>
      <a-popover placement="bottomRight">
        <template slot="content">
          <a-list itemLayout="horizontal" :dataSource="menus">
            <a-list-item slot="renderItem" slot-scope="item">
              <a-list-item-meta :description="item.description">
                <router-link slot="title" :to="item.url" :target="item.target">{{ item.name }}</router-link>
                <a-avatar slot="avatar" :icon="item.icon" />
              </a-list-item-meta>
            </a-list-item>
          </a-list>
        </template>
        <span class="action"><a-icon type="appstore" /></span>
      </a-popover>
      <a-dropdown>
        <span class="action ant-dropdown-link user-dropdown-menu">
          <a-avatar class="avatar" size="small" icon="insurance"/>
          <span>{{ userInfo.name }}</span>
        </span>
        <a-menu slot="overlay" class="user-dropdown-menu-wrapper">
          <a-menu-item key="0">
            <router-link to="/account/profile">
              <a-icon type="user"/>
              <span>个人信息</span>
            </router-link>
          </a-menu-item>
          <a-menu-item key="1">
            <router-link to="/account/password">
              <a-icon type="setting"/>
              <span>修改密码</span>
            </router-link>
          </a-menu-item>
          <a-menu-divider/>
          <a-menu-item key="3">
            <a href="javascript:;" @click="handleLogout">
              <a-icon type="logout"/>
              <span>退出登录</span>
            </a>
          </a-menu-item>
        </a-menu>
      </a-dropdown>
    </div>
  </div>
</template>

<script>
import { mapState, mapActions } from 'vuex'
import userService from '@/service/member/user'

export default {
  name: 'UserMenu',
  computed: {
    ...mapState({
      menus: (state) => state.user.data.menu,
      userInfo: (state) => state.user.data.info,
      settingPanelVisible: state => state.app.settingPanelVisible
    })
  },
  methods: {
    ...mapActions([ 'toggleSettingPanel' ]),
    handleSettingPanel () {
      this.toggleSettingPanel(!this.visible)
    },
    handleLogout () {
      this.$confirm({
        title: '提示',
        content: '真的要注销登录吗 ?',
        onOk: () => {
          return userService.logout().then(() => {
            setTimeout(() => {
              window.location.reload()
            }, 16)
          })
        },
        onCancel () {
        }
      })
    }
  }
}
</script>

<style lang="less" scoped>
.item-app {
  a {
    color: rgba(0, 0, 0, 0.65);
    display: inline-block;
    font-size: 14px;
    margin-left: 5px;
  }
}
</style>
