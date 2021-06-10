/**
 * Custom icon list
 * All icons are loaded here for easy management
 * @see https://vue.ant.design/components/icon/#Custom-Font-Icon
 *
 * 自定义图标加载表
 * 所有图标均从这里加载，方便管理
 */
import bxAnalyse from '@/assets/icons/bx-analyse.svg?inline' // path to your '*.svg?inline' file.

import fromButton from '@/assets/icons/form/form-button.svg?inline'
import formCascader from '@/assets/icons/form/form-cascader.svg?inline'
import formCheckbox from '@/assets/icons/form/form-checkbox.svg?inline'
import formColor from '@/assets/icons/form/form-color.svg?inline'
import formComponent from '@/assets/icons/form/form-component.svg?inline'
import formDate from '@/assets/icons/form/form-date.svg?inline'
import formDateRange from '@/assets/icons/form/form-date-range.svg?inline'
import formInput from '@/assets/icons/form/form-input.svg?inline'
import formNumber from '@/assets/icons/form/form-number.svg?inline'
import formPassword from '@/assets/icons/form/form-password.svg?inline'
import formRadio from '@/assets/icons/form/form-radio.svg?inline'
import formRate from '@/assets/icons/form/form-rate.svg?inline'
import formRichText from '@/assets/icons/form/form-rich-text.svg?inline'
import formRow from '@/assets/icons/form/form-row.svg?inline'
import formSelect from '@/assets/icons/form/form-select.svg?inline'
import formSlider from '@/assets/icons/form/form-slider.svg?inline'
import formSwitch from '@/assets/icons/form/form-switch.svg?inline'
import formTable from '@/assets/icons/form/form-table.svg?inline'
import formTextarea from '@/assets/icons/form/form-textarea.svg?inline'
import formTime from '@/assets/icons/form/form-time.svg?inline'
import formTimeRange from '@/assets/icons/form/form-time-range.svg?inline'
import formUpload from '@/assets/icons/form/form-upload.svg?inline'
import formTxt from '@/assets/icons/form/form-txt.svg?inline'
import formHtml from '@/assets/icons/form/form-html.svg?inline'
import formWrite from '@/assets/icons/form/form-write.svg?inline'
import formDivider from '@/assets/icons/form/form-divider.svg?inline'
import devicePc from '@/assets/icons/device/device-pc.svg?inline'
import deviceMobile from '@/assets/icons/device/device-mobile.svg?inline'
import deviceTablet from '@/assets/icons/device/device-tablet.svg?inline'
import workflowStartEvent from '@/assets/icons/workflow/workflow-start-event.svg?inline'
import workflowEndEvent from '@/assets/icons/workflow/workflow-end-event.svg?inline'
import workflowUserTask from '@/assets/icons/workflow/workflow-user-task.svg?inline'
import workflowExclusiveGateway from '@/assets/icons/workflow/workflow-exclusive-gateway.svg?inline'
import workflowParallelGateway from '@/assets/icons/workflow/workflow-parallel-gateway.svg?inline'
import workflowInclusiveGateway from '@/assets/icons/workflow/workflow-inclusive-gateway.svg?inline'
import workflowPool from '@/assets/icons/workflow/workflow-pool.svg?inline'
import workflowGroup from '@/assets/icons/workflow/workflow-group.svg?inline'
import actionAlignHorizontal from '@/assets/icons/action/action-align-horizontal.svg?inline'
import actionAlignVertical from '@/assets/icons/action/action-align-vertical.svg?inline'
import actionSameSize from '@/assets/icons/action/action-same-size.svg?inline'
import actionHand from '@/assets/icons/action/action-hand.svg?inline'
import actionLasso from '@/assets/icons/action/action-lasso.svg?inline'
import actionConnection from '@/assets/icons/action/action-connection.svg?inline'
import actionSpace from '@/assets/icons/action/action-space.svg?inline'

// const context = require.context('../assets/icons', false, /\.svg$/)
// const icons = Object.fromEntries(context.keys().map(key => {
//     const strs = key.slice(2, -4).split('-')
//     const str = strs.slice(0, 1).concat(strs.slice(1).map(i => i.slice(0, 1).toUpperCase() + i.slice(1))).join('')
//     const path = context.resolve(key).replace('./src', '@') + '?inline'
//     console.log(str, key, path)
//     return [str, import(path)]
// }))
// console.log(icons)

export default {
    bxAnalyse,
    fromButton,
    formCascader,
    formCheckbox,
    formColor,
    formComponent,
    formDate,
    formDateRange,
    formInput,
    formNumber,
    formPassword,
    formRadio,
    formRate,
    formRichText,
    formRow,
    formSelect,
    formSlider,
    formSwitch,
    formTable,
    formTextarea,
    formTime,
    formTimeRange,
    formUpload,
    formTxt,
    formHtml,
    formWrite,
    formDivider,
    devicePc,
    deviceMobile,
    deviceTablet,
    workflowStartEvent,
    workflowEndEvent,
    workflowUserTask,
    workflowExclusiveGateway,
    workflowParallelGateway,
    workflowInclusiveGateway,
    workflowPool,
    workflowGroup,
    actionAlignHorizontal,
    actionAlignVertical,
    actionSameSize,
    actionHand,
    actionLasso,
    actionConnection,
    actionSpace
}
