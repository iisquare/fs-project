<template>
  <section>
    <a-layout-header class="draw-header">
      <a-button type="primary" @click="save()" :loading="loading">保存</a-button>
      <a-divider type="vertical" />
      <a-button type="primary" @click="refresh()" :loading="loading">刷新节点</a-button>
    </a-layout-header>
    <a-layout id="draw-layout">
      <a-layout-sider class="draw-left" theme="light" collapsible>
        <a-card title="节点">
          <a-input-search style="margin-bottom: 8px" placeholder="Search" @change="onChange" />
          <a-tree
            @expand="onExpand"
            :expandedKeys="expandedKeys"
            :autoExpandParent="autoExpandParent"
            :treeData="gData"
          >
            <template slot="title" slot-scope="{title}">
              <span v-if="title.indexOf(searchValue) > -1">
                {{ title.substr(0, title.indexOf(searchValue)) }}
                <span style="color: #f50">{{ searchValue }}</span>
                {{ title.substr(title.indexOf(searchValue) + searchValue.length) }}
              </span>
              <span v-else>{{ title }}</span>
            </template>
          </a-tree>
        </a-card>
      </a-layout-sider>
      <a-layout-content class="draw-content">
        <a-card title="流程"></a-card>
      </a-layout-content>
      <a-layout-sider class="draw-right" theme="light">
        <a-card title="属性">
          <a-table :columns="propertyColumns" :dataSource="propertyData" :bordered="true" size="small">
          </a-table>
        </a-card>
      </a-layout-sider>
    </a-layout>
    <a-layout-footer class="draw-footer">Footer</a-layout-footer>
  </section>
</template>

<script>
import flowService from '@/service/flink/flow'

const x = 3
const y = 2
const z = 1
const gData = []

const generateData = (_level, _preKey, _tns) => {
  const preKey = _preKey || '0'
  const tns = _tns || gData

  const children = []
  for (let i = 0; i < x; i++) {
    const key = `${preKey}-${i}`
    tns.push({ title: key, key, scopedSlots: { title: 'title' } })
    if (i < y) {
      children.push(key)
    }
  }
  if (_level < 0) {
    return tns
  }
  const level = _level - 1
  children.forEach((key, index) => {
    tns[index].children = []
    return generateData(level, key, tns[index].children)
  })
}
generateData(z)

const dataList = []
const generateList = data => {
  for (let i = 0; i < data.length; i++) {
    const node = data[i]
    const key = node.key
    dataList.push({ key, title: key })
    if (node.children) {
      generateList(node.children)
    }
  }
}
generateList(gData)

const getParentKey = (key, tree) => {
  let parentKey
  for (let i = 0; i < tree.length; i++) {
    const node = tree[i]
    if (node.children) {
      if (node.children.some(item => item.key === key)) {
        parentKey = node.key
      } else if (getParentKey(key, node.children)) {
        parentKey = getParentKey(key, node.children)
      }
    }
  }
  return parentKey
}

export default {
  data () {
    return {
      loading: false,
      expandedKeys: [],
      searchValue: '',
      autoExpandParent: true,
      gData,
      propertyColumns: [
        { title: '名称', dataIndex: 'name' },
        { title: '值', dataIndex: 'value' }
      ],
      propertyData: [
      ]
    }
  },
  methods: {
    onExpand (expandedKeys) {
      this.expandedKeys = expandedKeys
      this.autoExpandParent = false
    },
    onChange (e) {
      const value = e.target.value
      const expandedKeys = dataList
        .map(item => {
          if (item.key.indexOf(value) > -1) {
            return getParentKey(item.key, gData)
          }
          return null
        })
        .filter((item, i, self) => item && self.indexOf(item) === i)
      Object.assign(this, {
        expandedKeys,
        searchValue: value,
        autoExpandParent: true
      })
    },
    load () {
      flowService.info({ id: this.$route.query.id }).then(result => {
        if (result.code === 0) {
        } else if (result.code === 404) {
          this.$router.push('/exception/404')
        } else {
          this.$router.push('/exception/500')
        }
      })
    },
    submit (e) {
      e.preventDefault()
      this.loading = true
      const data = {}
      flowService.save(data, { success: true }).then(result => {
        this.loading = false
      })
    }
  },
  mounted () {
    this.load()
  }
}
</script>
<style lang="less" scoped>
#draw-layout {
  .draw-header {}
  .draw-left {}
  .draw-content {}
  .draw-right {}
  .draw-footer {}
}
</style>
