<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { useChatViewStore } from '@renderer/stores/chatView'

const props = withDefaults(
  defineProps<{
    placement?: 'bottom' | 'bottom-end' | 'bottom-start'
    width?: number
    label?: string
    triggerLabel?: string
    showReset?: boolean
    resetLabel?: string
  }>(),
  {
    placement: 'bottom-end',
    width: 260,
    label: '',
    triggerLabel: '换行设置',
    showReset: false,
    resetLabel: '恢复默认设置'
  }
)
const emit = defineEmits<{ reset: [] }>()

const chatView = useChatViewStore()
const visible = ref(false)
const menuPanelRef = ref<HTMLElement | null>(null)
const submenuSide = ref<'left' | 'right'>('left')
const tableModeLabel = computed(() => ('wrap' === chatView.tableMode ? '自动换行' : '不换行'))
const codeModeLabel = computed(() => ('wrap' === chatView.codeMode ? '自动换行' : '不换行'))
const menuLabel = computed(
  () => props.label || `正文格式设置：表格${tableModeLabel.value}，代码块${codeModeLabel.value}`
)

function close(): void {
  visible.value = false
}

function setTableMode(mode: 'wrap' | 'nowrap'): void {
  chatView.setTableMode(mode)
  close()
}

function setCodeMode(mode: 'wrap' | 'nowrap'): void {
  chatView.setCodeMode(mode)
  close()
}

function resetSettings(): void {
  chatView.setTableMode('nowrap')
  chatView.setCodeMode('nowrap')
  emit('reset')
  close()
}

function updateSubmenuSide(): void {
  const panel = menuPanelRef.value
  if (!panel) return
  const rect = panel.getBoundingClientRect()
  const required = 266
  const leftSpace = Math.max(0, rect.left)
  const rightSpace = Math.max(0, window.innerWidth - rect.right)
  submenuSide.value = rightSpace >= required || rightSpace >= leftSpace ? 'right' : 'left'
}

async function handleVisibleChange(open: boolean): Promise<void> {
  window.removeEventListener('resize', updateSubmenuSide)
  if (!open) return
  await nextTick()
  updateSubmenuSide()
  window.addEventListener('resize', updateSubmenuSide)
}

watch(visible, (open) => void handleVisibleChange(open))
onBeforeUnmount(() => window.removeEventListener('resize', updateSubmenuSide))
</script>

<template>
  <span class="chat-format-menu">
    <el-popover
      v-model:visible="visible"
      trigger="click"
      :placement="placement"
      popper-class="chat-format-menu-popover"
      :show-arrow="false"
      :width="width"
    >
      <template #reference>
        <slot
          name="trigger"
          :visible="visible"
          :label="menuLabel"
          :short-label="triggerLabel"
          :table-label="tableModeLabel"
          :code-label="codeModeLabel"
        >
          <button
            type="button"
            class="chat-format-default-trigger"
            :class="{ 'is-active': visible }"
            :title="menuLabel"
            :aria-label="menuLabel"
            aria-haspopup="menu"
            :aria-expanded="visible"
          >
            <svg viewBox="0 0 16 16" width="15" height="15" aria-hidden="true">
              <circle cx="3.5" cy="8" r="1.25" fill="currentColor" />
              <circle cx="8" cy="8" r="1.25" fill="currentColor" />
              <circle cx="12.5" cy="8" r="1.25" fill="currentColor" />
            </svg>
          </button>
        </slot>
      </template>

      <div
        ref="menuPanelRef"
        class="chat-format-menu__panel"
        role="menu"
        :aria-label="menuLabel"
        @keydown.esc.stop="close"
      >
        <slot name="settings" />

        <div class="chat-format-menu__row" role="menuitem" tabindex="0" aria-haspopup="menu">
          <svg
            class="chat-format-menu__icon"
            viewBox="0 0 16 16"
            width="15"
            height="15"
            aria-hidden="true"
          >
            <rect
              x="2.2"
              y="3.2"
              width="11.6"
              height="9.6"
              rx="1"
              fill="none"
              stroke="currentColor"
              stroke-width="1.2"
            />
            <path
              d="M2.2 6.4h11.6M2.2 9.7h11.6M6.2 3.2v9.6M10.2 3.2v9.6"
              fill="none"
              stroke="currentColor"
              stroke-width="1.1"
            />
          </svg>
          <span class="chat-format-menu__copy">
            <span>表格换行</span>
            <small>当前：{{ tableModeLabel }}</small>
          </span>
          <svg
            class="chat-format-menu__arrow"
            viewBox="0 0 12 12"
            width="11"
            height="11"
            aria-hidden="true"
          >
            <path
              d="m4.6 2.8 3.2 3.2-3.2 3.2"
              fill="none"
              stroke="currentColor"
              stroke-width="1.25"
              stroke-linecap="round"
              stroke-linejoin="round"
            />
          </svg>

          <div
            class="chat-format-menu__submenu"
            :class="{ 'is-right': 'right' === submenuSide }"
            role="menu"
            aria-label="表格换行"
          >
            <div class="chat-format-menu__submenu-title">表格显示方式</div>
            <button
              type="button"
              role="menuitemradio"
              class="chat-format-menu__option"
              :class="{ 'is-selected': 'wrap' === chatView.tableMode }"
              :aria-checked="'wrap' === chatView.tableMode"
              @click="setTableMode('wrap')"
            >
              <span class="chat-format-menu__copy">
                <span>自动换行</span>
                <small>按消息宽度折行，减少横向滚动</small>
              </span>
              <svg
                v-if="'wrap' === chatView.tableMode"
                class="chat-format-menu__check"
                viewBox="0 0 16 16"
                width="13"
                height="13"
                aria-hidden="true"
              >
                <path
                  d="m3.3 8.2 3 3 6.4-6.6"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="1.5"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                />
              </svg>
            </button>
            <button
              type="button"
              role="menuitemradio"
              class="chat-format-menu__option"
              :class="{ 'is-selected': 'nowrap' === chatView.tableMode }"
              :aria-checked="'nowrap' === chatView.tableMode"
              @click="setTableMode('nowrap')"
            >
              <span class="chat-format-menu__copy">
                <span>不换行</span>
                <small>保持表格宽度，横向滚动</small>
              </span>
              <svg
                v-if="'nowrap' === chatView.tableMode"
                class="chat-format-menu__check"
                viewBox="0 0 16 16"
                width="13"
                height="13"
                aria-hidden="true"
              >
                <path
                  d="m3.3 8.2 3 3 6.4-6.6"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="1.5"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                />
              </svg>
            </button>
          </div>
        </div>

        <div class="chat-format-menu__row" role="menuitem" tabindex="0" aria-haspopup="menu">
          <svg
            class="chat-format-menu__icon"
            viewBox="0 0 16 16"
            width="15"
            height="15"
            aria-hidden="true"
          >
            <path
              d="m5.7 4.4-3.2 3.6 3.2 3.6M10.3 4.4l3.2 3.6-3.2 3.6M9.1 2.8 6.9 13.2"
              fill="none"
              stroke="currentColor"
              stroke-width="1.2"
              stroke-linecap="round"
              stroke-linejoin="round"
            />
          </svg>
          <span class="chat-format-menu__copy">
            <span>代码块换行</span>
            <small>当前：{{ codeModeLabel }}</small>
          </span>
          <svg
            class="chat-format-menu__arrow"
            viewBox="0 0 12 12"
            width="11"
            height="11"
            aria-hidden="true"
          >
            <path
              d="m4.6 2.8 3.2 3.2-3.2 3.2"
              fill="none"
              stroke="currentColor"
              stroke-width="1.25"
              stroke-linecap="round"
              stroke-linejoin="round"
            />
          </svg>

          <div
            class="chat-format-menu__submenu"
            :class="{ 'is-right': 'right' === submenuSide }"
            role="menu"
            aria-label="代码块换行"
          >
            <div class="chat-format-menu__submenu-title">代码块显示方式</div>
            <button
              type="button"
              role="menuitemradio"
              class="chat-format-menu__option"
              :class="{ 'is-selected': 'wrap' === chatView.codeMode }"
              :aria-checked="'wrap' === chatView.codeMode"
              @click="setCodeMode('wrap')"
            >
              <span class="chat-format-menu__copy">
                <span>自动换行</span>
                <small>长代码按消息宽度折行，保留缩进</small>
              </span>
              <svg
                v-if="'wrap' === chatView.codeMode"
                class="chat-format-menu__check"
                viewBox="0 0 16 16"
                width="13"
                height="13"
                aria-hidden="true"
              >
                <path
                  d="m3.3 8.2 3 3 6.4-6.6"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="1.5"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                />
              </svg>
            </button>
            <button
              type="button"
              role="menuitemradio"
              class="chat-format-menu__option"
              :class="{ 'is-selected': 'nowrap' === chatView.codeMode }"
              :aria-checked="'nowrap' === chatView.codeMode"
              @click="setCodeMode('nowrap')"
            >
              <span class="chat-format-menu__copy">
                <span>不换行</span>
                <small>保持代码行宽，横向滚动查看</small>
              </span>
              <svg
                v-if="'nowrap' === chatView.codeMode"
                class="chat-format-menu__check"
                viewBox="0 0 16 16"
                width="13"
                height="13"
                aria-hidden="true"
              >
                <path
                  d="m3.3 8.2 3 3 6.4-6.6"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="1.5"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                />
              </svg>
            </button>
          </div>
        </div>

        <button
          v-if="showReset"
          type="button"
          class="chat-format-menu__reset"
          @click="resetSettings"
        >
          <svg viewBox="0 0 16 16" width="14" height="14" aria-hidden="true">
            <path
              d="M12.6 5.1A5.2 5.2 0 1 0 13 9.3M12.6 5.1V2.7M12.6 5.1H10.2"
              fill="none"
              stroke="currentColor"
              stroke-width="1.25"
              stroke-linecap="round"
              stroke-linejoin="round"
            />
          </svg>
          <span>{{ resetLabel }}</span>
        </button>
      </div>
    </el-popover>
  </span>
</template>

<style lang="scss" scoped>
.chat-format-menu {
  display: inline-flex;
}

.chat-format-default-trigger {
  display: inline-flex;
  width: 28px;
  height: 28px;
  align-items: center;
  justify-content: center;
  padding: 0;
  border: 1px solid transparent;
  border-radius: 6px;
  background: transparent;
  color: var(--el-text-color-placeholder);
  cursor: pointer;
  transition:
    color 0.16s,
    border-color 0.16s,
    background-color 0.16s;

  &:hover,
  &:focus-visible,
  &.is-active {
    border-color: var(--el-border-color-lighter);
    background: var(--el-fill-color-light);
    color: var(--el-color-primary);
  }

  &:focus-visible {
    outline: 1px solid var(--el-color-primary);
    outline-offset: 1px;
  }
}
</style>

<style lang="scss">
.chat-format-menu-popover.el-popper {
  padding: 6px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 12px;
  background: var(--el-bg-color-overlay);
  box-shadow:
    0 14px 38px color-mix(in srgb, var(--el-text-color-primary) 15%, transparent),
    0 3px 10px color-mix(in srgb, var(--el-text-color-primary) 7%, transparent);
  overflow: visible;
}

.chat-format-menu__panel {
  display: flex;
  flex-direction: column;
  gap: 2px;
  width: 100%;
}

.chat-format-menu__row {
  position: relative;
  display: flex;
  width: 100%;
  min-width: 0;
  min-height: 58px;
  align-items: center;
  gap: 10px;
  padding: 9px 10px;
  border-radius: 8px;
  line-height: 1.35;
  cursor: default;
  transition:
    color 0.16s,
    background-color 0.16s,
    box-shadow 0.16s;

  &:hover,
  &:focus,
  &:focus-within {
    background: var(--el-fill-color-lighter);
  }

  &:focus-visible {
    outline: none;
    box-shadow: inset 0 0 0 1px var(--el-color-primary-light-5);
  }

  & + &::before {
    position: absolute;
    top: -1px;
    right: 10px;
    left: 50px;
    height: 1px;
    background: var(--el-border-color-extra-light);
    content: '';
  }
}

.chat-format-menu__icon {
  flex: none;
  box-sizing: border-box;
  width: 30px;
  height: 30px;
  padding: 7px;
  border-radius: 8px;
  background: var(--el-fill-color-light);
  color: var(--el-text-color-secondary);
  transition:
    color 0.16s,
    background-color 0.16s;
}

.chat-format-menu__row:hover .chat-format-menu__icon,
.chat-format-menu__row:focus-within .chat-format-menu__icon {
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
}

.chat-format-menu__arrow {
  flex: none;
  margin-left: 2px;
  color: var(--el-text-color-placeholder);
  transition:
    color 0.16s,
    transform 0.16s;
}

.chat-format-menu__row:hover .chat-format-menu__arrow,
.chat-format-menu__row:focus-within .chat-format-menu__arrow {
  color: var(--el-color-primary);
  transform: translateX(2px);
}

.chat-format-menu__submenu {
  position: absolute;
  top: -6px;
  right: calc(100% + 6px);
  z-index: 1;
  box-sizing: border-box;
  width: 260px;
  padding: 6px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 12px;
  background: var(--el-bg-color-overlay);
  box-shadow:
    0 14px 38px color-mix(in srgb, var(--el-text-color-primary) 15%, transparent),
    0 3px 10px color-mix(in srgb, var(--el-text-color-primary) 7%, transparent);
  opacity: 0;
  visibility: hidden;
  pointer-events: none;
  transform: translateX(6px) scale(0.985);
  transform-origin: right top;
  transition:
    opacity 0.16s ease,
    transform 0.16s ease,
    visibility 0.16s;

  &::before {
    position: absolute;
    top: 0;
    right: -7px;
    width: 7px;
    height: 100%;
    content: '';
  }

  &.is-right {
    right: auto;
    left: calc(100% + 6px);
    transform: translateX(-6px) scale(0.985);
    transform-origin: left top;

    &::before {
      right: auto;
      left: -7px;
    }
  }
}

.chat-format-menu__row:hover .chat-format-menu__submenu,
.chat-format-menu__row:focus-within .chat-format-menu__submenu {
  opacity: 1;
  visibility: visible;
  pointer-events: auto;
  transform: translateX(0) scale(1);
}

.chat-format-menu__submenu-title {
  padding: 4px 10px 6px;
  color: var(--el-text-color-placeholder);
  font-size: 11px;
  font-weight: 600;
  line-height: 1.4;
  letter-spacing: 0.04em;
}

.chat-format-menu__option {
  display: flex;
  width: 100%;
  min-width: 0;
  min-height: 54px;
  align-items: center;
  gap: 10px;
  padding: 9px 10px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: var(--el-text-color-primary);
  line-height: 1.35;
  text-align: left;
  cursor: pointer;
  transition:
    color 0.16s,
    background-color 0.16s;

  &:hover,
  &:focus-visible {
    background: var(--el-fill-color-light);
    outline: none;
  }

  &.is-selected {
    color: var(--el-color-primary);
    background: color-mix(in srgb, var(--el-color-primary) 7%, var(--el-bg-color-overlay));
  }
}

.chat-format-menu__copy {
  display: flex;
  min-width: 0;
  flex: 1;
  flex-direction: column;
  gap: 3px;

  > span {
    font-size: 13px;
    font-weight: 550;
    line-height: 1.35;
  }

  small {
    font-size: 11.5px;
    line-height: 1.5;
    color: var(--el-text-color-secondary);
  }
}

.chat-format-menu__check {
  flex: none;
  margin-left: 8px;
  color: var(--el-color-primary);
}

.chat-format-menu__reset {
  display: flex;
  width: 100%;
  height: 36px;
  align-items: center;
  justify-content: center;
  gap: 6px;
  margin-top: 4px;
  border: 0;
  border-top: 1px solid var(--el-border-color-extra-light);
  border-radius: 0 0 7px 7px;
  background: transparent;
  color: var(--el-text-color-placeholder);
  font-size: 12px;
  cursor: pointer;
  transition:
    color 0.16s,
    background-color 0.16s;

  &:hover,
  &:focus-visible {
    background: var(--el-fill-color-light);
    color: var(--el-color-primary);
    outline: none;
  }
}
</style>
