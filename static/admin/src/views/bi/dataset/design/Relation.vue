<template>
  <div class="fs-relation-layout">
    <div class="fs-relation-left">
      <a-card class="fs-ui-top">
        <a-form-model>
          <service-auto-complete :search="sourceService.list" v-model="sourceSelected" placeholder="检索选择数据源" :loading="loading" />
        </a-form-model>
      </a-card>
      <a-card class="fs-ui-content" title="数据表" :loading="loading">
        <ul>
          <li v-for="table in sourceTables" :key="table.name">
            <a-icon class="icon" :component="icons.biTable" />
            <span @dragstart="ev => dragTable = table" @dragend="ev => dragTable = null" draggable="true">{{ table.name }}</span>
          </li>
        </ul>
        <a-empty v-if="Object.values(sourceTables).length === 0" description="请选择有效数据源" />
      </a-card>
    </div>
    <div class="fs-relation-right">
      <a-card>
        <template slot="title">
          <a-form-model layout="inline" style="float: left;">
            <a-form-model-item label="宽度"><a-input-number v-model="canvas.width" :min="100" /></a-form-model-item>
            <a-form-model-item label="高度"><a-input-number v-model="canvas.height" :min="100" /></a-form-model-item>
          </a-form-model>
          <a-space style="float: right; padding-top: 2px;">
            <a-button icon="history" type="danger" @click="clear" :loading="loading">清空画布</a-button>
          </a-space>
        </template>
        <div class="fs-ui-left">
          <div
            ref="canvas"
            id="fs-layout-canvas"
            @click="triggerCanvas"
            @dragover="canvasDragOver"
            @drop="canvasDrop"
            :style="{ width: canvas.width + 'px', height: canvas.height + 'px', top: canvas.top + 'px' }"
          >
            <div
              :class="['fs-layout-item', activeItem === item && 'fs-layout-selected']"
              :id="item.id"
              :key="item.id"
              :title="item.table"
              @click.stop="triggerCanvasItem(item)"
              @contextmenu="ev => handleContextMenu(ev, item)"
              :style="{ left: item.x + 'px', top:item.y + 'px' }"
              v-for="item in draw.items">
              <a-icon class="icon" :component="icons.biTable" />
              <span>{{ item.name }}</span>
            </div>
          </div>
        </div>
        <div class="fs-ui-right">
          <a-table
            :columns="columns"
            :dataSource="rows"
            :pagination="false"
            :scroll="{ y: 'calc(100vh - 316px)' }"
            :rowKey="(record, index) => index">
            <span slot="icon" slot-scope="text, record">
              <a-icon class="icon" :component="icons['bi' + record.format]" />
            </span>
          </a-table>
        </div>
      </a-card>
    </div>
    <!--编辑数据-->
    <a-modal title="编辑数据" v-model="formVisible" :confirmLoading="loading" :maskClosable="false" @ok="submit">
      <a-form-model ref="form" :model="form" :rules="rules" :label-col="{ span: 5 }" :wrapper-col="{ span: 18 }">
        <a-form-model-item label="名称" prop="name">
          <a-input v-model="form.name" auto-complete="on"></a-input>
        </a-form-model-item>
        <a-form-model-item label="表名" prop="table">
          <a-input v-model="form.table" auto-complete="on"></a-input>
        </a-form-model-item>
        <a-form-model-item label="自定义查询">
          <a-textarea v-model="form.dsl" placeholder="为空时默认查询全表" />
        </a-form-model-item>
      </a-form-model>
    </a-modal>
    <!--关联条件-->
    <a-modal
      title="关联条件"
      v-model="relationVisible"
      :confirmLoading="loading"
      :maskClosable="false"
      cancelText="关闭"
      okText="删除"
      okType="danger"
      :width="1200"
      @ok="onDeleteConnection">
      <query-filter v-model="filter" :fields="fields" :volatile="relationConnectionInfo" />
    </a-modal>
  </div>
</template>

<script>
import icons from '@/assets/icons'
import sourceService from '@/service/bi/source'
import Draw from './draw'
import MenuUtil from '@/utils/menu'

export default {
  name: 'Relation',
  components: {
    QueryFilter: () => import('@/components/Service/QueryFilter'),
    ServiceAutoComplete: () => import('@/components/Service/AutoComplete')
  },
  props: {
    value: { type: Object, required: true }
  },
  data () {
    return {
      icons,
      sourceService,
      sourceSelected: undefined,
      loading: false,
      sourceTables: {},
      canvas: {
        width: 300,
        height: 300,
        top: 0
      },
      activeItem: null,
      dragTable: null,
      draw: new Draw('#fs-layout-canvas', this.onClickConnection),
      columns: [
        { title: '', scopedSlots: { customRender: 'icon' }, width: 46 },
        { title: '字段', dataIndex: 'name' },
        { title: '类型', dataIndex: 'type' }
      ],
      form: {},
      formVisible: false,
      rules: {
        name: [{ required: true, message: '名称不能为空', trigger: 'blur' }],
        table: [{ required: true, message: '表名不能为空', trigger: 'blur' }]
      },
      relationVisible: false,
      relationConnectionInfo: null
    }
  },
  computed: {
    rows () {
      if (!this.activeItem) return []
      return Object.values(this.activeItem.columns)
    },
    filter: {
      get () {
        const relation = this.draw.relation(this.relationConnectionInfo)
        return relation ? relation.filter : []
      },
      set (val) {
        const relation = this.draw.relation(this.relationConnectionInfo)
        if (relation) relation.filter = val
      }
    },
    fields () {
      return this.draw.fields()
    }
  },
  watch: {
    sourceSelected () {
      this.schema()
    },
    'canvas.height': {
      handler () {
        this.resizeCanvas()
      },
      deep: true
    }
  },
  methods: {
    onDeleteConnection () {
      this.draw.deleteConnection(this.relationConnectionInfo)
      this.relationConnectionInfo = null
      this.relationVisible = false
    },
    onClickConnection (connInfo, originalEvent) {
      this.relationConnectionInfo = connInfo
      this.relationVisible = true
    },
    canvasDragOver (ev) {
      if (this.dragTable) ev.preventDefault()
    },
    canvasDrop (ev) {
      const item = this.draw.generateItem(this.sourceSelected, this.dragTable, ev)
      this.draw.addItem(item)
      this.triggerCanvasItem(item)
    },
    resizeCanvas () {
      if (!this.$refs.canvas) return false
      const top = Math.max(0, this.$refs.canvas.parentElement.offsetHeight - this.canvas.height - 30)
      this.canvas.top = top / 2
      return true
    },
    triggerCanvas () {
      if (this.activeItem === null) return true
      this.activeItem = null
    },
    triggerCanvasItem (item) {
      if (this.activeItem === item) return true
      this.activeItem = item
    },
    handleContextMenu (ev, item) {
      MenuUtil.context(ev, [
        { key: 'edit', icon: 'edit', title: '编辑数据' },
        { key: 'delete', icon: 'delete', title: '删除节点' }
      ], menu => {
        switch (menu.key) {
          case 'edit':
            this.form = {
              item,
              name: item.name,
              table: item.table,
              dsl: item.dsl
            }
            this.formVisible = true
            break
          case 'delete':
            if (this.activeItem === item) this.activeItem = null
            this.relationConnectionInfo = null
            return this.draw.removeItem(item)
          default:
            return false
        }
      })
    },
    clear () {
      this.loading = true
      this.activeItem = null
      this.draw.clear()
      this.loading = false
    },
    schema () {
      if (!this.sourceSelected) {
        this.sourceTables = {}
        return true
      }
      if (this.loading) return false
      this.loading = true
      sourceService.schema({ id: this.sourceSelected }).then(result => {
        this.sourceTables = result.code === 0 ? result.data : {}
        this.loading = false
      })
    },
    submit () {
      this.$refs.form.validate(valid => {
        if (!valid || this.loading) return false
        this.loading = true
        const param = {
          id: this.form.item.sourceId,
          table: this.form.table,
          dsl: this.form.dsl
        }
        sourceService.schema(param).then(result => {
          if (result.code === 0) {
            Object.assign(this.form.item, {
              name: this.form.name,
              table: this.form.table,
              dsl: this.form.dsl,
              columns: result.data[this.form.table].columns
            })
            this.formVisible = false
          }
          this.loading = false
        })
      })
    },
    reset () {
      this.draw.clear()
      if (Object.values(this.value).length !== 0) {
        Object.assign(this.canvas, {
          width: this.value.width,
          height: this.value.height
        })
        this.value.items.forEach(item => this.draw.addItem(item))
        this.draw.addRelations(this.value.relations)
      }
      this.resizeCanvas()
      return true
    },
    collect () {
      return Object.assign({}, this.canvas, this.draw.collect())
    }
  },
  mounted () {
    window.onresize = () => { this.resizeCanvas() }
    this.reset()
  },
  updated () {
    this.draw.repaint()
  },
  destroyed () {
    this.$emit('input', this.collect())
  }
}
</script>

<style lang="less" scoped>
#fs-layout-canvas {
  margin: 15px auto;
  position: relative;
  min-width: 100px;
  min-height: 100px;
  background: url(../../../../assets/grid.gif) white;
  & /deep/ .jtk-connector {
    cursor: pointer;
  }
  .fs-layout-item {
    position: absolute;
    width: 125px;
    font-size: 12px;
    display: block;
    line-height: 26px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    background: #fff;
    border: 1px solid #d9d9d9;
    padding: 3px 10px;
    cursor: move;
    border-radius: 3px;
    content: attr(title);
    .icon {
      margin-right: 6px;
      font-size: 14px;
    }
    &:hover {
      border: 1px solid #409eff;
    }
  }
  .fs-layout-selected {
    color: #409eff;
  }
}
.fs-relation-layout {
  width: 100%;
  height: calc(100vh - 170px);
  .fs-relation-left {
    display: inline-block;
    width: 350px;
    margin-right: 20px;
    .fs-ui-top {
      margin-bottom: 20px;
      & /deep/ .ant-card-body {
        padding: 21px 24px;
      }
    }
    .fs-ui-content {
      & /deep/ .ant-card-body {
        padding: 0px;
        height: calc(100vh - 340px);
        overflow-y: auto;
        overflow-x: hidden;
      }
      li {
        font-size: 12px;
        display: block;
        line-height: 26px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
        padding: 5px 2px;
        content: attr(title);
        &:hover {
          color: #409eff;
        }
        span {
          cursor: move;
        }
        .icon {
          margin-right: 6px;
          font-size: 14px;
        }
      }
    }
  }
  .fs-relation-right {
    display: inline-block;
    width: calc(100% - 370px);
    vertical-align: top;
    & /deep/ .ant-card-body {
      height: calc(100vh - 262px);
      padding: 0px;
    }
    .fs-ui-left {
      height: 100%;
      width: calc(100% - 350px);
      display: inline-block;
      overflow: auto;
    }
    .fs-ui-right {
      height: 100%;
      width: 350px;
      display: inline-block;
      border-left: 1px solid #e8e8e8;
      vertical-align: top;
      padding-top: 1px;
      & /deep/ .ant-table-placeholder {
        border-top: none;
        border-bottom: none;
      }
    }
  }
}
</style>
