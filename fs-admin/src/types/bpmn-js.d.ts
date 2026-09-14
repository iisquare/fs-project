// bpmn-js 8.x 未内置 TypeScript 类型声明，此处按使用到的入口进行声明
declare module 'bpmn-js/lib/Modeler' {
  const Modeler: any
  export default Modeler
}

declare module 'bpmn-js/lib/Viewer' {
  const Viewer: any
  export default Viewer
}

declare module 'bpmn-js/lib/util/DiUtil' {
  export function isExpanded(element: any): boolean
}

declare module 'bpmn-js/lib/features/modeling/util/ModelingUtil' {
  export function isAny(element: any, types: string[]): boolean
}

declare module 'bpmn-js/lib/features/modeling/util/LaneUtil' {
  export function getChildLanes(element: any): any[]
}
