<script setup lang="ts">
/**
 * 紧凑参数行 - 左侧参数名（可选带启用开关），右侧取值控件，一行放一个参数，
 * 避免「标题一行 + 取值一行」占掉两倍高度。
 *
 * 取值控件默认靠右（数字、按钮组等短控件）；`wide` 时占满剩余宽度（模型名称等文本输入）。
 *
 * @prop {String}  label      - 参数名
 * @prop {Boolean} switchable - 是否展示启用开关（默认 false）
 * @prop {Boolean} wide       - 取值控件是否占满剩余宽度（默认 false，靠右）
 * @v-model:enabled {Boolean} - 启用状态（switchable 为 true 时使用）
 * @slot default - 取值控件
 */
const enabled = defineModel<boolean>('enabled', { default: true })
const {
  label = '',
  switchable = false,
  wide = false,
} = defineProps<{
  label?: string,
  switchable?: boolean,
  wide?: boolean,
}>()
</script>

<template>
  <el-form-item class="param-form-item" label="">
    <div class="param-row" :class="{ 'is-wide': wide }">
      <span class="param-name">{{ label }}</span>
      <el-switch v-if="switchable" v-model="enabled" />
      <div class="param-value">
        <slot></slot>
      </div>
    </div>
  </el-form-item>
</template>

<style lang="scss" scoped>
.param-row {
  width: 100%;
  min-height: 24px;
  /* 面板变窄时行内元素可收缩，避免撑出横向滚动条 */
  min-width: 0;
  @include flex-start();
  gap: 10px;
  .param-name {
    /* 参数名列保持基准宽度，必要时才收缩 */
    flex: 0 1 58px;
    min-width: 0;
    font-size: 12px;
    font-weight: 500;
    color: var(--el-text-color-regular);
    @include text-wrap();
  }
  .el-switch {
    flex: none;
  }
  /* 取值控件靠右：短控件（数字、下拉）只占自身宽度 */
  .param-value {
    display: flex;
    align-items: center;
    justify-content: flex-end;
    margin-left: auto;
    flex: 0 1 auto;
    min-width: 0;
    /**
     * 插槽里的 Element Plus 控件不会带上本组件的作用域属性（DOM 上只有 .el-select，没有 data-v-xxx），
     * 所以必须用 :deep 穿透，否则宽度规则静默失效：下拉会被压到只剩箭头的宽度，
     * 选中项文本被箭头盖住，看起来就是「选中后没有内容」。限定直接子元素，避免影响控件内部结构。
     */
    :deep(> .el-select),
    :deep(> .el-autocomplete),
    :deep(> .el-input) {
      width: 140px;
      max-width: 100%;
    }
    :deep(> .el-input-number) {
      width: 84px;
      max-width: 100%;
    }
    :deep(> .el-input-number .el-input__inner) {
      text-align: right;
    }
  }
  /* 文本类控件占满剩余宽度 */
  &.is-wide {
    .param-value {
      flex: 1;
      min-width: 0;
      :deep(> .el-select),
      :deep(> .el-autocomplete),
      :deep(> .el-input) {
        width: 100%;
      }
    }
  }
}

.param-form-item {
  margin-bottom: 10px;
}
</style>
