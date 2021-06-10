import {
  assign,
  isArray
} from 'min-dash'

import {
  isExpanded
} from 'bpmn-js/lib/util/DiUtil'

import {
  isAny
} from 'bpmn-js/lib/features/modeling/util/ModelingUtil'

import {
  getChildLanes
} from 'bpmn-js/lib/features/modeling/util/LaneUtil'

import {
  hasPrimaryModifier
} from 'diagram-js/lib/util/Mouse'

/**
 * A provider for BPMN 2.0 elements context pad
 */
export default function ContextPadProvider (
    config, injector, eventBus,
    contextPad, modeling, elementFactory,
    connect, create, popupMenu,
    canvas, rules, translate) {
  config = config || {}

  contextPad.registerProvider(this)

  this._contextPad = contextPad

  this._modeling = modeling

  this._elementFactory = elementFactory
  this._connect = connect
  this._create = create
  this._popupMenu = popupMenu
  this._canvas = canvas
  this._rules = rules
  this._translate = translate

  if (config.autoPlace !== false) {
    this._autoPlace = injector.get('autoPlace', false)
  }

  eventBus.on('create.end', 250, function (event) {
    const context = event.context
    const shape = context.shape

    if (!hasPrimaryModifier(event) || !contextPad.isOpen(shape)) {
      return
    }

    var entries = contextPad.getEntries(shape)

    if (entries.replace) {
      entries.replace.action.click(event, shape)
    }
  })
}

ContextPadProvider.$inject = [
  'config.contextPad',
  'injector',
  'eventBus',
  'contextPad',
  'modeling',
  'elementFactory',
  'connect',
  'create',
  'popupMenu',
  'canvas',
  'rules',
  'translate'
]

ContextPadProvider.prototype.getContextPadEntries = function (element) {
  const contextPad = this._contextPad
  const modeling = this._modeling
  const connect = this._connect
  const rules = this._rules
  const translate = this._translate

  var actions = {}

  if (element.type === 'label') {
    return actions
  }

  var businessObject = element.businessObject

  function startConnect (event, element) {
    connect.start(event, element)
  }

  function removeElement (e) {
    modeling.removeElements([ element ])
  }

  function splitLaneHandler (count) {
    return function (event, element) {
      // actual split
      modeling.splitLane(element, count)

      // refresh context pad after split to
      // get rid of split icons
      contextPad.open(element, true)
    }
  }

  if (isAny(businessObject, [ 'bpmn:Lane', 'bpmn:Participant' ]) && isExpanded(businessObject)) {
    var childLanes = getChildLanes(element)

    assign(actions, {
      'lane-insert-above': {
        group: 'lane-insert-above',
        className: 'bpmn-icon-lane-insert-above',
        title: translate('Add Lane above'),
        action: {
          click: function (event, element) {
            modeling.addLane(element, 'top')
          }
        }
      }
    })

    if (childLanes.length < 2) {
      if (element.height >= 120) {
        assign(actions, {
          'lane-divide-two': {
            group: 'lane-divide',
            className: 'bpmn-icon-lane-divide-two',
            title: translate('Divide into two Lanes'),
            action: {
              click: splitLaneHandler(2)
            }
          }
        })
      }

      if (element.height >= 180) {
        assign(actions, {
          'lane-divide-three': {
            group: 'lane-divide',
            className: 'bpmn-icon-lane-divide-three',
            title: translate('Divide into three Lanes'),
            action: {
              click: splitLaneHandler(3)
            }
          }
        })
      }
    }

    assign(actions, {
      'lane-insert-below': {
        group: 'lane-insert-below',
        className: 'bpmn-icon-lane-insert-below',
        title: translate('Add Lane below'),
        action: {
          click: function (event, element) {
            modeling.addLane(element, 'bottom')
          }
        }
      }
    })
  }

  if (isAny(businessObject, [
    'bpmn:FlowNode',
    'bpmn:InteractionNode',
    'bpmn:DataObjectReference',
    'bpmn:DataStoreReference'
  ])) {
    assign(actions, {
      'connect': {
        group: 'connect',
        className: 'bpmn-icon-connection-multi',
        title: translate('Connect using ' +
                  (businessObject.isForCompensation ? '' : 'Sequence/MessageFlow or ') +
                  'Association'),
        action: {
          click: startConnect,
          dragstart: startConnect
        }
      }
    })
  }

  if (isAny(businessObject, [ 'bpmn:DataObjectReference', 'bpmn:DataStoreReference' ])) {
    assign(actions, {
      'connect': {
        group: 'connect',
        className: 'bpmn-icon-connection-multi',
        title: translate('Connect using DataInputAssociation'),
        action: {
          click: startConnect,
          dragstart: startConnect
        }
      }
    })
  }

  // delete element entry, only show if allowed by rules
  var deleteAllowed = rules.allowed('elements.delete', { elements: [ element ] })

  if (isArray(deleteAllowed)) {
    // was the element returned as a deletion candidate?
    deleteAllowed = deleteAllowed[0] === element
  }

  if (deleteAllowed) {
    assign(actions, {
      'delete': {
        group: 'edit',
        className: 'bpmn-icon-trash',
        title: translate('Remove'),
        action: {
          click: removeElement
        }
      }
    })
  }

  return actions
}
