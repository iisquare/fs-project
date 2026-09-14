/**
 * Flowable 的 moddle 扩展描述，用于序列化/反序列化 flowable 命名空间下的扩展属性
 */
export default {
  name: 'Flowable',
  uri: 'http://flowable.org/bpmn',
  prefix: 'flowable',
  xml: {
    tagAlias: 'lowerCase'
  },
  associations: [],
  types: [{
    name: 'Authority',
    isAbstract: true,
    extends: ['bpmn:ExtensionElements'],
    properties: [{
      name: 'authority',
      type: 'Expression',
      xml: {
        serialize: 'xsi:type'
      }
    }]
  }],
  emumerations: []
}
