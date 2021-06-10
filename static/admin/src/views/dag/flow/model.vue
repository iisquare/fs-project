<template>
  <div class="fs-layout-box">
    <div class="fs-layout-header">
      <a-space>
        <a-button icon="save" @click="save" :loading="loading">保存模型</a-button>
        <a-button icon="deployment-unit" @click="loadTree" :loading="nodeLoading">刷新节点</a-button>
      </a-space>
    </div>
    <div class="fs-layout-content">
      <div class="fs-layout-left">
        <a-tabs default-active-key="node">
          <a-tab-pane key="node" tab="节点">
            <a-tree
              draggable
              :showIcon="true"
              :selectable="false"
              :expandedKeys="nodeExpanded"
              @dragstart="nodeDragStart"
              @expand="expandedKeys => this.nodeExpanded = expandedKeys"
              :tree-data="nodeTree">
              <template slot="icon" slot-scope="item">
                <a-icon :type="item.icon" />
              </template>
            </a-tree>
          </a-tab-pane>
        </a-tabs>
      </div>
      <div class="fs-layout-center">
        <div
          ref="canvas"
          id="flow-chart-canvas"
          @click="triggerCanvas"
          @dragover="canvasDragOver"
          @drop="canvasDrop"
          :style="{width: canvas.width + 'px', height: canvas.height + 'px', top: canvas.top + 'px'}"
        >
          <a-button
            :id="item.id"
            class="canvas-item"
            :icon="item.icon"
            v-for="item in canvasItems"
            :key="item.id"
            @click.stop="triggerCanvasItem(item)"
            @contextmenu.native="ev => handleContextMenu(ev, item)"
            :style="{ left: item.x + 'px', top:item.y + 'px' }"
          >{{ item.name }}</a-button>
        </div>
      </div>
      <div class="fs-layout-right">
        <a-tabs default-active-key="property">
          <a-tab-pane key="property" tab="属性">
            <dag-flow-property v-model="property.data" :grid="property.grid" />
          </a-tab-pane>
        </a-tabs>
      </div>
    </div>
    <div class="fs-layout-footer">
      <a-icon type="bulb" theme="twoTone" />
      <span>{{ tips }}</span>
    </div>
  </div>
</template>

<script>
import nodeService from '@/service/dag/node'
import { triggerWindowResizeEvent } from '@/utils/util'
import { jsPlumb } from 'jsplumb'
import MenuUtil from '@/utils/menu'
import DagFlowProperty from '@/views/dag/flow/property'
import flowService from '@/service/dag/flow'

export default {
  components: { DagFlowProperty },
  data () {
    return {
      loading: false,
      tips: '',
      nodeLastDragged: null,
      nodeLoading: false,
      nodeTree: [],
      nodeExpanded: [],
      nodeMap: {},
      canvas: {
        top: 0,
        width: 400,
        height: 300
      },
      canvasCounter: 0,
      canvasItems: [],
      jsPlumbInstance: null,
      jsPlumbEndpoints: {},
      jsPlumbChannels: [],
      jsPlumbAnchors: ['TopCenter', 'RightMiddle', 'BottomCenter', 'LeftMiddle'],
      property: { element: null, data: {}, grid: [] }
    }
  },
  watch: {
    'property.data': {
      handler (data) {
        if (this.property.element) {
          Object.assign(this.property.element, data)
        }
        if (this.property.element === this.canvas) {
          this.resizeCanvas()
        }
      },
      deep: true
    }
  },
  methods: {
    resizeCanvas () {
      const top = Math.max(0, this.$refs.canvas.parentElement.offsetHeight - this.canvas.height)
      this.canvas.top = top / 2
    },
    triggerCanvasItem (item) {
      if (this.property.element === item) return true
      const node = this.nodeMap[item.parent]
      if (!node) {
        this.$message.warning(`节点信息异常，无法定位[${item.parent}]！`)
        return false
      }
      let grid = [
        { 'name': '节点', 'field': 'id', 'value': '', 'group': '基础信息', 'placeholder': '', 'options': {} },
        { 'name': '别名', 'field': 'alias', 'value': '', 'group': '基础信息', 'editor': 'text', 'placeholder': '', 'options': {} },
        { 'name': '名称', 'field': 'name', 'value': '', 'group': '基础信息', 'editor': 'text', 'placeholder': '', 'options': {} },
        { 'name': '备注', 'field': 'description', 'value': '', 'group': '基础信息', 'editor': 'textarea', 'placeholder': '', 'options': {} },
        { 'name': '父级', 'field': 'parent', 'value': '', 'group': '插件信息', 'editor': 'text', 'placeholder': '', 'options': {} },
        { 'name': '类型', 'field': 'type', 'value': '', 'group': '插件信息', 'editor': 'text', 'placeholder': '', 'options': {} },
        { 'name': '插件', 'field': 'plugin', 'value': '', 'group': '插件信息', 'editor': 'text', 'placeholder': '', 'options': {} },
        { 'name': '类名', 'field': 'classname', 'value': '', 'group': '插件信息', 'editor': 'text', 'placeholder': '', 'options': {} },
        { 'name': '前缀', 'field': 'kvConfigPrefix', 'value': '', 'group': '配置中心', 'editor': 'text', 'placeholder': '', 'options': {} },
        { 'name': '并行度', 'field': 'parallelism', 'value': '', 'group': '配置中心', 'editor': 'number', 'placeholder': '', 'options': {} }
      ]
      grid = grid.concat(node.data.properties)
      grid.push({ 'name': 'JSON', 'field': 'returns', 'value': '', 'group': '字段类型', 'editor': 'textarea', 'placeholder': '', 'options': {} })
      this.tips = `选中节点 ${item.id} - ${item.name}`
      Object.assign(this.property, { element: item, data: item, grid })
    },
    triggerCanvas () {
      if (this.property.element === this.canvas) return true
      const grid = [
        { 'name': '宽度', 'field': 'width', 'value': '', 'group': '画布设置', 'editor': 'number', 'placeholder': '', 'options': {} },
        { 'name': '高度', 'field': 'height', 'value': '', 'group': '画布设置', 'editor': 'number', 'placeholder': '', 'options': {} }
      ]
      this.tips = '选中画布'
      Object.assign(this.property, { element: this.canvas, data: this.canvas, grid })
    },
    handleContextMenu (ev, item) {
      MenuUtil.context(ev, [{ key: 'delete', icon: 'delete', title: '删除节点' }], menu => {
        switch (menu.key) {
          case 'delete':
            return this.canvasItemRemove(item)
          default:
            return false
        }
      })
    },
    canvasItemAdd (item) {
      const result = this.canvasItems.push(item)
      this.jsPlumbChannels.push(() => {
        this.jsPlumbInstance.draggable(item.id, {
          containment: '#flow-chart-canvas',
          stop (ev) {
            [item.x, item.y] = ev.finalPos
          }
        })
        this.jsPlumbAnchors.forEach((anchor) => {
          const el = this.jsPlumbInstance.addEndpoint(item.id, { anchor }, {
            isSource: true,
            isTarget: true,
            maxConnections: -1,
            connectorOverlays: [
              ['Arrow', { width: 10, length: 10, location: 1 }],
              ['Label', { label: 'X', cssClass: 'label-pointer' }]
            ]
          })
          this.jsPlumbEndpoints[item.id + '-' + anchor] = el
        })
      })
      return result
    },
    canvasItemRemove (item) {
      for (const index in this.canvasItems) {
        if (item.id === this.canvasItems[index].id) {
          this.jsPlumbInstance.removeAllEndpoints(item.id)
          this.canvasItems.splice(index, 1)
          return true
        }
      }
      return false
    },
    canvasLineClick (connInfo, originalEvent) {
      if (!originalEvent || !(connInfo instanceof jsPlumb.Connection)) return true
      this.jsPlumbInstance.deleteConnection(connInfo)
      return true
    },
    nodeDragStart ({ ev, node }) {
      this.nodeLastDragged = node
    },
    canvasDragOver (ev) {
      if (this.nodeLastDragged !== null && this.nodeLastDragged.dataRef.draggable) {
        ev.preventDefault()
      }
    },
    canvasDrop (ev) {
      const node = this.nodeLastDragged.dataRef
      const counter = ++this.canvasCounter
      const item = {
        id: `item${counter}`,
        name: `${node.title}[${counter}]`,
        icon: node.icon,
        x: ev.offsetX - 60,
        y: ev.offsetY - 18,
        index: counter,
        description: node.data.description,
        parent: node.key,
        type: node.data.type,
        plugin: node.data.plugin,
        classname: node.data.classname,
        kvConfigPrefix: '',
        parallelism: 0
      }
      this.canvasItemAdd(item)
      this.triggerCanvasItem(item)
    },
    formatTree (data, expandedKeys, map) {
      const result = []
      for (const index in data) {
        const item = data[index]
        item.properties = item.properties ? JSON.parse(item.properties) : []
        const node = {
          key: item.id + '',
          title: item.name,
          data: item,
          scopedSlots: { icon: 'icon' }
        }
        const children = this.formatTree(item.children, expandedKeys, map)
        if (children.length > 0) {
          node.children = children
          node.draggable = false
          node.icon = item.icon || 'folder'
          expandedKeys && item.state !== 'closed' && expandedKeys.push(node.key)
        } else {
          node.draggable = item.draggable === 1
          node.icon = item.icon || 'file'
        }
        result.push(node)
        map[node.key] = node
      }
      return result
    },
    loadTree () {
      this.nodeLoading = true
      this.tips = '正在载入节点信息...'
      nodeService.tree().then(result => {
        this.nodeLoading = false
        this.nodeTree = this.formatTree(result.data, this.nodeExpanded = [], this.nodeMap = {})
        this.tips = '节点数据载入完成'
      })
    },
    load () {
      this.loading = true
      this.tips = '正在载入数据信息...'
      flowService.info({ id: this.$route.query.id }).then(result => {
        try {
          if (result.code !== 0 || result.data.content.length === 0) {
            return false
          }
          const data = JSON.parse(result.data.content)
          Object.assign(this.canvas, data.canvas)
          data.items.forEach(item => {
            this.canvasItemAdd(item)
            if (item.index > this.canvasCounter) {
              this.canvasCounter = item.index
            }
          })
          this.jsPlumbChannels.push(() => {
            data.relations.forEach(item => {
              this.jsPlumbInstance.connect({
                source: this.jsPlumbEndpoints[item.sourceId + '-' + item.sourceAnchor],
                target: this.jsPlumbEndpoints[item.targetId + '-' + item.targetAnchor]
              })
            })
          })
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
      const data = {
        canvas: this.canvas,
        items: this.canvasItems,
        relations: []
      }
      this.jsPlumbInstance.getAllConnections().forEach(connection => {
        const item = { sourceId: connection.sourceId, targetId: connection.targetId }
        connection.endpoints.forEach(endpoint => {
          if (!item['sourceAnchor'] && connection.sourceId === endpoint.anchor.elementId) {
            item['sourceAnchor'] = endpoint.anchor.type
          } else {
            item['targetAnchor'] = endpoint.anchor.type
          }
        })
        data.relations.push(item)
      })
      return data
    },
    save () {
      this.loading = true
      this.tips = '正在保存...'
      const data = this.collect()
      if (data === null) return (this.loading = false)
      flowService.save({ id: this.$route.query.id, content: JSON.stringify(data) }).then(result => {
        this.loading = false
        this.tips = result.message
      })
    }
  },
  mounted () {
    const _this = this
    jsPlumb.ready(() => {
      _this.jsPlumbInstance = jsPlumb.getInstance({
        Endpoint: ['Dot', { radius: 3 }],
        Connector: ['Bezier']
      })
      _this.jsPlumbInstance.bind('click', (connInfo, originalEvent) => {
        _this.canvasLineClick(connInfo, originalEvent)
      })
    })
    window.onresize = () => { this.resizeCanvas() }
    this.loadTree()
    this.load()
  },
  updated () {
    while (this.jsPlumbChannels.length > 0) {
      this.jsPlumbChannels.shift()()
    }
    this.jsPlumbInstance.repaintEverything()
  }
}
</script>

<style lang="less">
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
      width: 230px;
      height: 100%;
      display: inline-block;
      border-right: solid 1px #cbcccc;
      .ant-tabs {
        height: 100%;
      }
      .ant-tabs-bar {
        margin: 0px;
      }
      .ant-tabs-content {
        height: calc(100% - 44px);
        overflow: auto;
        padding: 3px 5px 3px 5px;
      }
      .ant-tree-iconEle {
        margin-top: -2px;
      }
    }
    .fs-layout-center {
      height: 100%;
      width: calc(100% - 580px);
      display: inline-block;
      position: relative;
      overflow: auto;
      background-color: #f8f9fa;
      #flow-chart-canvas {
        margin: 0 auto;
        position: relative;
        min-width: 300px;
        min-height: 300px;
        background: url(../../../assets/grid.gif);
        .canvas-item {
          position: absolute;
        }
        .label-pointer {
          cursor: pointer;
        }
      }
    }
    .fs-layout-right {
      height: 100%;
      width: 350px;
      display: inline-block;
      border-left: solid 1px #cbcccc;
      .ant-tabs {
        height: 100%;
      }
      .ant-tabs-bar {
        margin: 0px;
      }
      .ant-tabs-content {
        height: calc(100% - 44px);
        overflow-y: scroll;
      }
      .ant-form-item {
        margin-bottom: 10px;
        padding-bottom: 10px;
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
