<template>
  <div class="fs-layout-diagram">
    <div class="fs-layout-content">
      <div class="fs-layout-left">
        <a-collapse :activeKey="config.widgets.map(item => item.name)" :bordered="false">
          <a-collapse-panel :key="widget.name" :header="widget.name" v-for="widget in config.widgets">
            <ul>
              <li
                draggable
                class="fs-widget-item fs-widget-enabled"
                :title="item.title"
                :data-id="item.type"
                :key="item.type"
                v-for="item in widget.children"
                @dragstart="ev => dragWidget = item"
                @dragend="ev => dragWidget = null">
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
            <a-button type="link" @click="clear">清空</a-button>
          </a-space>
        </div>
        <div class="fs-layout-main">
          <div
            ref="canvas"
            id="fs-layout-canvas"
            @click="triggerCanvas"
            @dragover="canvasDragOver"
            @drop="canvasDrop"
            :style="{ width: canvas.options.width + 'px', height: canvas.options.height + 'px', top: canvas.options.top + 'px' }"
          >
            <div
              :class="['fs-layout-item', activeItem === item && 'fs-layout-selected']"
              :id="item.id"
              :key="item.id"
              @click.stop="triggerCanvasItem(item)"
              @contextmenu="ev => handleContextMenu(ev, item)"
              :style="{ left: item.x + 'px', top:item.y + 'px' }"
              v-for="item in draw.items">
              <a-icon class="icon" :component="icons[item.icon]" />
              <span>{{ item.title }}</span>
            </div>
          </div>
        </div>
      </div>
      <div class="fs-layout-right">
        <fs-property v-if="activeItem" v-model="activeItem" :config="config" :activeItem="activeItem" :tips.sync="tips" />
        <fs-property v-else v-model="canvas" :config="config" :activeItem="activeItem" :tips.sync="tips" />
      </div>
    </div>
    <div class="fs-layout-footer">
      <a-icon type="bulb" theme="twoTone" />
      <span>{{ tips }}</span>
    </div>
  </div>
</template>

<script>
import icons from '@/assets/icons'
import config from './config'
import Draw from './draw'
import MenuUtil from '@/utils/menu'

export default {
  name: 'Diagram',
  components: {
    FsProperty: () => import('./Property.vue')
  },
  props: {
    value: { type: Object, required: true }
  },
  data () {
    return {
      icons,
      config,
      tips: '',
      loading: false,
      canvas: {
        options: config.canvas.options()
      },
      activeItem: null,
      dragWidget: null,
      draw: new Draw('#fs-layout-canvas')
    }
  },
  watch: {
    'canvas.options.height': {
      handler () {
        this.resizeCanvas()
      },
      deep: true
    },
    activeItem: {
      handler (item) {
        this.draw.updateItem(item)
      },
      deep: true
    }
  },
  methods: {
    canvasDragOver (ev) {
      if (this.dragWidget) ev.preventDefault()
    },
    canvasDrop (ev) {
      const item = this.draw.generateItem(this.dragWidget, ev)
      this.draw.addItem(item)
      this.triggerCanvasItem(item)
    },
    triggerCanvas () {
      if (this.activeItem === null) return true
      this.activeItem = null
      this.tips = '选中画布'
    },
    triggerCanvasItem (item) {
      if (this.activeItem === item) return true
      this.activeItem = item
      this.tips = `选中节点 ${item.id} - ${item.title}`
    },
    resizeCanvas () {
      const top = Math.max(0, this.$refs.canvas.parentElement.offsetHeight - this.canvas.options.height - 30)
      this.canvas.options.top = top / 2
    },
    handleContextMenu (ev, item) {
      MenuUtil.context(ev, [{ key: 'delete', icon: 'delete', title: '删除节点' }], menu => {
        switch (menu.key) {
          case 'delete':
            if (this.activeItem === item) this.activeItem = null
            return this.draw.removeItem(item)
          default:
            return false
        }
      })
    },
    clear () {
      this.loading = true
      this.tips = '执行画布清空...'
      this.activeItem = null
      this.draw.clear()
      this.tips = '画布清空完成'
      this.loading = false
    },
    reset () {
      this.draw.clear()
      if (Object.values(this.value).length !== 0) {
        Object.assign(this.canvas, this.value.canvas || {})
        this.value.items.forEach(item => this.draw.addItem(item))
        this.draw.addRelations(this.value.relations)
      }
      this.resizeCanvas()
      this.tips = '设计器已就绪'
      return true
    },
    collect () {
      return Object.assign({}, { canvas: this.canvas }, this.draw.collect())
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
/deep/ #fs-layout-canvas {
  margin: 15px auto;
  position: relative;
  min-width: 100px;
  min-height: 100px;
  background: url(../../../../assets/grid.gif) white;
}
.fs-layout-diagram {
  width: 100%;
  height: calc(100vh - 48px);
  .fs-layout-content {
    height: calc(100% - 26px);
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
            background: #e4e7ed;
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
      height: 100%;
      width: 350px;
      display: inline-block;
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
        margin-bottom: 0px;
        padding: 5px 0px;
      }
      & /deep/ .fs-property-title {
          color: rgba(0,0,0,.85);
          font-weight: 600;
          font-size: 14px;
          line-height: 1.5;
          padding: 5px 0px;
      }
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
