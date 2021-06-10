import BpmnJS from 'bpmn-js/lib/Modeler'
import PaletteProvider from './palette'
import ContextPadProvider from './context-pad'
import DrawModule from './draw'
import RulesModule from './rules'
import FlowableExtension from './flowable.json'

class BPMN {
  constructor (container, config) {
    this.modeler = new BpmnJS({
      container,
      additionalModules: [PaletteProvider, ContextPadProvider, DrawModule, RulesModule],
      moddleExtensions: { flowable: FlowableExtension }
    })
    this.config = config
    this.canvas = this.modeler.get('canvas')
    this.moddle = this.modeler.get('moddle')
    this.modeling = this.modeler.get('modeling')
    this.palette = this.modeler.get('paletteProvider')
    this.contextPad = this.modeler.get('contextPadProvider')
    this.bpmnFactory = this.modeler.get('bpmnFactory')
  }

  parseCDATA (data) {
    if (!data) return ''
    data = data.replace(/<!\[CDATA\[(.+)\]\]>/, '$1')
    data = data.replace(/&lt;!\[CDATA\[(.+)\]\]&gt;/, '$1')
    return data
  }

  expressBoolean (value) {
    const type = Object.prototype.toString.call(value)
    if (type === '[object Boolean]') return value
    if (type === '[object Boolean]') return value === 'true'
    return false
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
