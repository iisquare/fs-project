<template>
  <div class="fs-layout-box">
    <div class="fs-layout-header">
      <a-menu mode="horizontal" :selectable="false">
        <a-sub-menu title="文件">
          <a-menu-item :disabled="loading" @click="load"><a-icon type="sync" /><span>重新载入</span></a-menu-item>
          <a-menu-item :disabled="loading" @click="save"><a-icon type="save" /><span>保存模型</span></a-menu-item>
        </a-sub-menu>
      </a-menu>
    </div>
    <div class="fs-layout-content">
      <div class="fs-layout-left">
        <div class="fs-layout-title">图层</div>
      </div>
      <div class="fs-layout-center">
        <div class="fs-layout-top">
          <a-space class="fs-toolbar">
            <a-popover placement="bottomLeft" trigger="hover">
              <section slot="content">
                <a-tabs :default-active-key="0" :animated="false" tabPosition="left">
                  <a-tab-pane :tab="catalog.name" :key="ci" v-for="(catalog, ci) in config.widgets">
                    <ul class="fs-ui-elements">
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
                  </a-tab-pane>
                </a-tabs>
              </section>
              <a-button icon="align-left" type="dashed" shape="round">元素</a-button>
            </a-popover>
            <span>缩放：</span>
            <a-slider v-model="zoom" :min="1" />
          </a-space>
        </div>
        <div class="fs-layout-main" ref="workbench" @click="triggerCanvas">
          <div
            class="fs-layout-canvas"
            ref="canvas"
            @dragover="canvasDragOver"
            @drop="canvasDrop"
            :style="config.canvasStyle(screen.options, zoom)">
            <div v-for="item in items" :key="item.id" class="fs-bi-element" :style="config.elementStyle(item)" @click.stop="triggerCanvasItem(item)">
              <fs-element :value="item" :config="config" :activeItem="activeItem" />
            </div>
          </div>
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
import { Ruler } from '@/components/Ruler'

export default {
  components: { FsProperty: Property, FsElement: Element },
  data () {
    return {
      icons,
      config,
      loading: false,
      tips: '',
      zoom: 100,
      screen: {
        id: 0,
        name: '',
        options: config.canvas.options()
      },
      activeItem: null,
      dragWidget: null,
      items: {},
      ruler: null
    }
  },
  watch: {
    'diagram.options.height': {
      handler () {
        this.resizeCanvas()
      },
      deep: true
    },
    activeItem: {
      handler (item) {
        if (item === null) return
        this.items[item.id] = item
      },
      deep: true
    }
  },
  methods: {
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
    },
    resizeCanvas () {
      const top = Math.max(0, this.$refs.canvas.parentElement.offsetHeight - this.screen.options.height - 70)
      this.screen.options.top = top / 2
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
          this.resizeCanvas()
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
    this.ruler = new Ruler(this.$refs.workbench)
    window.onresize = () => { this.resizeCanvas() }
    this.load()
  },
  destroyed () {
  }
}
</script>

<style lang="less" scoped>
@import url('../../../components/Ruler/ruler.less');

.fs-bi-element {
  position: absolute;
  border: 1px solid transparent;
}
.fs-ui-elements {
  width: 365px;
  height: 300px;
  overflow-x: hidden;
  overflow-y: auto;
  position: relative;
  padding: 0;
  margin: 0;
  li {
    display: block;
    position: relative;
    float: left;
    width: 110px;
    height: 100px;
    margin: 2px 5px;
    box-sizing: border-box;
    border: 1px solid transparent;
    text-align: center;
    overflow: hidden;
    cursor: pointer;
    img {
      border: none;
      width: 100%;
      height: 70px;
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
.fs-layout-box {
  width: 100%;
  height: 100%;
  .fs-layout-header {
    width: 100%;
    height: 48px;
    padding: 0px 5px;
    border-bottom: solid 1px #cbcccc;
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
    }
    .fs-layout-center {
      height: 100%;
      width: calc(100% - 590px);
      display: inline-block;
      position: relative;
      overflow: auto;
      .fs-layout-top {
        height: 44px;
        vertical-align: middle;
        border-bottom: solid 1px #e8e8e8;
        .fs-toolbar {
          padding: 0px 15px;
          height: 100%;
          line-height: 100%;
          vertical-align: middle;
          & /deep/ .ant-slider {
            width: 100px;
          }
        }
      }
      .fs-layout-main {
        height: calc(100% - 44px);
        overflow: auto;
        padding: 35px;
        background: url(../../../assets/dot.png) #fafafa;
        .fs-layout-canvas {
          margin: 0 auto;
          position: relative;
          overflow: hidden;
        }
      }
      ::-webkit-scrollbar {
        width: 10px;
        height: 10px;
        background-color: #414141;
      }
      ::-webkit-scrollbar-thumb {
        border-radius: 5px;
        background-color: #7e7e7e;
      }
      ::-webkit-scrollbar-corner {
        background-color: #535353;
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
