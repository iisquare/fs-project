<template>
  <div class="fs-layout-box">
    <div class="fs-layout-header">
      <a-space>
        <a-button icon="menu-unfold" @click="json2rows">解析</a-button>
        <a-button icon="menu-fold" @click="rows2json">生成</a-button>
        <a-button icon="rollback" @click="jsonCompress">压缩</a-button>
        <a-button icon="unordered-list" @click="jsonFormat">格式化</a-button>
      </a-space>
    </div>
    <div class="fs-layout-content">
      <div class="fs-layout-left">
        <a-textarea v-model="json" />
      </div>
      <div class="fs-layout-center">
        <div class="fs-layout-top">
          <a-space>
            <a-button icon="plus" @click="rowAdd">新增</a-button>
            <a-button icon="vertical-align-middle" @click="rowInsert">插入</a-button>
            <a-button icon="vertical-align-top" @click="rowUp">上移</a-button>
            <a-button icon="vertical-align-bottom" @click="rowDown">下移</a-button>
            <a-button icon="delete" @click="rowDelete">删除</a-button>
          </a-space>
        </div>
        <div class="fs-layout-main">
          <a-table
            class="fs-layout-table"
            bordered
            :rowKey="(record, index) => index"
            :pagination="false"
            :data-source="rows"
            :columns="columns"
            :customRow="customRow"
            :scroll="{ x: 'calc(100vh - 566px)', y: 'calc(100vh - 152px)' }"
            :rowSelection="rowSelection">
          </a-table>
        </div>
      </div>
      <div class="fs-layout-right">
        <a-tabs default-active-key="property">
          <a-tab-pane key="property" tab="属性">
            <a-form-model :model="row" labelAlign="left" :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
              <a-form-model-item label="名称" prop="name">
                <a-input v-model="row.name" auto-complete="on" placeholder="name"></a-input>
              </a-form-model-item>
              <a-form-model-item label="字段" prop="field">
                <a-input v-model="row.field" auto-complete="on" placeholder="field"></a-input>
              </a-form-model-item>
              <a-form-model-item label="默认值" prop="value">
                <a-input v-model="row.value" auto-complete="on" placeholder="value"></a-input>
              </a-form-model-item>
              <a-form-model-item label="分组名" prop="group">
                <a-input v-model="row.group" auto-complete="on" placeholder="group"></a-input>
              </a-form-model-item>
              <a-form-model-item label="占位符" prop="placeholder">
                <a-input v-model="row.placeholder" auto-complete="on" placeholder="placeholder"></a-input>
              </a-form-model-item>
              <a-form-model-item label="编辑器" prop="editor">
                <a-select v-model="row.editor" placeholder="editor">
                  <a-select-option v-for="(value, key) in config.editors" :key="key" :value="value.type">{{ value.type }} - {{ value.label }}</a-select-option>
                </a-select>
              </a-form-model-item>
            </a-form-model>
            <dag-tool-selector v-if="row.editor == 'selector'" v-model="row.options"></dag-tool-selector>
            <dag-tool-empty v-else v-model="row.options"></dag-tool-empty>
          </a-tab-pane>
        </a-tabs>
      </div>
    </div>
    <div class="fs-layout-footer">
      <a-icon type="bulb" theme="twoTone" />
    </div>
  </div>
</template>

<script>
import DagToolSelector from '@/views/dag/tool/selector'
import DagToolEmpty from '@/views/dag/tool/empty'

export default {
  components: { DagToolSelector, DagToolEmpty },
  data () {
    return {
      columns: [
        { title: 'name', dataIndex: 'name', width: 120 },
        { title: 'field', dataIndex: 'field', width: 120 },
        { title: 'value', dataIndex: 'value', width: 100 },
        { title: 'group', dataIndex: 'group', width: 100 },
        { title: 'editor', dataIndex: 'editor', width: 100 },
        { title: 'placeholder', dataIndex: 'placeholder', ellipsis: true }
      ],
      rowSelection: {
        type: 'radio',
        selectedRowKeys: [],
        onChange (selectedRowKeys, selectedRows) {
          this.selectedRowKeys = selectedRowKeys
        }
      },
      row: this.rowItem(),
      rows: [],
      json: '[]',
      config: {
        ready: true,
        editors: [{
          label: '分割线',
          type: 'divider' // default value will be ignored
        }, {
          label: '数值',
          type: 'number'
        }, {
          label: '单行文本',
          type: 'text'
        }, {
          label: '多行文本',
          type: 'textarea'
        }, {
          label: '复选框',
          type: 'checkbox' // boolean - true | number - 1
        }, {
          label: '选择器',
          type: 'selector' // default json like '["A", "B", "C"]'
        }]
      }
    }
  },
  watch: {
    'rowSelection.selectedRowKeys' (selectedRowKeys) {
      if (selectedRowKeys.length > 0) {
        this.row = this.rows[selectedRowKeys[0]]
      }
    }
  },
  methods: {
    json2rows () {
      try {
        const data = JSON.parse(this.json)
        if (!(data instanceof Array)) {
          this.$warning({ title: 'JSON格式异常', content: '必须为数组' })
          return false
        }
        this.rows = data
      } catch (e) {
        this.$error({ title: '解析JSON失败', content: e.message })
      }
    },
    rows2json () {
      this.json = JSON.stringify(this.rows)
    },
    jsonCompress () {
      try {
        const data = JSON.parse(this.json)
        this.json = JSON.stringify(data)
      } catch (e) {
        this.$error({ title: '解析JSON失败', content: e.message })
      }
    },
    jsonFormat () {
      try {
        const data = JSON.parse(this.json)
        this.json = JSON.stringify(data, null, 2)
      } catch (e) {
        this.$error({ title: '解析JSON失败', content: e.message })
      }
    },
    customRow (record, index) {
      const _this = this
      return {
        on: {
          click () {
            _this.rowSelection.selectedRowKeys = [index]
          }
        }
      }
    },
    rowItem () {
      return { name: '', field: '', value: '', group: '', editor: '', placeholder: '', options: {} }
    },
    rowSelectedIndex () {
      if (this.rowSelection.selectedRowKeys.length > 0) {
        return this.rowSelection.selectedRowKeys[0]
      }
      return -1
    },
    rowAdd () {
      this.rows.push(this.rowItem())
    },
    rowInsert () {
      const index = this.rowSelectedIndex()
      this.rows.splice(index === -1 ? index + 1 : index, 0, this.rowItem())
      if (index !== -1) {
        this.rowSelection.selectedRowKeys = [index + 1]
      }
    },
    rowUp () {
      const index = this.rowSelectedIndex()
      if (index < 1) {
        return false
      }
      const row = this.rows[index]
      this.$set(this.rows, index, this.rows[index - 1])
      this.$set(this.rows, index - 1, row)
      this.rowSelection.selectedRowKeys = [index - 1]
    },
    rowDown () {
      const index = this.rowSelectedIndex()
      if (index < 0 || index >= this.rows.length - 1) {
        return false
      }
      const row = this.rows[index]
      this.$set(this.rows, index, this.rows[index + 1])
      this.$set(this.rows, index + 1, row)
      this.rowSelection.selectedRowKeys = [index + 1]
    },
    rowDelete () {
      const index = this.rowSelectedIndex()
      if (index === -1) {
        return false
      }
      this.rows.splice(index, 1)
      this.rowSelection.selectedRowKeys = []
    }
  }
}
</script>

<style lang="less" scoped>
.fs-layout-box {
  height: 100vh;
  display: flex;
  flex-direction: column;
  .fs-layout-header {
    flex-grow: 0;
    padding: 2px 5px 2px 5px;
    border-bottom: solid 1px #cbcccc;
  }
  .fs-layout-content {
    flex-grow: 1;
    display: flex;
    flex-direction: row;
    overflow: hidden;
    .fs-layout-left {
      width: 260px;
      flex-grow: 0;
      padding: 2px 5px 2px 5px;
      border-right: solid 1px #cbcccc;
      textarea {
        width: 100%;
        height: 100%;
        resize: none;
      }
    }
    .fs-layout-center {
      flex-grow: 1;
      flex-direction: column;
      padding: 2px 5px 2px 5px;
      .fs-layout-top {
        flex-grow: 0;
        padding: 0px 0px 3px 0px;
      }
      .fs-layout-main {
        flex-grow: 1;
      }
    }
    .fs-layout-right {
      width: 300px;
      flex-grow: 0;
      border-left: solid 1px #cbcccc;
      & /deep/ .ant-tabs {
        height: 100%;
      }
      & /deep/ .ant-tabs-bar {
        margin: 0px;
      }
      & /deep/ .ant-tabs-content {
        height: calc(100% - 44px);
        overflow-y: scroll;
        padding: 10px 15px 10px 15px;
      }
      & /deep/ .ant-form-item {
        border-bottom: solid 1px #ececec;
        margin-bottom: 10px;
        padding-bottom: 10px;
      }
    }
  }
  .fs-layout-footer {
    padding: 2px 5px 2px 5px;
    flex-grow: 0;
    border-top: solid 1px #cbcccc;
  }
}
</style>
