import BpmnJSViewer from './Viewer'
import BpmnJSModeler from 'bpmn-js/lib/Modeler'
import PaletteProvider from './palette'
import ContextPadProvider from './context-pad'
import DrawModule from './draw'
import RulesModule from './rules'
import FlowableExtension from './flowable.json'

class BPMN {
  constructor (container, config, editable) {
    if (editable) {
      this.modeler = new BpmnJSModeler({
        container,
        additionalModules: [PaletteProvider, ContextPadProvider, DrawModule, RulesModule],
        moddleExtensions: { flowable: FlowableExtension }
      })
      this.palette = this.modeler.get('paletteProvider')
      this.copyPaste = this.modeler.get('copyPaste')
      this.clipboard = this.modeler.get('clipboard')
      this.contextPad = this.modeler.get('contextPadProvider')
      this.bpmnFactory = this.modeler.get('bpmnFactory')
      this.commandStack = this.modeler.get('commandStack')
    } else {
      this.modeler = new BpmnJSViewer({
        container,
        moddleExtensions: { flowable: FlowableExtension }
      })
    }
    this.config = config
    this.canvas = this.modeler.get('canvas')
    this.moddle = this.modeler.get('moddle')
    this.modeling = this.modeler.get('modeling')
    this.elementRegistry = this.modeler.get('elementRegistry')
    // 功能性变量
    this._elementCopied = null
  }

  copy (element) {
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

  colorful (historicActivityInstances) {
    for (const instance of historicActivityInstances) {
      if (instance.activityType === 'sequenceFlow') continue
      const color = instance.endTime ? (instance.deleteReason === null ? 'green' : 'red') : 'orange'
      const element = this.elementRegistry.get(instance.activityId)
      this.modeling.setColor(element, { stroke: color })
    }
  }

  parseCDATA (data) {
    if (!data) return ''
    data = data.replace(/^<!\[CDATA\[/, '').replace(/\]\]>$/, '')
    data = data.replace(/^&lt;!\[CDATA\[/, '').replace(/(.+)\]\]&gt;$/, '')
    return data
  }

  createDocumentation (documentation) {
    if (!documentation) return []
    return [this.moddle.create('bpmn:Documentation', { text: documentation })]
  }

  parseDocumentation (element) {
    const obj = element.businessObject
    if (!('documentation' in obj)) return ''
    return obj.documentation.map(item => item.text).join(',')
  }

  /**
   * UserTask的isSequential解析到了multiInstanceLoopCharacteristics属性上
   * 直接保存会抛Uncaught (in promise) TypeError异常，无法catch捕获
   * 通过把isSequential转移到multiInstanceLoopCharacteristics.$attrs中可解决
   */
  fixedUserTask (element) {
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
    // this.modeling.updateProperties(element, { loopCharacteristics: multiInstanceLoopCharacteristics })
    return true
  }
}

export default BPMN
