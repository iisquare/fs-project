<template>
  <page-view :avatar="avatar" :title="false">
    <div slot="headerContent">
      <div class="title">{{ timeFix }}，{{ user.name }}<span class="welcome-text">，欢迎光临</span></div>
      <div>技术中台、数据中台、AI中台、业务中台一体化服务平台</div>
    </div>
    <div slot="extra">
      <a-row class="more-info">
        <a-col :span="8">
          <head-info title="项目" content="56" :center="false" :bordered="false"/>
        </a-col>
        <a-col :span="8">
          <head-info title="团队排名" content="8/24" :center="false" :bordered="false"/>
        </a-col>
        <a-col :span="8">
          <head-info title="项目数" content="2,223" :center="false" />
        </a-col>
      </a-row>
    </div>
    <a-list :grid="{ gutter: 16, column: 3 }" :dataSource="menus">
      <a-list-item slot="renderItem" slot-scope="item" v-if="item.url !== '/'">
        <a-card :hoverable="true">
          <a-card-meta>
            <router-link slot="title" :to="item.url" :target="item.target">{{ item.name }}</router-link>
            <a-avatar class="card-avatar" slot="avatar" :icon="item.icon" size="large"/>
            <div class="meta-content" slot="description">{{ item.description }}</div>
          </a-card-meta>
          <template class="ant-card-actions" slot="actions">
            <router-link slot="title" :to="item.url" :target="item.target">进入</router-link>
          </template>
        </a-card>
      </a-list-item>
    </a-list>
  </page-view>
</template>

<script>
import { timeFix } from '@/utils/util'
import { mapState } from 'vuex'

import PageView from '@/views/frame/layout/page'
import HeadInfo from '@/components/tools/HeadInfo'
import { Radar } from '@/components'

export default {
  name: 'Workplace',
  components: {
    PageView,
    HeadInfo,
    Radar
  },
  data () {
    return {
      timeFix: timeFix(),
      avatar: '',
      user: {}
    }
  },
  computed: {
    ...mapState({
      menus: (state) => state.user.data.menu,
      userInfo: (state) => state.user.data.info
    })
  },
  created () {
    this.user = this.userInfo
    this.avatar = this.userInfo.avatar
  }
}
</script>
