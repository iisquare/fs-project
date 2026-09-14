/**
 * 调色板提供者 - 默认调色板在界面中通过样式隐藏，此处保留实例以便执行器调用内部的
 * create/elementFactory/tool 等能力（左侧组件库与工具栏依赖这些内部实例）
 */
export default class PaletteProvider {

  static $inject = [
    'palette',
    'create',
    'elementFactory',
    'spaceTool',
    'lassoTool',
    'handTool',
    'globalConnect',
    'translate'
  ]

  private _palette: any
  private _create: any
  private _elementFactory: any
  private _spaceTool: any
  private _lassoTool: any
  private _handTool: any
  private _globalConnect: any
  private _translate: any

  constructor (palette: any, create: any, elementFactory: any, spaceTool: any, lassoTool: any,
    handTool: any, globalConnect: any, translate: any) {
    this._palette = palette
    this._create = create
    this._elementFactory = elementFactory
    this._spaceTool = spaceTool
    this._lassoTool = lassoTool
    this._handTool = handTool
    this._globalConnect = globalConnect
    this._translate = translate

    palette.registerProvider(this)
  }

  getPaletteEntries (element: any) {
    const actions: any = {}
    const create = this._create
    const elementFactory = this._elementFactory
    const spaceTool = this._spaceTool
    const lassoTool = this._lassoTool
    const handTool = this._handTool
    const globalConnect = this._globalConnect
    const translate = this._translate

    function createAction (type: string, group: string, className: string, title: string, options?: any) {
      function createListener (event: any) {
        const shape = elementFactory.createShape(Object.assign({ type }, options))
        if (options) {
          shape.businessObject.di.isExpanded = options.isExpanded
        }
        create.start(event, shape)
      }

      const shortType = type.replace(/^bpmn:/, '')

      return {
        group,
        className,
        title: title || translate('Create {type}', { type: shortType }),
        action: {
          dragstart: createListener,
          click: createListener
        }
      }
    }

    function createSubprocess (event: any) {
      const subProcess = elementFactory.createShape({
        type: 'bpmn:SubProcess',
        x: 0,
        y: 0,
        isExpanded: true
      })

      const startEvent = elementFactory.createShape({
        type: 'bpmn:StartEvent',
        x: 40,
        y: 82,
        parent: subProcess
      })

      create.start(event, [subProcess, startEvent], {
        hints: {
          autoSelect: [startEvent]
        }
      })
    }

    function createParticipant (event: any) {
      create.start(event, elementFactory.createParticipantShape())
    }

    Object.assign(actions, {
      'hand-tool': {
        group: 'tools',
        className: 'bpmn-icon-hand-tool',
        title: translate('Activate the hand tool'),
        action: {
          click (event: any) {
            handTool.activateHand(event)
          }
        }
      },
      'lasso-tool': {
        group: 'tools',
        className: 'bpmn-icon-lasso-tool',
        title: translate('Activate the lasso tool'),
        action: {
          click (event: any) {
            lassoTool.activateSelection(event)
          }
        }
      },
      'space-tool': {
        group: 'tools',
        className: 'bpmn-icon-space-tool',
        title: translate('Activate the create/remove space tool'),
        action: {
          click (event: any) {
            spaceTool.activateSelection(event)
          }
        }
      },
      'global-connect-tool': {
        group: 'tools',
        className: 'bpmn-icon-connection-multi',
        title: translate('Activate the global connect tool'),
        action: {
          click (event: any) {
            globalConnect.toggle(event)
          }
        }
      },
      'tool-separator': {
        group: 'tools',
        separator: true
      },
      'create.start-event': createAction(
        'bpmn:StartEvent', 'event', 'bpmn-icon-start-event-none', translate('Create StartEvent')
      ),
      'create.intermediate-event': createAction(
        'bpmn:IntermediateThrowEvent', 'event', 'bpmn-icon-intermediate-event-none', translate('Create Intermediate/Boundary Event')
      ),
      'create.end-event': createAction(
        'bpmn:EndEvent', 'event', 'bpmn-icon-end-event-none', translate('Create EndEvent')
      ),
      'create.exclusive-gateway': createAction(
        'bpmn:ExclusiveGateway', 'gateway', 'bpmn-icon-gateway-none', translate('Create Gateway')
      ),
      'create.task': createAction(
        'bpmn:Task', 'activity', 'bpmn-icon-task', translate('Create Task')
      ),
      'create.data-object': createAction(
        'bpmn:DataObjectReference', 'data-object', 'bpmn-icon-data-object', translate('Create DataObjectReference')
      ),
      'create.data-store': createAction(
        'bpmn:DataStoreReference', 'data-store', 'bpmn-icon-data-store', translate('Create DataStoreReference')
      ),
      'create.subprocess-expanded': {
        group: 'activity',
        className: 'bpmn-icon-subprocess-expanded',
        title: translate('Create expanded SubProcess'),
        action: {
          dragstart: createSubprocess,
          click: createSubprocess
        }
      },
      'create.participant-expanded': {
        group: 'collaboration',
        className: 'bpmn-icon-participant',
        title: translate('Create Pool/Participant'),
        action: {
          dragstart: createParticipant,
          click: createParticipant
        }
      },
      'create.group': createAction(
        'bpmn:Group', 'artifact', 'bpmn-icon-group', translate('Create Group')
      )
    })

    return actions
  }
}

export const PaletteModule = {
  __init__: ['paletteProvider'],
  paletteProvider: ['type', PaletteProvider]
}
