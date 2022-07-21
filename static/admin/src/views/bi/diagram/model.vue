<template>
  <div class="fs-layout-box">
    <div class="fs-layout-header">
      <a-space>
        <a-button icon="save" @click="save" :loading="loading">保存模型</a-button>
        <a-button icon="delete" @click="clear" :loading="loading">清空画布</a-button>
        <a-button icon="pic-right" @click="generateJSON">生成JSON</a-button>
      </a-space>
    </div>
    <div class="fs-layout-content">
      <div class="fs-layout-left">
        <a-collapse :activeKey="config.widgets.map(item => item.name)" :bordered="false">
          <a-collapse-panel :key="widget.name" :header="widget.name" v-for="widget in config.widgets">
            <ul>
              <li
                :class="['fs-widget-item', draggable(item) ? 'fs-widget-enabled' : 'fs-widget-disabled']"
                :title="item.title"
                :data-id="item.type"
                :key="item.type"
                v-for="item in widget.children"
                @dragstart="event => onWidgetDragStart(item, event)"
                :draggable="draggable(item)">
                <a-icon class="icon" :component="icons[item.icon]" />
                <span>{{ item.label }}</span>
              </li>
            </ul>
          </a-collapse-panel>
        </a-collapse>
      </div>
      <div class="fs-layout-center">
        <div class="fs-layout-top">
          <a-space class="fs-device">
            <a-icon
              :class="[activeToolbar === item.type && 'fs-selected']"
              :component="icons[item.icon]"
              :key="item.type"
              :title="item.label"
              v-for="item in config.toolbars"
              @click="event => handleToolbar(item, event)" />
            <a-divider type="vertical" />
            <span>计算引擎：{{ diagram.engine }}</span>
            <a-divider type="vertical" />
            <span>处理模式：{{ diagram.model }}</span>
          </a-space>
        </div>
        <div class="fs-layout-main">
          <div ref="canvas" id="fs-flow-canvas" />
        </div>
      </div>
      <div class="fs-layout-right">
        <property
          v-model="activeItem"
          v-if="flow"
          :flow="flow"
          :config="config"
          :diagram.sync="diagram"
          :activeItem.sync="activeItem"
          :tips.sync="tips" />
      </div>
    </div>
    <div class="fs-layout-footer">
      <a-icon type="bulb" theme="twoTone" />
      <span>{{ tips }}</span>
    </div>
    <a-modal title="JSON信息" v-model="jsonVisible" :maskClosable="true">
      <a-textarea v-model="jsonText" rows="12" />
    </a-modal>
  </div>
</template>

<script>
import diagramService from '@/service/bi/diagram'
import icons from '@/assets/icons'
import config from './design/config'
import Property from './design/Property'
import Flow from '@/components/X6/flow'
import { triggerWindowResizeEvent } from '@/utils/util'

export default {
  components: { Property },
  data () {
    return {
      icons,
      config,
      loading: false,
      tips: '',
      jsonVisible: false,
      jsonText: '',
      diagram: {
        id: 0,
        name: '',
        engine: '',
        model: '',
        options: config.canvas.options()
      },
      activeItem: null,
      flow: null,
      activeToolbar: 'hand'
    }
  },
  watch: {
    activeItem: {
      handler (item) {
        this.flow.updateCell(item)
      },
      deep: true
    }
  },
  methods: {
    handleToolbar (toolbar, event) {
      if (toolbar.type !== 'fit') {
        this.activeToolbar = toolbar.type
      }
      toolbar.callback(toolbar, this.flow, event)
    },
    onWidgetDragStart (widget, event) {
      this.flow.widgetDragStart(widget, event)
    },
    triggerCanvas () {
      if (this.activeItem === null) return true
      this.activeItem = null
      this.tips = '选中画布'
      this.flow.highlight([])
      return true
    },
    triggerCanvasItem (item) {
      if (this.activeItem && this.activeItem.id === item.id) return true
      this.activeItem = item
      this.tips = `选中 ${item.shape} ${item.data.title}`
      this.flow.highlight([item])
      return true
    },
    draggable (widget) {
      return widget.supports.some(item => {
        return item.indexOf(this.diagram.engine) !== -1 && item.indexOf(this.diagram.model) !== -1
      })
    },
    generateJSON () {
      this.jsonText = JSON.stringify(this.collect(), null, 2)
      this.jsonVisible = true
    },
    clear () {
      this.loading = true
      this.tips = '执行画布清空...'
      this.activeItem = null
      this.flow.reset()
      this.tips = '画布清空完成'
      this.loading = false
    },
    load () {
      this.loading = true
      this.tips = '正在载入数据信息...'
      diagramService.info({ id: this.$route.query.id }).then(result => {
        if (result.code !== 0) return false
        const diagram = {
          id: result.data.id,
          name: result.data.name,
          engine: result.data.engine,
          model: result.data.model
        }
        Object.assign(this.diagram, diagram)
        Object.assign(this.config.diagram, diagram)
        try {
          if (result.data.content) {
            const data = JSON.parse(result.data.content)
            Object.assign(this.diagram.options, { grid: !!data.grid })
            this.flow.reset(data.cells)
          }
        } catch (e) {
          this.$error({ title: '数据解析异常', content: e.message })
        } finally {
          this.loading = false
          this.tips = '数据载入完成'
          triggerWindowResizeEvent()
          this.triggerCanvas()
        }
      })
    },
    collect () {
      return Object.assign({}, this.diagram.options, this.flow.collect())
    },
    save () {
      this.loading = true
      this.tips = '正在保存...'
      const data = this.collect()
      if (data === null) return (this.loading = false)
      diagramService.save({ id: this.$route.query.id, content: JSON.stringify(data) }).then(result => {
        this.loading = false
        this.tips = result.message
      })
    }
  },
  mounted () {
    this.load()
    const _this = this
    this.flow = new Flow(this.$refs.canvas, {
      onBlankClick () {
        return _this.triggerCanvas()
      },
      onCellClick ({ cell }) {
        return _this.triggerCanvasItem(_this.flow.cell2meta(cell))
      },
      onNodeAdded ({ node }) {
        return _this.triggerCanvasItem(_this.flow.cell2meta(node))
      },
      onEdgeConnected ({ edge }) {
        return _this.triggerCanvasItem(_this.flow.cell2meta(edge))
      }
    })
  }
}
</script>

<style lang="less" scoped>
@import url('../../../components/X6/flow.less');

/deep/ #fs-flow-canvas {
  width: 100%;
  height: 100%;
  overflow: hidden;
  .widget()
}
.fs-layout-box {
  width: 100%;
  height: 100%;
  .fs-layout-header {
    width: 100%;
    height: 38px;
    padding: 2px 5px 2px 5px;
    border-bottom: solid 1px #cbcccc;
  }
  .fs-layout-content {
    height: calc(100% - 64px);
    overflow: hidden;
    .fs-layout-left {
      width: 300px;
      height: 100%;
      display: inline-block;
      border-right: solid 1px #cbcccc;
      overflow-y: auto;
      & /deep/ .ant-collapse-header {
        padding: 6px 6px 6px 35px;
      }
      ul {
        position: relative;
        overflow: hidden;
        padding: 0;
        margin: 0;
      }
      .fs-widget-item {
        font-size: 12px;
        display: block;
        width: 48%;
        line-height: 26px;
        position: relative;
        float: left;
        left: 0;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
        margin: 1%;
        border: 1px solid #f4f6fc;
        background: #f4f6fc;
        padding: 3px 10px;
        content: attr(title);
        .icon {
          margin-right: 6px;
          font-size: 14px;
        }
      }
      .fs-widget-enabled {
        color: #333;
        cursor: move;
        &:hover {
          color: #409eff;
          border: 1px dashed #409eff;
        }
      }
      .fs-widget-disabled {
        color: gray;
        cursor: no-drop;
        &:hover {
          border: 1px dashed #f581a4;
        }
      }
    }
    .fs-layout-center {
      height: 100%;
      width: calc(100% - 650px);
      display: inline-block;
      position: relative;
      .fs-layout-top {
        height: 44px;
        border-bottom: solid 1px #e8e8e8;
        .fs-device {
          padding: 0px 15px;
          height: 100%;
          line-height: 100%;
          vertical-align: middle;
          .fs-selected {
            color: #409eff;
          }
          .icon {
            padding: 3px;
            border-radius: 3px;
            cursor: pointer;
          }
        }
      }
      .fs-layout-main {
        height: calc(100% - 44px);
        background: #fafafa;
        overflow: auto;
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
          .icon {
            margin-right: 6px;
            font-size: 14px;
          }
        }
        .fs-layout-selected {
          color: #409eff;
          border: 1px solid #409eff;
        }
      }
    }
    .fs-layout-right {
      .property(350px)
    }
  }
  .fs-layout-footer {
    height: 26px;
    padding: 2px 5px 2px 5px;
    border-top: solid 1px #cbcccc;
    span {
      padding: 0px 5px 0px 5px;
      font-size: 12px;
    }
  }
}
</style>
