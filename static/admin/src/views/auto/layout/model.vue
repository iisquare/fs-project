<template>
  <div class="fs-layout-box">
    <div class="fs-layout-header">
      <a-menu class="fs-header-menu" mode="horizontal" :selectable="false">
        <a-sub-menu title="文件">
          <a-menu-item :disabled="loading" @click="load"><a-icon type="sync" /><span>重新载入</span></a-menu-item>
          <a-menu-item :disabled="loading" @click="save"><a-icon type="save" /><span>保存模型</span></a-menu-item>
        </a-sub-menu>
        <a-sub-menu title="排列">
          <a-menu-item :disabled="loading"><a-icon :component="icons.arrangeLayerTop" /><span>置于顶层</span></a-menu-item>
          <a-menu-item :disabled="loading"><a-icon :component="icons.arrangeLayerBottom" /><span>置于底层</span></a-menu-item>
          <a-menu-item :disabled="loading"><a-icon :component="icons.arrangeLayerUp" /><span>上移一层</span></a-menu-item>
          <a-menu-item :disabled="loading"><a-icon :component="icons.arrangeLayerDown" /><span>下移一层</span></a-menu-item>
          <a-menu-divider />
          <a-sub-menu title="分布对齐">
            <a-menu-item :disabled="loading"><a-icon :component="icons.arrangeAlignLeft" /><span>左对齐</span></a-menu-item>
            <a-menu-item :disabled="loading"><a-icon :component="icons.arrangeAlignHorizontal" /><span>居中对齐</span></a-menu-item>
            <a-menu-item :disabled="loading"><a-icon :component="icons.arrangeAlignRight" /><span>右对齐</span></a-menu-item>
            <a-menu-divider />
            <a-menu-item :disabled="loading"><a-icon :component="icons.arrangeAlignTop" /><span>顶端对齐</span></a-menu-item>
            <a-menu-item :disabled="loading"><a-icon :component="icons.arrangeAlignVertical" /><span>垂直居中对齐</span></a-menu-item>
            <a-menu-item :disabled="loading"><a-icon :component="icons.arrangeAlignBottom" /><span>底端对齐</span></a-menu-item>
            <a-menu-divider />
            <a-menu-item :disabled="loading"><a-icon :component="icons.arrangeDistributeHorizontal" /><span>水平平均分布</span></a-menu-item>
            <a-menu-item :disabled="loading"><a-icon :component="icons.arrangeDistributeVertical" /><span>垂直平均分布</span></a-menu-item>
          </a-sub-menu>
          <a-sub-menu title="匹配大小">
            <a-menu-item :disabled="loading"><a-icon :component="icons.arrangeFitWidth" /><span>宽度匹配</span></a-menu-item>
            <a-menu-item :disabled="loading"><a-icon :component="icons.arrangeFitHeight" /><span>高度匹配</span></a-menu-item>
            <a-menu-item :disabled="loading"><a-icon :component="icons.arrangeFitSame" /><span>宽高匹配</span></a-menu-item>
          </a-sub-menu>
          <a-menu-divider />
          <a-menu-item :disabled="loading"><a-icon :component="icons.arrangeOtherGroup" /><span>组合</span></a-menu-item>
          <a-menu-item :disabled="loading"><a-icon :component="icons.arrangeOtherUngroup" /><span>取消组合</span></a-menu-item>
          <a-menu-divider />
          <a-menu-item :disabled="loading"><a-icon :component="icons.arrangeOtherLock" /><span>锁定</span></a-menu-item>
          <a-menu-item :disabled="loading"><a-icon :component="icons.arrangeOtherUnlock" /><span>解锁</span></a-menu-item>
        </a-sub-menu>
      </a-menu>
      <a-space class="fs-header-right">
        <a-button type="primary">预览</a-button>
      </a-space>
    </div>
    <div class="fs-layout-content">
      <div class="fs-layout-left">
        <a-tabs default-active-key="widget" :animated="false">
          <a-tab-pane key="widget" tab="控件">
            <a-collapse :activeKey="config.widgets.map(item => item.name)" :bordered="false">
              <a-collapse-panel :key="catalog.name" :header="catalog.name" v-for="catalog in config.widgets">
                <ul>
                  <li
                    v-for="(widget, wi) in catalog.children"
                    :key="wi"
                    draggable
                    @dragstart="ev => dragWidget = widget"
                    @dragend="ev => dragWidget = null">
                    <img :src="widget.image" />
                    <div>{{ widget.name }}</div>
                  </li>
                </ul>
              </a-collapse-panel>
            </a-collapse>
          </a-tab-pane>
          <a-tab-pane key="item" tab="元素">
          </a-tab-pane>
        </a-tabs>
      </div>
      <div class="fs-layout-center">
        <div class="fs-layout-top">
          <a-space class="fs-toolbar-left">
            <a-button icon="rest" type="link" @click="clear">清空</a-button>
          </a-space>
          <a-space class="fs-toolbar-right">
            <span>缩放：</span>
            <a-input-number
              v-model="ruler.zoom"
              :min="10"
              :max="500"
              :step="10"
              :formatter="value => `${value}%`"
              :parser="value => value.replace('%', '')"
            />
          </a-space>
        </div>
        <div class="fs-layout-main">
          <fs-ruler v-model="ruler" :canvas="screen.options">
            <div
              ref="canvas"
              class="fs-layout-canvas"
              @dragover="canvasDragOver"
              @drop="canvasDrop"
              :style="config.canvasStyle(screen.options)">
              <div
                v-for="item in items"
                :key="item.id"
                :class="['fs-auto-element', selected[item.id] && 'fs-auto-element-selected']"
                :id="item.id">
                <fs-element :value="item" :config="config" :activeItem="activeItem" />
              </div>
            </div>
          </fs-ruler>
        </div>
      </div>
      <div class="fs-layout-right">
        <fs-property v-if="activeItem" v-model="activeItem" :config="config" :activeItem="activeItem" :tips.sync="tips" />
        <fs-property v-else v-model="screen" :config="config" :activeItem="activeItem" :tips.sync="tips" />
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
import config from './design/config'
import layoutService from '@/service/auto/layout'
import Property from './design/Property'
import Element from './design/Element'
import Ruler from '@/components/Ruler/Ruler'
import Moveable from 'moveable'
import Selecto from 'selecto'

export default {
  components: { FsProperty: Property, FsElement: Element, FsRuler: Ruler },
  data () {
    return {
      icons,
      config,
      loading: false,
      tips: '',
      screen: {
        id: 0,
        name: '',
        options: config.canvas.options()
      },
      activeItem: null,
      dragWidget: null,
      items: {},
      ruler: {
        zoom: 100,
        lines: {
          h: [],
          v: []
        },
        isShowReferLine: true
      },
      moveable: null,
      selecto: null,
      selected: {}
    }
  },
  watch: {
    activeItem: {
      handler (item) {
        if (item === null) return
        this.items[item.id] = item
        this.$nextTick(() => { // 目标数据发生变化时，更新样式
          const target = document.querySelector(`#${item.id}`)
          Object.assign(target.style, config.elementStyle(item))
          this.moveable.updateTarget()
        })
      },
      deep: true
    },
    ruler: {
      handler (ruler) {
        this.moveable.snapGap = ruler.isShowReferLine
        this.moveable.verticalGuidelines = ruler.isShowReferLine ? ruler.lines.h : []
        this.moveable.horizontalGuidelines = ruler.isShowReferLine ? ruler.lines.v : []
      },
      deep: true
    }
  },
  methods: {
    clear () {
      this.items = {}
      this.activeItem = null
    },
    canvasDragOver (ev) {
      if (this.dragWidget) ev.preventDefault()
    },
    canvasDrop (ev) {
      const item = this.config.generateItem(this.dragWidget, ev)
      this.items[item.id] = item
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
      this.tips = `选中节点 ${item.id} - ${item.name}`
      this.$nextTick(() => {
        this.moveable.target = document.querySelector(`#${item.id}`)
      })
    },
    load () {
      this.loading = true
      this.tips = '正在载入数据信息...'
      layoutService.info({ id: this.$route.query.id }).then(result => {
        if (result.code !== 0) return false
        Object.assign(this.screen, {
          id: result.data.id,
          name: result.data.name
        })
        try {
          if (result.data.content) {
            const data = JSON.parse(result.data.content)
            if (data.canvas) Object.assign(this.screen.options, data.canvas)
          }
        } catch (e) {
          this.$error({ title: '数据解析异常', content: e.message })
        } finally {
          this.loading = false
          this.tips = '数据载入完成'
          this.triggerCanvas()
        }
      })
    },
    collect () {
      return Object.assign({}, { canvas: this.screen.options })
    },
    save () {
      const data = this.collect()
      layoutService.save({ id: this.info.id, content: JSON.stringify(data) }).then(result => {
        this.loading = false
        this.tips = result.message
      })
    }
  },
  mounted () {
    this.moveable = new Moveable(this.$refs.canvas.parentElement, {
      target: null,
      container: this.$refs.canvas,
      draggable: true, // 拖拽移动
      resizable: true, // 更改大小
      rotatable: true, // 旋转
      scalable: false, // 缩放
      warpable: false, // 弯曲
      origin: true,
      keepRatio: false,
      edge: false,
      snappable: true, // 对齐
      snapDirections: { left: true, top: true, right: true, bottom: true, center: true, middle: true },
      throttleDrag: 0,
      throttleResize: 0,
      throttleScale: 0,
      throttleRotate: 0
    })
    let moveItem = {}
    const eventStart = e => {
      this.selecto.selectByClick = false
      const item = this.items[e.target.id]
      Object.assign(moveItem, item, {
        offset: {
          top: item.top,
          left: item.left,
          width: item.width,
          height: item.height
        }
      })
    }
    const eventEnd = e => { // 在操作完成后更新目标数据，防止频繁刷新DOM导致卡顿
      this.selecto.selectByClick = true
      const item = this.items[e.target.id]
      item.top = moveItem.top
      item.left = moveItem.left
      item.width = moveItem.width
      item.height = moveItem.height
      item.rotateAngle = moveItem.rotateAngle
      moveItem = {}
    }
    this.moveable
    .on('dragStart', eventStart).on('rotateStart', eventStart).on('resizeStart', eventStart)
    .on('dragEnd', eventEnd).on('rotateEnd', eventEnd).on('resizeEnd', eventEnd)
    .on('drag', e => {
      moveItem.top = e.top
      moveItem.left = e.left
      Object.assign(e.target.style, config.elementStyle(moveItem))
    }).on('resize', e => {
      moveItem.width = moveItem.offset.width + e.dist[0]
      if (e.direction[0] === -1) {
        moveItem.left = moveItem.offset.left - e.dist[0]
      }
      moveItem.height = moveItem.offset.height + e.dist[1]
      if (e.direction[1] === -1) {
        moveItem.top = moveItem.offset.top - e.dist[1]
      }
      Object.assign(e.target.style, config.elementStyle(moveItem))
    }).on('rotate', e => {
      moveItem.rotateAngle = Math.round(e.rotate)
      Object.assign(e.target.style, config.elementStyle(moveItem))
    })
    let groupEvent = null
    this.moveable.on('dragGroupStart', e => {
      groupEvent = e
    }).on('dragGroup', e => {
      for (const target of e.targets) {
        const item = Object.assign({}, this.items[target.id])
        item.top += e.clientY - groupEvent.clientY
        item.left += e.clientX - groupEvent.clientX
        Object.assign(target.style, config.elementStyle(item))
      }
    }).on('dragGroupEnd', e => {
      for (const target of e.targets) {
        const item = this.items[target.id]
        item.top += e.clientY - groupEvent.clientY
        item.left += e.clientX - groupEvent.clientX
        Object.assign(target.style, config.elementStyle(item))
      }
    })
    this.selecto = new Selecto({
      container: this.$refs.canvas,
      rootContainer: this.$refs.canvas.parentElement,
      selectableTargets: ['.fs-auto-element'],
      selectByClick: true,
      selectFromInside: false,
      continueSelect: false,
      toggleContinueSelect: 'ctrl',
      keyContainer: window,
      hitRate: 10,
      getElementRect: Moveable.getElementInfo
    })
    this.selecto.on('selectStart', e => {
      this.moveable.target = null
    }).on('select', e => {
      const selected = {}
      for (const dom of e.selected) {
        selected[dom.id] = dom
      }
      this.selected = selected
    }).on('selectEnd', e => {
      const selected = e.selected.filter(target => {
        return !this.items[target.id].locked
      })
      this.moveable.target = selected
      this.selected = {}
      if (selected.length === 0) {
        this.triggerCanvas()
      } else if (selected.length === 1) {
        this.moveable.resizable = true
        this.moveable.scalable = true
        this.moveable.rotatable = true
        this.triggerCanvasItem(this.items[selected[0].id])
      } else {
        this.moveable.resizable = false
        this.moveable.scalable = false
        this.moveable.rotatable = false
      }
    })
    this.load()
  },
  destroyed () {
    this.selecto.destroy()
    this.moveable.destroy()
  }
}
</script>

<style lang="less" scoped>
.fs-auto-element {
  position: absolute;
  border: 1px solid transparent;
}
.fs-auto-element-selected {
  border: 1px dashed #88d5fc;
}
.fs-layout-box {
  width: 100%;
  height: 100%;
  .fs-layout-header {
    width: 100%;
    height: 48px;
    padding: 0px 5px;
    border-bottom: solid 1px #cbcccc;
    .fs-header-menu {
      float: left;
    }
    .fs-header-right {
      float: right;
      height: 100%;
      line-height: 100%;
      padding: 0px 15px;
    }
  }
  .fs-layout-content {
    height: calc(100% - 74px);
    overflow: hidden;
    .fs-layout-left {
      width: 240px;
      height: 100%;
      display: inline-block;
      border-right: solid 1px #cbcccc;
      overflow-y: auto;
      .fs-layout-title {
        height: 44px;
        line-height: 44px;
        padding: 0px 15px;
        vertical-align: middle;
        border-bottom: solid 1px #e8e8e8;
      }
      /deep/ .ant-tabs-bar {
        margin-bottom: 0px;
      }
      /deep/ .ant-collapse-content-box {
        padding: 10px;
      }
      ul {
        position: relative;
        overflow: hidden;
        padding: 0;
        margin: 0 -5px;
      }
      li {
        display: block;
        float: left;
        width: 90px;
        height: 92px;
        margin: 2px 10px;
        box-sizing: border-box;
        border: 1px solid transparent;
        text-align: center;
        overflow: hidden;
        cursor: pointer;
        img {
          border: none;
          width: 88px;
          height: 64px;
        }
        div {
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
          line-height: 25px;
        }
        &:hover {
          border: solid 1px black;
          border-radius: 5px;
        }
      }
    }
    .fs-layout-center {
      height: 100%;
      width: calc(100% - 590px);
      display: inline-block;
      position: relative;
      overflow: hidden;
      .fs-layout-top {
        height: 44px;
        vertical-align: middle;
        border-bottom: solid 1px #e8e8e8;
        .fs-toolbar-left {
          padding: 0px 15px;
          height: 100%;
          line-height: 100%;
          vertical-align: middle;
          & /deep/ .ant-slider {
            width: 100px;
          }
        }
        .fs-toolbar-right {
          float: right;
          padding: 0px 15px;
          height: 100%;
          line-height: 100%;
          vertical-align: middle;
        }
      }
      .fs-layout-main {
        height: calc(100% - 44px);
        background: #f5f5f5;
        .fs-layout-canvas {
          position: relative;
          width: 100%;
          height: 100%;
          overflow: visible;
        }
        /deep/ .selecto-selection {
          background: rgba(68, 170, 255, 0.1);
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
