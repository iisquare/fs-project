import inherits from 'inherits'

import BaseModeler from 'bpmn-js/lib/BaseModeler'

import Viewer from 'bpmn-js/lib/Viewer'
import NavigatedViewer from 'bpmn-js/lib/NavigatedViewer'

import KeyboardMoveModule from 'diagram-js/lib/navigation/keyboard-move'
import MoveCanvasModule from 'diagram-js/lib/navigation/movecanvas'
import TouchModule from 'diagram-js/lib/navigation/touch'
import ZoomScrollModule from 'diagram-js/lib/navigation/zoomscroll'

import AlignElementsModule from 'diagram-js/lib/features/align-elements'
import AutoPlaceModule from 'bpmn-js/lib/features/auto-place'
import AutoResizeModule from 'bpmn-js/lib/features/auto-resize'
import AutoScrollModule from 'diagram-js/lib/features/auto-scroll'
import DistributeElementsModule from 'bpmn-js/lib/features/distribute-elements'
import GridSnappingModule from 'bpmn-js/lib/features/grid-snapping'
import InteractionEventsModule from 'bpmn-js/lib/features/interaction-events'
import KeyboardModule from 'bpmn-js/lib/features/keyboard'
import KeyboardMoveSelectionModule from 'diagram-js/lib/features/keyboard-move-selection'
import ModelingModule from 'bpmn-js/lib/features/modeling'
import MoveModule from 'diagram-js/lib/features/move'
import ResizeModule from 'diagram-js/lib/features/resize'
import SnappingModule from 'bpmn-js/lib/features/snapping'

export default function Modeler (options) {
  BaseModeler.call(this, options)
}

inherits(Modeler, BaseModeler)

Modeler.Viewer = Viewer
Modeler.NavigatedViewer = NavigatedViewer

Modeler.prototype._interactionModules = [
  KeyboardMoveModule,
  MoveCanvasModule,
  TouchModule,
  ZoomScrollModule
]

Modeler.prototype._modelingModules = [
  AlignElementsModule,
  AutoPlaceModule,
  AutoScrollModule,
  AutoResizeModule,
  DistributeElementsModule,
  GridSnappingModule,
  InteractionEventsModule,
  KeyboardModule,
  KeyboardMoveSelectionModule,
  ModelingModule,
  MoveModule,
  ResizeModule,
  SnappingModule
]

Modeler.prototype._modules = [].concat(
  Viewer.prototype._modules,
  Modeler.prototype._interactionModules,
  Modeler.prototype._modelingModules
)
