<template>
  <section>
    <div class="fs-candidate-tags" @click="handleClick">
      <a-button type="link" icon="loading" v-if="loading">正在载入数据...</a-button>
      <template v-else>
        <a-tag v-for="tag in tags" :key="tag.id"><a-icon :type="tag.icon" :class="'fs-tag-icon fs-icon-' + tag.type" />{{ tag.label }}</a-tag>
        <a-button type="link" icon="plus" v-if="tags.length === 0">点击设置设置负责人</a-button>
      </template>
    </div>
    <a-modal title="候选人信息配置" v-model="visible" :width="800" :footer="null">
      <div class="fs-candidate-tags fs-candidate-editor" @click="handleClick">
        <a-tag v-for="tag in tags" :key="tag.id" closable @close="removeTag(tag)"><a-icon :type="tag.icon" :class="'fs-tag-icon fs-icon-' + tag.type" />{{ tag.label }}</a-tag>
      </div>
      <div class="fs-candidate-card">
        <a-tabs v-model="activeKey">
          <a-tab-pane key="user" tab="用户">
            <div class="fs-card-layout">
              <div class="fs-card-left">
                <div class="fs-card-title">已选用户</div>
                <div class="fs-card-content">
                  <ul>
                    <li v-for="tag in tags.filter(item => item.type === 'user')" :key="tag.id">
                      <a-tag closable @close="removeTag(tag)"><a-icon :type="tag.icon" :class="'fs-tag-icon fs-icon-' + tag.type" />{{ tag.label }}</a-tag>
                    </li>
                  </ul>
                </div>
              </div>
              <div class="fs-card-right">
                <div class="fs-card-title">
                  <a-input-search placeholder="用户名称" style="width: 200px" @search="search" :loading="loading" />
                </div>
                <div class="fs-card-content">
                  <a-table
                    :loading="loading"
                    :columns="columns"
                    :data-source="rows"
                    size="middle"
                    :pagination="false"
                    :rowSelection="selection"
                    :rowKey="record => record.id">
                  </a-table>
                </div>
              </div>
            </div>
          </a-tab-pane>
          <a-tab-pane key="role" tab="角色">
            <div class="fs-card-layout">
              <div class="fs-card-left">
                <div class="fs-card-title">已选角色</div>
                <div class="fs-card-content">
                  <ul>
                    <li v-for="tag in tags.filter(item => item.type === 'role')" :key="tag.id">
                      <a-tag closable @close="removeTag(tag)"><a-icon :type="tag.icon" :class="'fs-tag-icon fs-icon-' + tag.type" />{{ tag.label }}</a-tag>
                    </li>
                  </ul>
                </div>
              </div>
              <div class="fs-card-right">
                <div class="fs-card-title">
                  <a-input-search placeholder="角色名称" style="width: 200px" @search="search" :loading="loading" />
                </div>
                <div class="fs-card-content">
                  <a-table
                    :loading="loading"
                    :columns="columns"
                    :data-source="rows"
                    size="middle"
                    :pagination="false"
                    :rowSelection="selection"
                    :rowKey="record => record.id">
                  </a-table>
                </div>
              </div>
            </div>
          </a-tab-pane>
          <a-tab-pane key="variable" tab="变量">
            <div class="fs-card-layout">
              <div class="fs-card-left">
                <div class="fs-card-title">已选变量</div>
                <div class="fs-card-content">
                  <ul>
                    <li v-for="tag in tags.filter(item => item.type === 'variable')" :key="tag.id">
                      <a-tag closable @close="removeTag(tag)"><a-icon :type="tag.icon" :class="'fs-tag-icon fs-icon-' + tag.type" />{{ tag.label }}</a-tag>
                    </li>
                  </ul>
                </div>
              </div>
              <div class="fs-card-right">
                <div class="fs-card-title">
                  <a-input-search placeholder="变量名称" style="width: 200px" @search="search" :loading="loading" />
                </div>
                <div class="fs-card-content">
                  <a-table
                    :loading="loading"
                    :columns="columns"
                    :data-source="rows"
                    size="middle"
                    :pagination="false"
                    :rowSelection="selection"
                    :rowKey="record => record.id">
                  </a-table>
                </div>
              </div>
            </div>
          </a-tab-pane>
        </a-tabs>
      </div>
    </a-modal>
  </section>
</template>

<script>
import rbacService from '@/service/member/rbac'
import userService from '@/service/member/user'
import roleService from '@/service/member/role'

export default {
  name: 'UserTaskCandidate',
  props: {
    value: { type: String, required: true },
    bpmn: { type: Object, required: true },
    element: { type: Object, required: true },
    workflow: { type: Object, required: true }
  },
  data () {
    return {
      activeKey: 'user',
      idGenerator: 0,
      tags: [],
      volatile: 0, // 乐观锁
      loading: true,
      visible: false,
      icons: { user: 'user', role: 'team', variable: 'number' },
      variables: {
        ['$' + '{submitter}']: { id: '$' + '{submitter}', name: '发起人' }
      },
      columns: [
        { title: 'ID', dataIndex: 'id' },
        { title: '名称', dataIndex: 'name' }
      ],
      rows: []
    }
  },
  computed: {
    selection () {
      const selectedRowKeys = []
      this.tags.forEach(tag => {
        if (tag.type === this.activeKey) selectedRowKeys.push(tag.value)
      })
      return {
        selectedRowKeys,
        onChange: this.onSelectedChange
      }
    }
  },
  watch: {
    activeKey (key) {
      if (key === 'variable') {
        this.rows = Object.values(this.variables)
      } else {
        this.rows = []
      }
    },
    element: {
      handler () {
        this.loading = true
        const volatile = this.volatile = new Date().getTime()
        const tags = []
        const args = { userIds: [], roleIds: [] }
        const regexp = /^\$\{\w+\}$/
        this.value.split(',').forEach(tag => {
          if (tag.match(regexp)) {
            return tags.push({ id: this.idGenerator++, type: 'variable', icon: this.icons.variable, value: tag, label: '$-' + this.variables[tag]?.name ?? '未知变量' })
          }
          const strs = tag.split('-')
          if (strs.length !== 2) return false
          const id = Number.parseInt(strs[1])
          if (!Number.isInteger(id)) return false
          switch (strs[0]) {
            case 'user':
              args.userIds.push(id)
              tags.push({ id: this.idGenerator++, type: 'user', icon: this.icons.user, value: id, label: id + '-未知用户' })
              break
            case 'role':
              args.roleIds.push(id)
              tags.push({ id: this.idGenerator++, type: 'role', icon: this.icons.role, value: id, label: id + '-未知角色' })
              break
          }
        })
        rbacService.infos(args).then(result => {
          if (volatile !== this.volatile) return false
          if (result.code !== 0) {
            return this.$message.warning('载入候选人信息失败！')
          }
          tags.map(item => {
            const value = item.value + ''
            if (item.type === 'user' && result.data.users[value]) {
              item.label = value + '-' + result.data.users[value].name
            }
            if (item.type === 'role' && result.data.roles[value]) {
              item.label = value + '-' + result.data.roles[value].name
            }
            return item
          })
          this.tags = tags
          this.loading = false
        })
      },
      immediate: true
    },
    tags: {
      handler (tags) {
        const result = []
        tags.forEach(tag => {
          switch (tag.type) {
            case 'user':
            case 'role':
              result.push(`${tag.type}-${tag.value}`)
              break
            case 'variable':
              result.push(tag.value)
              break
          }
        })
        this.$emit('input', result.join(','))
      },
      deep: true
    }
  },
  methods: {
    onSelectedChange (selectedRowKeys, selectedRows) {
      const allMap = {}
      this.rows.forEach(row => { allMap[row.id] = row })
      const selectedMap = {}
      selectedRows.forEach(row => { selectedMap[row.id] = row })
      for (let index = this.tags.length - 1; index >= 0; index--) {
        const tag = this.tags[index]
        if (tag.type !== this.activeKey) continue
        if (selectedMap[tag.value]) { // 已选中
          delete selectedMap[tag.value]
          continue
        }
        if (!allMap[tag.value]) continue // 标签不在当前列表中
        this.tags.splice(index, 1) // 移除列表中未选中的标签
      }
      for (const key in selectedMap) { // 加入标签中不存在的选中元素
        const row = selectedMap[key]
        const label = (this.activeKey === 'variable' ? '$' : row.id) + '-' + row.name
        this.tags.push({ id: this.idGenerator++, type: this.activeKey, icon: this.icons[this.activeKey], value: row.id, label })
      }
    },
    search (value) {
      this.loading = true
      const _this = this
      const services = {
        user: userService,
        role: roleService,
        variable: {
          list (param) {
            const rows = Object.values(_this.variables).filter(item => item.name.indexOf(param.name) !== -1)
            return Promise.resolve({ code: 0, data: { rows, total: rows.length } })
          }
        }
      }
      services[this.activeKey].list({ name: value, pageSize: 6 }).then((result) => {
        if (result.code === 0) {
          this.rows = result.data.rows
        }
        this.loading = false
      })
    },
    handleClick () {
      if (!this.loading) this.visible = true
    },
    removeTag (tag) {
      for (const index in this.tags) {
        const item = this.tags[index]
        if (tag.id !== item.id) continue
        this.tags.splice(index, 1)
        return true
      }
      return false
    }
  }
}
</script>

<style lang="less">
.fs-candidate-tags {
  cursor: pointer;
  min-height: 100px;
  border: 1px solid #e8e8e8;
  padding: 10px 10px 30px 10px;
  .ant-btn {
    margin: 20px 0px 0px 60px;
  }
  .ant-tag {
    margin-bottom: 8px;
    padding-right: 10px;
  }
}
.fs-tag-icon {
  border-radius: 9px;
  padding: 3px;
  margin: 5px 5px 3px 3px;
}
.fs-icon-user {
  color: white;
  background-color: #0DB3A6;
}
.fs-icon-role {
  color: white;
  background-color: #5D9CEE;
}
.fs-icon-variable {
  color: white;
  background-color: #FA0;
}
.fs-candidate-editor {
  height: 80px;
  overflow-x: auto;
  padding-bottom: 2px;
}
.fs-candidate-card {
  margin-top: 10px;
  border: 1px solid #e8e8e8;
  .ant-tabs-bar {
    margin-bottom: 0px;
  }
  .fs-card-layout {
    width: 100%;
    height: 380px;
    .fs-card-left {
      width: 250px;
      height: 100%;
      overflow: auto;
      display: inline-block;
      vertical-align: top;
      border-right: 1px solid #e8e8e8;
      ul {
        position: relative;
        padding: 0;
        margin: 0;
      }
      li {
        padding: 5px;
      }
    }
    .fs-card-right {
      display: inline-block;
      vertical-align: top;
      width: calc(100% - 250px);
      height: 100%;
    }
    .fs-card-title {
        height: 45px;
        line-height: 45px;
        vertical-align: middle;
        padding: 0px 10px;
        border-bottom: 1px solid #e8e8e8;
    }
    .fs-card-content {
      overflow: auto;
      height: calc(100% - 45px);
    }
  }
}
</style>
