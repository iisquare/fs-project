<template>
  <section>
    <a-card class="fs-layout">
      <template slot="title">
        <a-button icon="plus-circle" type="primary" @click="add">新增</a-button>
        <a-divider type="vertical" />
        <a-button icon="minus-circle" type="danger" @click="remove" :disabled="form === null">删除</a-button>
        <a-divider type="vertical" />
        <a-button icon="save" @click="submit" :loading="loading">保存</a-button>
      </template>
      <aside class="fs-layout-left">
        <div class="head"><h3>菜单列表</h3></div>
        <div class="body">
          <a-tree
            class="draggable-tree"
            :expandedKeys.sync="expandedKeys"
            :selectedKeys.sync="selectedKeys"
            draggable
            :tree-data="tree"
            @drop="onDrop"
            @select="onSelect"
          />
        </div>
      </aside>
      <section class="fs-layout-body">
        <a-form-model
          v-if="form"
          ref="form"
          :model="form"
          :rules="rules"
          :labelCol="{lg: {span: 7}, sm: {span: 7}}"
          :wrapperCol="{lg: {span: 10}, sm: {span: 17} }">
          <a-form-model-item label="临时标识">{{ form.key }}</a-form-model-item>
          <a-form-model-item label="菜单名称"><a-input v-model="form.title" /></a-form-model-item>
          <a-form-model-item label="提示标题"><a-input v-model="form.data.title" /></a-form-model-item>
          <a-form-model-item label="展示禁用"><a-switch v-model="form.data.disabled" /></a-form-model-item>
          <a-form-model-item label="打开方式">
            <a-select v-model="form.data.target" placeholder="请选择" :allowClear="true">
              <a-select-option v-for="(value, key) in targets" :key="key" :value="key">{{ value }}</a-select-option>
            </a-select>
          </a-form-model-item>
          <a-form-model-item label="链接地址"><a-input v-model="form.data.href" /></a-form-model-item>
          <a-form-model-item label="封面图片"><fs-input-image v-model="form.data.imageUrl" bucket="cms" /></a-form-model-item>
        </a-form-model>
        <a-empty v-else />
      </section>
    </a-card>
  </section>
</template>

<script>
import UIUtil from '@/utils/ui'
import settingService from '@/service/cms/setting'

export default {
  components: { FsInputImage: () => import('@/components/Upload/InputImage.vue') },
  data () {
    return {
      counter: 0,
      tree: [],
      loading: false,
      form: null,
      rules: {},
      targets: { _blank: '新页打开', _parent: '父级页面', _self: '当前页面', _top: '顶层页面' },
      expandedKeys: [],
      selectedKeys: [],
      settingKey: null
    }
  },
  watch: {
    '$route.path': {
      handler (path) {
        this.form = null
        const paths = path.split('/')
        this.settingKey = paths[paths.length - 1]
        this.load()
      },
      immediate: true
    }
  },
  methods: {
    itemById (tree, id) {
      for (const item of tree) {
        if (id === item.id) return item
        const child = this.itemById(item.children, id)
        if (child !== null) return child
      }
      return null
    },
    add () {
      const node = this.formatNode({})
      if (this.form === null) {
        this.tree.push(node)
      } else {
        this.form.children.push(node)
        if (this.expandedKeys.indexOf(this.form.key) === -1) {
          this.expandedKeys.push(this.form.key)
        }
      }
      this.form = node
      this.selectedKeys = [node.key]
    },
    remove () {
      UIUtil.treeRemove(this.tree, this.form)
      this.form = null
    },
    onSelect (selectedKeys, { selected, selectedNodes, node, event }) {
      this.$nextTick(() => {
        this.form = node.selected ? node.dataRef : null
      })
    },
    onDrop (node) {
      this.tree = UIUtil.treeDrop(this.tree, node)
    },
    formatNode (item) {
      item = this.formatted(item, true)
      return { key: item.id, title: item.name || item.id, data: item, children: [] }
    },
    formatTree (tree, expandedKeys) {
      const result = []
      if (!Array.isArray(tree)) return result
      tree.forEach(item => {
        const node = this.formatNode(item)
        node.children = this.formatTree(item.children, expandedKeys)
        expandedKeys.push(node.key)
        result.push(node)
      })
      return result
    },
    formatted (obj, useId) {
      const result = {
        name: obj.name || '',
        title: obj.title || '',
        href: obj.href || '',
        target: obj.target || '',
        disabled: !!obj.disabled,
        imageUrl: obj.imageUrl || ''
      }
      if (useId) {
        result.id = 'fs-' + ++this.counter
      }
      return result
    },
    generate (tree) {
      const result = []
      if (!Array.isArray(tree)) return result
      tree.forEach(item => {
        const node = this.formatted(Object.assign({}, item.data, { name: item.title }), false)
        node.children = this.generate(item.children)
        result.push(node)
      })
      return result
    },
    load () {
      if (!this.settingKey) return false
      this.loading = true
      settingService.load({ names: [this.settingKey] }).then(result => {
        if (result.code !== 0) return
        try {
          this.tree = this.formatTree(JSON.parse(result.data[this.settingKey]), this.expandedKeys = [])
        } catch (e) {
          this.$error({ title: '数据解析异常', content: e.message })
          this.tree = []
          this.expandedKeys = []
        }
        this.loading = false
      })
    },
    submit (e) {
      if (!this.settingKey || this.loading) return false
      this.loading = true
      const data = { [this.settingKey]: JSON.stringify(this.generate(this.tree)) }
      settingService.change({ data }, { success: true }).then(result => {
        this.loading = false
      })
    }
  }
}
</script>
<style lang="less" scoped>
.fs-layout {
  min-height: 300px;
  height: calc(100vh - 112px);
  overflow: hidden;
  & /deep/ .ant-card-body {
    padding: 0px;
    height: calc(100% - 64px);
  }
  .fs-layout-left {
    width: 360px;
    height: 100%;
    display: inline-block;
    border-right: 1px solid #e8e8e8;
    .head {
      height: 44px;
      line-height: 44px;
      padding: 0px 15px;
      border-bottom: solid 1px #e8e8e8;
    }
    .body {
      height: calc(100% - 44px);
      padding: 15px;
      overflow: auto;
    }
  }
  .fs-layout-body {
    height: 100%;
    width: calc(100% - 360px);
    display: inline-block;
    vertical-align: top;
    padding: 15px;
    overflow: auto;
    & /deep/ .ant-empty {
      margin-top: 150px;
    }
  }
}
</style>
