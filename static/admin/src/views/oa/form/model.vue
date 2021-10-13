<template>
  <div class="fs-layout-box">
    <div class="fs-layout-header">
      <a-space>
        <a-button icon="save" @click="save" :loading="loading">保存模型</a-button>
        <a-button icon="delete" @click="clear">清空表单</a-button>
        <a-button icon="pic-right" @click="generateJSON">生成JSON</a-button>
      </a-space>
    </div>
    <div class="fs-layout-content">
      <div class="fs-layout-left">
        <a-collapse :activeKey="config.widgets.map(item => item.name)" :bordered="false">
          <a-collapse-panel :key="widget.name" :header="widget.name" v-for="widget in config.widgets">
            <draggable tag="ul" v-bind="{ group: { name:'widgets', pull:'clone', put:false }, sort:false, ghostClass: 'ghost' }">
              <li class="fs-widget-item" :data-id="item.type" :key="item.type" v-for="item in widget.children">
                <a-icon class="icon" :component="icons[item.icon]" />
                <span>{{ item.label }}</span>
              </li>
            </draggable>
          </a-collapse-panel>
        </a-collapse>
      </div>
      <div class="fs-layout-center">
        <div class="fs-layout-top">
          <a-space class="fs-device">
            <a-icon
              :class="['icon', device === item.type && 'fs-selected']"
              :component="icons[item.icon]"
              :key="item.type"
              v-for="item in config.devices"
              @click="handleDevice(item)" />
          </a-space>
        </div>
        <div class="fs-layout-main">
          <div class="fs-layout-device" @click="triggerForm" :style="{ width: devices[device].width }">
            <a-form-model v-bind="formLayout">
              <widget-form v-model="frame.widgets" :config="config" :activeItem.sync="activeItem" :tips.sync="tips" />
              <a-empty v-if="frame.widgets.length === 0" class="fs-widget-empty" description="从左侧拖入组件进行表单设计" />
            </a-form-model>
          </div>
        </div>
      </div>
      <div class="fs-layout-right">
        <property v-if="activeItem" v-model="activeItem" :config="config" :activeItem="activeItem" :tips.sync="tips" />
        <property v-else v-model="frame" :config="config" :activeItem="activeItem" :tips.sync="tips" />
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
import formFrameService from '@/service/oa/formFrame'
import icons from '@/core/icons'
import config from './design/config'
import draggable from 'vuedraggable'
import WidgetForm from './design/WidgetForm'
import Property from './design/Property'

export default {
  components: { draggable, WidgetForm, Property },
  data () {
    return {
      icons,
      config,
      device: 'tablet',
      loading: false,
      tips: '',
      jsonVisible: false,
      jsonText: '',
      frame: {
        id: 0,
        name: '',
        widgets: [],
        options: config.form.options()
      },
      activeItem: null
    }
  },
  computed: {
    formLayout () { return this.config.exhibition.formLayout(this.frame) },
    devices () {
      const map = {}
      this.config.devices.forEach(item => {
        map[item.type] = item
      })
      return map
    }
  },
  watch: {
    activeItem: {
      handler (val) {
        val && this.handleProperty(this.frame.widgets, val)
      },
      deep: true
    }
  },
  methods: {
    handleProperty (widgets, active) {
      for (const index in widgets) {
        const item = widgets[index]
        if (item.id === active.id) {
          Object.assign(item, active)
          return true
        }
        if (item.type !== 'grid') continue
        for (const i in item.options.items) {
          if (this.handleProperty(item.options.items[i].widgets, active)) return true
        }
      }
      return false
    },
    handleDevice (item) {
      this.device = item.type
      this.triggerForm()
    },
    generateJSON () {
      this.jsonText = JSON.stringify(this.frame, null, 2)
      this.jsonVisible = true
    },
    clear () {
      this.$set(this.frame, 'widgets', [])
      this.tips = '表单清空！'
    },
    triggerForm () {
      this.activeItem = null
      this.tips = '选中表单'
    },
    load () {
      this.loading = true
      this.tips = '正在载入数据信息...'
      formFrameService.info({ id: this.$route.query.id }).then(result => {
        try {
          if (result.code !== 0 || result.data.content.length === 0) {
            return false
          }
          const data = JSON.parse(result.data.content)
          const frame = {
            id: result.data.id,
            name: result.data.name,
            widgets: data.widgets || this.frame.widgets,
            options: Object.assign({}, this.frame.options, data.options || {})
          }
          this.$set(this, 'frame', frame)
        } catch (e) {
          this.$error({ title: '数据解析异常', content: e.message })
        } finally {
          this.loading = false
          this.tips = '数据载入完成'
          this.triggerForm()
        }
      })
    },
    collect () {
      const data = {
        widgets: this.frame.widgets,
        options: this.frame.options
      }
      return data
    },
    save () {
      this.loading = true
      this.tips = '正在保存...'
      const data = this.collect()
      if (data === null) return (this.loading = false)
      formFrameService.save({ id: this.$route.query.id, content: JSON.stringify(data) }).then(result => {
        this.loading = false
        this.tips = result.message
      })
    }
  },
  mounted () {
    this.load()
  }
}
</script>

<style lang="less" scoped>
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
      width: 240px;
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
        color: #333;
        cursor: move;
        background: #f4f6fc;
        border: 1px solid #f4f6fc;
        padding: 3px 10px;
        &:hover {
          color: #409eff;
          border: 1px dashed #409eff;
        }
        .icon {
          margin-right: 6px;
          font-size: 14px;
        }
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
        & /deep/ .ant-form {
          width: 100%;
          height: 100%;
        }
        .fs-layout-device {
          height: 100%;
          overflow-x: auto;
          background: white;
          margin: 0 auto;
          .fs-form-flex {
            & /deep/ .ant-form-item {
              display: flex;
            }
          }
          .sortable-ghost {
            width: 100%;
            height: 60px;
            background-color: #f6f7ff;
            border-top: solid 3px #5959df;
            .icon {
              margin: 0px 6px;
              font-size: 14px;
            }
          }
          .fs-widget-empty {
            position: absolute;
            top: calc(50% - 75px);
            left: calc(50% - 150px);
            width: 250px;
          }
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
