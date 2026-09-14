import BpmnJSModeler from 'bpmn-js/lib/Modeler'
import BpmnJSViewer from 'bpmn-js/lib/Viewer'
import { PaletteModule } from './PaletteProvider'
import { ContextPadModule } from './ContextPadProvider'
import FlowableExtension from './flowable'
import './assets'

/**
 * bpmn-js 封装 - editable 为 true 时创建建模器，否则创建只读查看器
 */
export default class BpmnModeler {

  modeler: any
  config: any
  palette: any = null
  copyPaste: any = null
  clipboard: any = null
  contextPad: any = null
  bpmnFactory: any = null
  commandStack: any = null
  canvas: any
  moddle: any
  modeling: any = null
  elementRegistry: any

  // 功能性变量：记录已复制的元素
  _elementCopied: any = null

  constructor (container: any, config: any, editable = true) {
    if (editable) {
      this.modeler = new BpmnJSModeler({
        container,
        additionalModules: [PaletteModule, ContextPadModule],
        moddleExtensions: { flowable: FlowableExtension }
      })
      this.palette = this.modeler.get('paletteProvider')
      this.copyPaste = this.modeler.get('copyPaste')
      this.clipboard = this.modeler.get('clipboard')
      this.contextPad = this.modeler.get('contextPadProvider')
      this.bpmnFactory = this.modeler.get('bpmnFactory')
      this.commandStack = this.modeler.get('commandStack')
      this.modeling = this.modeler.get('modeling')
    } else {
      this.modeler = new BpmnJSViewer({
        container,
        moddleExtensions: { flowable: FlowableExtension }
      })
    }
    this.config = config
    this.canvas = this.modeler.get('canvas')
    this.moddle = this.modeler.get('moddle')
    this.elementRegistry = this.modeler.get('elementRegistry')
  }

  copy (element: any) {
    if (!element) return false
    this.copyPaste.copy(element)
    this._elementCopied = this.clipboard.get()
    return true
  }

  paste () {
    if (!this._elementCopied) return false
    this.clipboard.set(this._elementCopied)
    this.copyPaste.paste({ point: { x: 0, y: 0 } })
    return true
  }

  highlight (colors: Record<string, string>) {
    for (const id in colors) {
      const color = colors[id]
      if (!color) continue
      const element = this.elementRegistry.get(id)
      if (!element) continue
      if (this.modeling) {
        this.modeling.setColor(element, { stroke: color })
        continue
      }
      const gfx = this.canvas.getGraphics(element)
      const target = gfx && gfx.querySelector('.djs-visual > :nth-child(1)')
      if (target) target.style.stroke = color
    }
  }

  /**
   * 历史活动着色：已完成-绿色、已删除-红色、进行中-橙色
   */
  colorful (historicActivityInstances: any[]) {
    const colors: Record<string, string> = {}
    for (const instance of historicActivityInstances || []) {
      if (instance.activityType === 'sequenceFlow') continue
      colors[instance.activityId] = instance.endTime ? (instance.deleteReason === null ? 'green' : 'red') : 'orange'
    }
    this.highlight(colors)
  }

  parseCDATA (data: any) {
    if (!data) return ''
    data = data.replace(/^<!\[CDATA\[/, '').replace(/\]\]>$/, '')
    data = data.replace(/^&lt;!\[CDATA\[/, '').replace(/(.+)\]\]&gt;$/, '')
    return data
  }

  createDocumentation (documentation: any) {
    if (!documentation) return []
    return [this.moddle.create('bpmn:Documentation', { text: documentation })]
  }

  parseDocumentation (element: any) {
    const obj = element.businessObject
    if (!('documentation' in obj)) return ''
    return obj.documentation.map((item: any) => item.text).join(',')
  }

  /**
   * UserTask的isSequential解析到了multiInstanceLoopCharacteristics属性上
   * 直接保存会抛Uncaught (in promise) TypeError异常，无法catch捕获
   * 通过把isSequential转移到multiInstanceLoopCharacteristics.$attrs中可解决
   */
  fixedUserTask (element: any) {
    const obj = element.businessObject
    const multiInstanceLoopCharacteristics = obj.loopCharacteristics
    if (!multiInstanceLoopCharacteristics) return false
    const loop = multiInstanceLoopCharacteristics.$attrs
    if (Object.keys(loop).indexOf('isSequential') !== -1) return true
    Object.assign(multiInstanceLoopCharacteristics.$attrs, {
      isSequential: !!multiInstanceLoopCharacteristics.isSequential,
      loopCardinality: multiInstanceLoopCharacteristics.loopCardinality || ''
    })
    delete multiInstanceLoopCharacteristics.isSequential
    delete multiInstanceLoopCharacteristics.loopCardinality
    element.loopCharacteristics = multiInstanceLoopCharacteristics
    return true
  }
}
