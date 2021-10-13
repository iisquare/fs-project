<template>
  <div class="fs-layout-box">
    <div class="fs-layout-header">
      <a-menu mode="horizontal" :selectable="false">
        <a-sub-menu title="文件">
          <a-menu-item :disabled="loading" @click="load"><a-icon type="sync" /><span>重新载入</span></a-menu-item>
          <a-menu-item :disabled="loading" @click="save"><a-icon type="save" /><span>保存模型</span></a-menu-item>
          <a-menu-item>
            <a-upload name="file" :multiple="false" accept=".xml" :beforeUpload="handleImport">
              <a-icon type="folder-open" /><span>导入模型</span>
            </a-upload>
          </a-menu-item>
          <a-menu-item @click="bpmnImportXML()"><a-icon type="plus-circle" /><span>新建模型</span></a-menu-item>
          <a-menu-divider />
          <a-menu-item @click="download('XML')"><a-icon type="download" /><span>导出为XML</span></a-menu-item>
          <a-menu-item @click="download('SVG')"><a-icon type="picture" /><span>导出为SVG</span></a-menu-item>
        </a-sub-menu>
        <a-sub-menu title="编辑">
          <a-menu-item @click="runCommand('undo')"><a-icon type="undo" /><span>撤销</span></a-menu-item>
          <a-menu-item @click="runCommand('redo')"><a-icon type="redo" /><span>重做</span></a-menu-item>
          <a-menu-divider />
          <a-menu-item :disabled="!canCommand('cut')" @click="runCommand('cut')"><a-icon type="scissor" /><span>剪切</span></a-menu-item>
          <a-menu-item :disabled="!canCommand('copy')" @click="runCommand('copy')"><a-icon type="copy" /><span>复制</span></a-menu-item>
          <a-menu-item :disabled="!canCommand('paste')" @click="runCommand('paste')"><a-icon type="snippets" /><span>粘贴</span></a-menu-item>
          <a-menu-divider />
          <a-menu-item :disabled="!canCommand('delete')" @click="runCommand('delete')"><a-icon type="delete" /><span>删除</span></a-menu-item>
        </a-sub-menu>
        <a-sub-menu title="视图">
          <a-menu-item @click="zoom('in')"><a-icon type="zoom-in" /><span>放大</span></a-menu-item>
          <a-menu-item @click="zoom('out')"><a-icon type="zoom-out" /><span>缩小</span></a-menu-item>
          <a-menu-item @click="zoom()"><a-icon type="fullscreen-exit" /><span>自动适应</span></a-menu-item>
        </a-sub-menu>
        <a-sub-menu title="排列">
          <a-menu-item disabled><a-icon :component="icons.actionAlignVertical" /><span>垂直对齐</span></a-menu-item>
          <a-menu-item disabled><a-icon :component="icons.actionAlignHorizontal" /><span>水平对齐</span></a-menu-item>
          <a-menu-item disabled><a-icon :component="icons.actionSameSize" /><span>相同大小</span></a-menu-item>
        </a-sub-menu>
      </a-menu>
    </div>
    <div class="fs-layout-content">
      <div class="fs-layout-left">
        <a-collapse :activeKey="config.widgets.map(item => item.name)" :bordered="false">
          <a-collapse-panel :key="widget.name" :header="widget.name" v-for="widget in config.widgets">
            <ul>
              <li
                class="fs-widget-item"
                draggable
                @dragstart="event => onWidgetDragStart(item, event)"
                :key="item.type"
                v-for="item in widget.children">
                <a-icon class="icon" :component="icons[item.icon]" />
                <span>{{ item.label }}</span>
              </li>
            </ul>
          </a-collapse-panel>
        </a-collapse>
      </div>
      <div class="fs-layout-center">
        <div class="fs-layout-top">
          <a-space class="fs-toolbar">
            <a-icon
              :class="['fs-toolbar-icon', activeToolbar === item && 'fs-toolbar-selected']"
              :component="icons[item.icon]"
              :key="item.type"
              v-for="item in config.toolbars"
              @click="event => handleToolbar(item, event)" />
          </a-space>
          <a-divider type="vertical" />
          <span class="fs-info-title">{{ info.name }}</span>
        </div>
        <div class="fs-layout-main">
          <div class="fs-layout-canvas" ref="canvas"></div>
        </div>
      </div>
      <div class="fs-layout-right">
        <property v-if="bpmn" v-model="info" :bpmn="bpmn" :element="activeElement" :tips.sync="tips" />
      </div>
    </div>
    <div class="fs-layout-footer">
      <a-icon type="bulb" theme="twoTone" />
      <span>{{ tips }}</span>
    </div>
  </div>
</template>

<script>
import icons from '@/core/icons'
import config from './design/config'
import workflowService from '@/service/oa/workflow'
import Property from './design/Property'
import BPMN from './bpmn/bpmn'
import XMLTemplate from './bpmn/template'

export default {
  components: { Property },
  data () {
    return {
      icons,
      config,
      loading: false,
      tips: '',
      activeElement: null,
      activeToolbar: null,
      bpmn: null,
      bpmnZoom: 1.0,
      info: {
        id: 0,
        name: ''
      }
    }
  },
  methods: {
    zoom (type) {
      switch (type) {
        case 'in': // 放大
          this.bpmn.canvas.zoom(this.bpmn.canvas.zoom() + 0.1)
          break
        case 'out': // 缩小
        this.bpmn.canvas.zoom(this.bpmn.canvas.zoom() - 0.1)
          break
        default: // 自适应
          this.bpmn.canvas.zoom('fit-viewport')
      }
    },
    canCommand (cmd) {
      switch (cmd) {
        case 'cut':
        case 'copy':
        case 'delete':
          return this.activeElement !== null
        case 'paste':
          return this.bpmn && this.bpmn._elementCopied !== null
        default:
          return false
      }
    },
    runCommand (cmd) {
      this.tips = `执行${cmd}指令`
      try {
        switch (cmd) {
          case 'undo':
          case 'redo':
            this.bpmn.commandStack[cmd]()
            break
          case 'cut':
            this.bpmn.copy(this.activeElement)
            if (this.activeElement) {
              this.bpmn.modeling.removeElements([ this.activeElement ])
            }
            break
          case 'copy':
            this.bpmn.copy(this.activeElement)
            break
          case 'paste':
            this.bpmn.paste()
            break
          case 'delete':
            if (this.activeElement) {
              this.bpmn.modeling.removeElements([ this.activeElement ])
            }
            break
        }
      } catch (e) {
        this.tips = `执行${cmd}指令异常：${e.message}`
      }
    },
    download (type) {
      this.tips = `导出${type}文件`
      const options = { format: true }
      this.bpmn.modeler[type === 'XML' ? 'saveXML' : 'saveSVG'](options).then(bpmn => {
        const url = window.URL.createObjectURL(new Blob([type === 'XML' ? bpmn.xml : bpmn.svg], { type: type === 'XML' ? 'application/xml' : 'image/svg+xml' }))
        const dom = document.createElement('a')
        dom.href = url
        dom.download = this.info.name + (type === 'XML' ? '.bpmn20.xml' : '')
        dom.click()
        window.URL.revokeObjectURL(url)
      }).catch(err => {
        this.$error({ title: '数据获取异常', content: err.message })
      })
    },
    handleImport (file) {
      const reader = new FileReader()
      reader.onload = () => this.bpmnImportXML(reader.result)
      reader.readAsText(file)
      return false
    },
    onWidgetDragStart (widget, event) {
      widget.callback(widget, this.bpmn, event)
    },
    handleToolbar (toolbar, event) {
      this.activeToolbar = toolbar
    },
    async bpmnImportXML (xml, processId = '') {
      if (!xml) {
        xml = XMLTemplate.flowable
        if (!processId) processId = 'fs-' + this.info.id
      }
      try {
        this.tips = '正在解析XML数据...'
        await this.bpmn.modeler.importXML(xml)
        if (processId && this.bpmn.modeler._definitions && this.bpmn.modeler._definitions.rootElements.length > 0) {
          this.bpmn.modeler._definitions.rootElements[0].id = processId
        }
        this.zoom()
      } catch (e) {
        this.$error({ title: '解析XML异常', content: e.message })
      } finally {
        this.tips = '载入XML完成'
        this.activeElement = null
      }
    },
    load () {
      this.loading = true
      this.tips = '正在载入数据信息...'
      workflowService.info({ id: this.$route.query.id, withForm: true }).then(async result => {
        this.loading = false
        if (result.code === 0) {
          this.info = result.data
          this.bpmnImportXML(result.data.content)
        }
      })
    },
    save () {
      const options = { format: true }
      this.bpmn.modeler.saveXML(options).then(bpmn => {
        this.loading = true
        this.tips = '正在保存...'
        workflowService.save({ id: this.$route.query.id, content: bpmn.xml }).then(result => {
          this.loading = false
          this.tips = result.message
        })
      }).catch(err => {
        this.$error({ title: '数据获取异常', content: err.message })
      })
    },
    unHandledRejection (event) {
      this.$error({ title: '未捕获的异常', content: event.reason.message })
    }
  },
  mounted () {
    this.bpmn = new BPMN(this.$refs.canvas, config, true)
    this.bpmn.modeler._container.addEventListener('mouseenter', event => {
      if (this.activeToolbar !== null) {
        this.activeToolbar.callback(this.activeToolbar, this.bpmn, event)
        this.activeToolbar = null
      }
    });
    ['shape.added', 'element.click'].forEach(name => {
      this.bpmn.modeler.on(name, event => {
        if (!event.element || event.element.type === 'label') return true
        if (event.element.parent && this.config.elements[event.element.type]) {
          this.activeElement = event.element
          this.tips = '选中元素 - ' + this.activeElement.id
        } else {
          this.activeElement = null
          this.tips = '选中画布'
        }
        if (name === 'shape.added' && event.element.type === 'bpmn:UserTask') {
          this.bpmn.fixedUserTask(event.element)
        }
        return true
      })
    });
    ['shape.removed', 'connection.removed'].forEach(name => {
      this.bpmn.modeler.on(name, event => {
        if (!event.element || event.element.type === 'label') return true
        if (this.activeElement !== null) {
          this.tips = '选中元素已移除'
        }
        this.activeElement = null
        return true
      })
    });
    ['element.dblclick'].forEach(name => { // 禁用双击editing编辑
      this.bpmn.modeler.on(name, 10000, event => {
        return false
      })
    })
    this.load()
    window.addEventListener('unhandledrejection', this.unHandledRejection)
  },
  destroyed () {
    window.removeEventListener('unhandledrejection', this.unHandledRejection)
  }
}
</script>

<style lang="less" scoped>
@import './bpmn/bpmn.less';

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
        vertical-align: middle;
        border-bottom: solid 1px #e8e8e8;
        .fs-toolbar {
          padding: 0px 15px;
          height: 100%;
          line-height: 100%;
          vertical-align: middle;
          .fs-toolbar-selected {
            background: #e4e7ed;
            color: #409eff;
          }
          .fs-toolbar-icon {
            padding: 3px;
            border-radius: 3px;
            cursor: pointer;
          }
        }
        .fs-info-title {
          padding: 0px 15px;
        }
      }
      .fs-layout-main {
        height: calc(100% - 44px);
        background: #fafafa;
        .fs-layout-canvas {
          width: 100%;
          height: 100%;
          overflow: auto;
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
