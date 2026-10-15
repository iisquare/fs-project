<script setup lang="ts">
/**
 * 上下文用量圆环：输入框发送按钮左边的一小圈，画的是「下一轮请求大约要占多少 / 窗口多大」。
 *
 * - 窗口大小优先取手工指定，其次服务端在模型清单里声明的上下文长度（见 settings store）；
 * - 已用量是客户端按字符折算的估算值（客户端没有分词器，见 `core/Tokens`），只作体量提示；
 * - 服务端没声明、也没手工指定时窗口未知，圆环退化成灰色问号，点开气泡填一个窗口即可；
 * - 点圆环打开气泡：改窗口大小或回到服务端声明的值——值写到哪儿由调用方决定
 *   （对话页把它固化进项目配置，见 `views/home/HomePage.vue`）。
 */
import { computed, ref } from 'vue'
import { formatTokens } from '@renderer/core/Tokens'

export interface ContextInfo {
  /** 当前模型名：手工指定的窗口按模型名存，换模型各记各的 */
  model: string
  /** 下一轮请求的用量（词元数，估算） */
  used: number
  /** 生效的窗口大小：0 表示未知 */
  limit: number
  /** 服务端声明的窗口大小：0 表示服务端没给 */
  declared: number
  /** 生效的窗口是否来自手工指定（项目配置里的那份） */
  custom: boolean
}

const props = defineProps<{ context: ContextInfo }>()

/** 保存 / 恢复：value 为 null 表示删掉手工值，回到服务端声明的窗口 */
const emit = defineEmits<{ 'update:limit': [value: number | null] }>()

/**
 * 圆环几何：32px 见方、半径 13、描边 3——圈里的直径够放三位数加百分号（如 100%），
 * 尺寸也与发送按钮（32px 的圆形按钮）齐平。
 */
const SIZE = 32
const CENTER = SIZE / 2
const RADIUS = 13
const STROKE = 3
const CIRCUMFERENCE = 2 * Math.PI * RADIUS

/** 面板里的输入：打开面板时按当前口径预填 */
const draft = ref<number | undefined>(undefined)
const panel = ref(false)

/** 常用窗口：点一下就填上，省得手敲一长串数字（按惯例取 2 的幂，标签用 K / M 的常用叫法） */
const PRESETS = [
  { value: 32768, label: '32K' },
  { value: 65536, label: '64K' },
  { value: 131072, label: '128K' },
  { value: 262144, label: '256K' },
  { value: 1048576, label: '1M' }
]

const known = computed(() => props.context.limit > 0)
/** 真实占比：可能超过 100%（已用量比窗口还大），数字照实给 */
const raw = computed(() => (known.value ? props.context.used / props.context.limit : 0))
/** 圆环画的比例：超出窗口时也就画满一圈，超出的部分由数字表达 */
const ratio = computed(() => Math.max(0, Math.min(1, raw.value)))
const percent = computed(() => Math.max(0, Math.round(raw.value * 100)))
/** 用量的三档：过半提醒、九成告警，颜色与圆环本体一致 */
const level = computed(() => {
  if (!known.value) return 'unknown'
  if (percent.value >= 90) return 'danger'
  if (percent.value >= 70) return 'warn'
  return 'ok'
})
const dash = computed(() => `${CIRCUMFERENCE * ratio.value} ${CIRCUMFERENCE}`)
/** 圆环里那一眼能看到的东西：完整百分比；窗口未知时给问号，大到放不下时才收成 999+ */
const label = computed(() => {
  if (!known.value) return '?'
  return percent.value > 999 ? '999+' : `${percent.value}%`
})
const tip = computed(() => {
  if (!known.value) {
    return `上下文用量约 ${formatTokens(props.context.used)} 词元；窗口大小未知，点这里指定`
  }
  return `上下文用量约 ${formatTokens(props.context.used)} / ${formatTokens(props.context.limit)} 词元（${percent.value}%）`
})
/** 面板里进度条的百分比：Element Plus 只认 0-100，超出部分一样按满格画 */
const usedPercent = computed(() => Math.min(100, percent.value))

/** 打开面板：按当前生效值预填（没声明也没指定就留空，由用户填） */
function open(): void {
  draft.value = known.value ? props.context.limit : undefined
}

function save(): void {
  const value = Math.floor(Number(draft.value ?? 0))
  if (!Number.isFinite(value) || value <= 0) return
  // 填的正好等于服务端声明的值：等同恢复默认，不留一条没意义的手工值
  emit('update:limit', value === props.context.declared ? null : value)
  panel.value = false
}

function reset(): void {
  emit('update:limit', null)
  panel.value = false
}
</script>

<template>
  <el-popover
    v-model:visible="panel"
    trigger="click"
    placement="top-end"
    :width="300"
    popper-class="context-ring-panel"
    @show="open"
  >
    <template #reference>
      <button type="button" class="context-ring" :class="`is-${level}`" :title="tip">
        <svg :viewBox="`0 0 ${SIZE} ${SIZE}`" :width="SIZE" :height="SIZE" aria-hidden="true">
          <!-- 底圈：整圈浅色，表示窗口总量 -->
          <circle
            class="ring-track"
            :cx="CENTER"
            :cy="CENTER"
            :r="RADIUS"
            fill="none"
            :stroke-width="STROKE"
          />
          <!-- 进度圈：从 12 点方向顺时针画，长度按用量占比 -->
          <circle
            class="ring-value"
            :cx="CENTER"
            :cy="CENTER"
            :r="RADIUS"
            fill="none"
            :stroke-width="STROKE"
            stroke-linecap="round"
            :stroke-dasharray="dash"
            :transform="`rotate(-90 ${CENTER} ${CENTER})`"
          />
          <text
            class="ring-text"
            :x="CENTER"
            :y="CENTER"
            text-anchor="middle"
            dominant-baseline="central"
          >
            {{ label }}
          </text>
        </svg>
      </button>
    </template>

    <div class="ring-panel">
      <div class="panel-title">上下文窗口</div>
      <div class="panel-model">{{ context.model || '未选择模型' }}</div>
      <div class="panel-row">
        <span>已用</span>
        <span>{{ formatTokens(context.used) }} 词元</span>
      </div>
      <div class="panel-row">
        <span>服务端声明</span>
        <span>{{
          context.declared > 0 ? `${formatTokens(context.declared)} 词元` : '未声明'
        }}</span>
      </div>
      <el-progress
        v-if="context.limit > 0"
        class="panel-progress"
        :percentage="usedPercent"
        :stroke-width="6"
        :show-text="false"
        :status="level === 'danger' ? 'exception' : level === 'warn' ? 'warning' : 'success'"
      />

      <div class="panel-field">
        <span class="field-label">本项目指定</span>
        <el-input-number
          v-model="draft"
          class="field-input"
          size="small"
          :min="1"
          :step="1024"
          :controls="false"
          placeholder="词元数"
        />
      </div>
      <div class="panel-presets">
        <el-button
          v-for="item in PRESETS"
          :key="item.value"
          size="small"
          text
          @click="draft = item.value"
        >
          {{ item.label }}
        </el-button>
      </div>

      <div class="panel-actions">
        <el-button size="small" text :disabled="!context.custom" @click="reset">
          按服务端声明
        </el-button>
        <el-button size="small" type="primary" :disabled="!draft" @click="save">保存</el-button>
      </div>
      <div class="panel-tip">
        <div>窗口按「项目指定 > 服务端声明」生效，保存在项目配置里，本项目共用。</div>
        <div>用量按字符折算，仅供参考；超出窗口后较早的内容会被丢弃，或直接被服务端拒绝。</div>
        <div>待发送的 @ 引用正文未计入，发送后按实际内容估算。</div>
      </div>
    </div>
  </el-popover>
</template>

<style lang="scss" scoped>
.context-ring {
  flex: none;
  width: 32px;
  height: 32px;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: none;
  cursor: pointer;
  transition: opacity 0.2s;

  svg {
    display: block;
  }

  &:hover {
    opacity: 0.75;
  }

  .ring-track {
    stroke: var(--el-fill-color-dark);
  }

  .ring-value {
    stroke: var(--el-color-primary);
    transition: stroke-dasharray 0.3s;
  }

  .ring-text {
    /* 圈里要放下 100% 这样的完整百分比（三位数 + 百分号） */
    font-size: 9px;
    fill: var(--el-text-color-secondary);
    /* 数字不参与鼠标事件，点哪都是按钮的点击 */
    pointer-events: none;
  }

  /* 窗口未知：整圈浅灰 + 问号，提示这里还没口径 */
  &.is-unknown {
    .ring-track {
      stroke: var(--el-border-color);
    }

    .ring-value {
      stroke: none;
    }
  }

  &.is-warn .ring-value {
    stroke: var(--el-color-warning);
  }

  &.is-danger .ring-value {
    stroke: var(--el-color-danger);
  }

  &.is-danger .ring-text,
  &.is-warn .ring-text {
    fill: var(--el-text-color-primary);
  }
}
</style>

<!-- 气泡被 teleport 到 body，脱离组件根节点，只能用非 scoped 样式命中 -->
<style lang="scss">
.context-ring-panel.el-popper {
  padding: 12px;
  border-radius: 10px;

  .panel-title {
    font-size: 13px;
    font-weight: 500;
    color: var(--el-text-color-primary);
  }

  .panel-model {
    margin-top: 2px;
    font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
    font-size: 12px;
    color: var(--el-text-color-secondary);
    word-break: break-all;
  }

  .panel-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    margin-top: 6px;
    font-size: 12px;
    color: var(--el-text-color-regular);
  }

  .panel-progress {
    margin-top: 6px;
  }

  .panel-field {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-top: 10px;

    .field-label {
      flex: none;
      font-size: 12px;
      color: var(--el-text-color-regular);
    }

    .field-input {
      flex: 1 1 auto;
      min-width: 0;
    }
  }

  .panel-presets {
    display: flex;
    align-items: center;
    gap: 2px;
    margin-top: 4px;

    .el-button {
      padding: 2px 6px;
      font-size: 12px;
    }
  }

  .panel-actions {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
    margin-top: 8px;
  }

  .panel-tip {
    margin-top: 8px;
    padding-top: 8px;
    border-top: solid 1px var(--el-border-color-lighter);
    font-size: 11px;
    line-height: 1.6;
    color: var(--el-text-color-placeholder);
  }
}
</style>
