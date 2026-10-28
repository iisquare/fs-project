<script setup lang="ts">
import { computed, inject, onBeforeUnmount, onMounted, ref } from 'vue'
import BotApi from '@renderer/api/agent/BotApi'
import { useUserStore } from '@renderer/stores/user'
import ApiUtil from '@renderer/utils/ApiUtil'

const user = useUserStore()
const logout = inject<() => Promise<void>>('logout')

const displayName = computed(() => user.info.name || user.info.serial || '用户')
const initial = computed(() => displayName.value.trim().charAt(0).toUpperCase() || 'U')

function pad(value: number): string {
  return String(value).padStart(2, '0')
}

function formatTimestamp(value: unknown): string {
  const time = Number(value ?? 0)
  if (!time) return '—'
  const date = new Date(time)
  if (Number.isNaN(date.getTime())) return '—'
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(
    date.getHours()
  )}:${pad(date.getMinutes())}`
}

interface CreditInterval {
  id: number
  name: string
  limit: number
  used: number
  remained: number
  interval: number
  beginTime: number
  resetTime: number
}

interface CreditSummary {
  available: boolean
  status: number
  remained: number
  consumed: number
  updatedTime: number
  intervals: CreditInterval[]
}

const credit = ref<CreditSummary | null>(null)
const creditLoading = ref(true)
const creditRefreshing = ref(false)
const creditError = ref('')
const now = ref(Date.now())

function numberValue(value: unknown): number {
  const number = Number(value)
  return Number.isFinite(number) ? number : 0
}

function objectValue(value: unknown): Record<string, unknown> {
  return value && typeof value === 'object' ? (value as Record<string, unknown>) : {}
}

function normalizeCredit(data: unknown): CreditSummary {
  const source = objectValue(data)
  const intervals: CreditInterval[] = Array.isArray(source.intervals)
    ? source.intervals.map((value) => {
        const item = objectValue(value)
        return {
          id: numberValue(item.id),
          name: typeof item.name === 'string' ? item.name : '',
          limit: numberValue(item.limit),
          used: numberValue(item.used),
          remained: numberValue(item.remained),
          interval: numberValue(item.interval),
          beginTime: numberValue(item.beginTime),
          resetTime: numberValue(item.resetTime)
        }
      })
    : []
  intervals.sort((a, b) => a.interval - b.interval)
  const status = numberValue(source.status)
  return {
    available: typeof source.available === 'boolean' ? source.available : status === 1,
    status,
    remained: numberValue(source.remained),
    consumed: numberValue(source.consumed),
    updatedTime: numberValue(source.updatedTime),
    intervals
  }
}

function errorMessage(error: unknown): string {
  const message = objectValue(error).message
  return typeof message === 'string' && message ? message : '积分信息加载失败'
}

async function loadCredit(): Promise<void> {
  if (creditRefreshing.value) return
  creditRefreshing.value = true
  try {
    const result = await BotApi.credit({ warning: false, error: false })
    credit.value = normalizeCredit(ApiUtil.data(result))
    creditError.value = ''
  } catch (error) {
    if (!credit.value) creditError.value = errorMessage(error)
  } finally {
    creditLoading.value = false
    creditRefreshing.value = false
  }
}

function formatCredit(value: unknown): string {
  const number = numberValue(value)
  if (number !== 0 && Math.abs(number) < 0.01) {
    return number.toLocaleString('zh-CN', { maximumSignificantDigits: 3 })
  }
  return number.toLocaleString('zh-CN', { maximumFractionDigits: 2 })
}

function formatInterval(seconds: number): string {
  if (seconds <= 0) return '—'
  if (seconds % 86400 === 0) return `${seconds / 86400} 天`
  if (seconds % 3600 === 0) return `${seconds / 3600} 小时`
  if (seconds % 60 === 0) return `${seconds / 60} 分钟`
  if (seconds < 60) return `${seconds} 秒`
  return `${Math.floor(seconds / 60)} 分 ${seconds % 60} 秒`
}

function formatCountdown(resetTime: number): string {
  const seconds = Math.max(0, Math.ceil((resetTime - now.value) / 1000))
  if (seconds <= 0) return '即将重置'
  if (seconds >= 86400) {
    const days = Math.floor(seconds / 86400)
    const hours = Math.floor((seconds % 86400) / 3600)
    return hours ? `${days} 天 ${hours} 小时后` : `${days} 天后`
  }
  if (seconds >= 3600) {
    const hours = Math.floor(seconds / 3600)
    const minutes = Math.floor((seconds % 3600) / 60)
    return minutes ? `${hours} 小时 ${minutes} 分钟后` : `${hours} 小时后`
  }
  if (seconds >= 60) return `${Math.ceil(seconds / 60)} 分钟后`
  return `${seconds} 秒后`
}

function progressPercent(item: CreditInterval): number {
  if (item.limit <= 0) return 0
  return Math.min(100, Math.max(0, (item.used / item.limit) * 100))
}

function progressTone(item: CreditInterval): string {
  const percent = progressPercent(item)
  if (percent >= 90) return 'is-danger'
  if (percent >= 70) return 'is-warning'
  return ''
}

let creditTimer: number | undefined
onMounted(() => {
  loadCredit()
  creditTimer = window.setInterval(() => {
    now.value = Date.now()
    loadCredit()
  }, 60 * 1000)
})
onBeforeUnmount(() => {
  if (creditTimer) window.clearInterval(creditTimer)
})

const joinedAt = computed(() => formatTimestamp(user.info.createdTime).slice(0, 10))
const lastLoginAt = computed(() => formatTimestamp(user.info.loginTime))

async function handleLogout(): Promise<void> {
  await logout?.()
}
</script>

<template>
  <div class="personal-info">
    <header class="page-header">
      <div>
        <h1>个人信息</h1>
        <p class="page-description">查看当前账号的身份、联系方式与登录记录</p>
      </div>
      <span class="account-state">
        <span class="account-state__dot"></span>
        已登录
      </span>
    </header>

    <section class="identity-card">
      <div class="identity-main">
        <div class="avatar-wrap">
          <el-avatar :size="84" :src="user.info.avatar" class="identity-avatar">
            {{ initial }}
          </el-avatar>
          <span class="avatar-status" title="当前账号在线"></span>
        </div>

        <div class="identity-copy">
          <div class="identity-title">
            <h2>{{ displayName }}</h2>
            <span class="identity-badge">当前账号</span>
          </div>
          <p class="identity-serial">@{{ user.info.serial || '未设置账号' }}</p>
          <p class="identity-description">
            {{ user.info.description || '暂无个人简介' }}
          </p>
        </div>
      </div>

      <div class="identity-meta">
        <div class="meta-item">
          <span class="meta-label">账号 ID</span>
          <strong class="meta-value">{{ user.info.id || '—' }}</strong>
        </div>
        <div class="meta-item">
          <span class="meta-label">加入时间</span>
          <strong class="meta-value">{{ joinedAt }}</strong>
        </div>
        <div class="meta-item">
          <span class="meta-label">最近登录</span>
          <strong class="meta-value">{{ lastLoginAt }}</strong>
        </div>
      </div>
    </section>

    <section class="credit-card">
      <header class="credit-header">
        <div class="credit-heading">
          <span class="detail-icon" aria-hidden="true">
            <svg viewBox="0 0 24 24">
              <circle cx="12" cy="12" r="8.5" />
              <path d="M9 9.2h4.2a2.1 2.1 0 0 1 0 4.2H10.8a2.1 2.1 0 0 0 0 4.2H15" />
              <path d="M12 6.8v10.4" />
            </svg>
          </span>
          <div>
            <h3>积分与用量</h3>
            <p>查看当前账号的积分余额与间隔用量</p>
          </div>
        </div>
        <el-button class="credit-refresh" link :loading="creditRefreshing" @click="loadCredit">
          刷新
        </el-button>
      </header>

      <div v-if="creditLoading" class="credit-loading">
        <el-skeleton :rows="3" animated />
      </div>

      <div v-else-if="creditError" class="credit-message">
        <strong>暂时无法获取积分信息</strong>
        <p>{{ creditError }}</p>
        <el-button size="small" @click="loadCredit">重新加载</el-button>
      </div>

      <div v-else-if="credit && !credit.status" class="credit-message">
        <strong>积分账户未开通</strong>
        <p>当前账号还没有积分记录，请联系管理员开通后再使用模型服务。</p>
      </div>

      <template v-else-if="credit">
        <div v-if="!credit.available" class="credit-alert">
          当前积分账户已停用，模型调用可能无法继续，请联系管理员处理。
        </div>

        <div class="credit-overview">
          <div class="credit-stat credit-stat--primary">
            <span class="credit-stat__label">剩余积分</span>
            <strong class="credit-stat__value">{{ formatCredit(credit.remained) }}</strong>
          </div>
          <span class="credit-separator" aria-hidden="true"></span>
          <div class="credit-stat">
            <span class="credit-stat__label">已消耗</span>
            <strong class="credit-stat__value">{{ formatCredit(credit.consumed) }}</strong>
          </div>
          <span class="credit-updated">更新于 {{ formatTimestamp(credit.updatedTime) }}</span>
        </div>

        <div v-if="credit.intervals.length" class="interval-section">
          <div class="interval-section__head">
            <span>间隔用量</span>
            <span>窗口结束后自动重置</span>
          </div>
          <div class="interval-list">
            <article v-for="item in credit.intervals" :key="item.id" class="interval-item">
              <div class="interval-item__head">
                <strong>{{ item.name || '积分周期' }}</strong>
                <span>每 {{ formatInterval(item.interval) }}</span>
              </div>
              <div class="interval-item__numbers">
                <span
                  >本期剩余 <strong>{{ formatCredit(item.remained) }}</strong></span
                >
                <span
                  >本期已用 <strong>{{ formatCredit(item.used) }}</strong></span
                >
              </div>
              <div class="interval-progress" :class="progressTone(item)">
                <span :style="{ width: `${progressPercent(item)}%` }"></span>
              </div>
              <div class="interval-item__foot">
                <span>额度 {{ formatCredit(item.limit) }}</span>
                <span>
                  下次重置 {{ formatTimestamp(item.resetTime) }}（{{
                    formatCountdown(item.resetTime)
                  }}）
                </span>
              </div>
            </article>
          </div>
        </div>
        <p v-else class="interval-empty">
          当前账号未设置间隔积分限额，模型调用仅按总剩余积分计费。
        </p>
      </template>
    </section>

    <div class="detail-grid">
      <section class="detail-card">
        <header class="detail-header">
          <span class="detail-icon" aria-hidden="true">
            <svg viewBox="0 0 24 24">
              <rect x="3.5" y="5.5" width="17" height="13" rx="3" />
              <path d="m4.5 8 6.4 4.5a2 2 0 0 0 2.2 0L19.5 8" />
            </svg>
          </span>
          <div>
            <h3>联系方式</h3>
            <p>用于账号通知与身份验证</p>
          </div>
        </header>

        <dl class="detail-list">
          <div class="detail-row">
            <dt>邮箱</dt>
            <dd :class="{ 'is-empty': !user.info.email }">{{ user.info.email || '未绑定' }}</dd>
          </div>
          <div class="detail-row">
            <dt>手机号</dt>
            <dd :class="{ 'is-empty': !user.info.phone }">{{ user.info.phone || '未绑定' }}</dd>
          </div>
        </dl>
      </section>

      <section class="detail-card">
        <header class="detail-header">
          <span class="detail-icon" aria-hidden="true">
            <svg viewBox="0 0 24 24">
              <path d="M12 3.5 19 6v5.2c0 4.2-2.8 7.7-7 9.3-4.2-1.6-7-5.1-7-9.3V6l7-2.5Z" />
              <path d="m9 12 2 2 4-4" />
            </svg>
          </span>
          <div>
            <h3>账号信息</h3>
            <p>账号标识与登录状态</p>
          </div>
        </header>

        <dl class="detail-list">
          <div class="detail-row">
            <dt>登录账号</dt>
            <dd>{{ user.info.serial || '—' }}</dd>
          </div>
          <div class="detail-row">
            <dt>账号状态</dt>
            <dd>
              <span class="status-pill">
                <span class="status-pill__dot"></span>
                正常
              </span>
            </dd>
          </div>
        </dl>
      </section>
    </div>

    <section class="action-card">
      <div>
        <h3>退出当前账号</h3>
        <p>退出后需要重新输入账号密码才能继续使用当前设备。</p>
      </div>
      <el-button class="logout-button" plain type="danger" @click="handleLogout">
        退出登录
      </el-button>
    </section>
  </div>
</template>

<style lang="scss" scoped>
.personal-info {
  width: min(100%, 820px);
  margin: 0 auto;
  padding: var(--ui-page-padding-top) var(--ui-page-padding-x) var(--ui-page-padding-bottom);
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 24px;

  h1 {
    font-size: 26px;
    line-height: 1.3;
    font-weight: 650;
    letter-spacing: -0.02em;
    color: var(--ev-c-text-1);
  }
}

.page-description {
  margin-top: 8px;
  font-size: 14px;
  color: var(--ev-c-text-3);
}

.account-state {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  flex-shrink: 0;
  margin-top: 22px;
  padding: 5px 11px;
  font-size: 12px;
  color: var(--ev-c-text-2);
  background: var(--el-fill-color-light);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 999px;
}

.account-state__dot {
  width: 6px;
  height: 6px;
  background: var(--el-color-success);
  border-radius: 50%;
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--el-color-success) 14%, transparent);
}

.identity-card {
  position: relative;
  overflow: hidden;
  padding: 30px 32px 0;
  background:
    radial-gradient(
      circle at 92% 0%,
      color-mix(in srgb, var(--el-color-primary) 13%, transparent),
      transparent 38%
    ),
    linear-gradient(
      135deg,
      color-mix(in srgb, var(--el-color-primary) 6%, var(--el-bg-color)),
      var(--el-bg-color) 55%
    );
  border: 1px solid color-mix(in srgb, var(--el-color-primary) 14%, var(--el-border-color-lighter));
  border-radius: 16px;
  box-shadow: 0 1px 2px rgb(0 0 0 / 3%);
}

.identity-main {
  display: flex;
  align-items: center;
  gap: 22px;
  min-width: 0;
}

.avatar-wrap {
  position: relative;
  flex-shrink: 0;
}

.identity-avatar {
  color: #fff;
  font-size: 32px;
  font-weight: 600;
  background: linear-gradient(
    145deg,
    color-mix(in srgb, var(--el-color-primary) 80%, #fff),
    var(--el-color-primary)
  );
  border: 4px solid color-mix(in srgb, var(--el-bg-color) 75%, transparent);
  box-shadow: 0 8px 24px color-mix(in srgb, var(--el-color-primary) 22%, transparent);
}

.avatar-status {
  position: absolute;
  right: 4px;
  bottom: 5px;
  width: 14px;
  height: 14px;
  background: var(--el-color-success);
  border: 3px solid var(--el-bg-color);
  border-radius: 50%;
}

.identity-copy {
  min-width: 0;
}

.identity-title {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;

  h2 {
    overflow: hidden;
    font-size: 23px;
    line-height: 1.35;
    font-weight: 650;
    letter-spacing: -0.01em;
    color: var(--ev-c-text-1);
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.identity-badge {
  padding: 2px 8px;
  font-size: 11px;
  color: var(--el-color-primary);
  background: color-mix(in srgb, var(--el-color-primary) 10%, transparent);
  border-radius: 5px;
}

.identity-serial {
  margin-top: 3px;
  font-size: 13px;
  color: var(--ev-c-text-3);
}

.identity-description {
  max-width: 540px;
  margin-top: 10px;
  overflow: hidden;
  font-size: 14px;
  line-height: 1.6;
  color: var(--ev-c-text-2);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.identity-meta {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  margin-top: 28px;
  border-top: 1px solid color-mix(in srgb, var(--el-border-color-lighter) 80%, transparent);
}

.meta-item {
  min-width: 0;
  padding: 18px 20px 20px 0;

  & + & {
    padding-left: 20px;
    border-left: 1px solid color-mix(in srgb, var(--el-border-color-lighter) 80%, transparent);
  }
}

.meta-label {
  display: block;
  margin-bottom: 5px;
  font-size: 11px;
  letter-spacing: 0.04em;
  color: var(--ev-c-text-3);
}

.meta-value {
  display: block;
  overflow: hidden;
  font-size: 14px;
  font-weight: 550;
  color: var(--ev-c-text-1);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.credit-card {
  margin-top: 16px;
  padding: 22px 24px;
  background: var(--ui-surface);
  border: 1px solid var(--ui-card-border);
  border-radius: 14px;
  box-shadow: 0 1px 2px rgb(0 0 0 / 3%);
}

.credit-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.credit-heading {
  display: flex;
  align-items: center;
  gap: 12px;

  h3 {
    font-size: 15px;
    font-weight: 600;
    color: var(--ev-c-text-1);
  }

  p {
    margin-top: 2px;
    font-size: 12px;
    color: var(--ev-c-text-3);
  }
}

.credit-refresh {
  flex-shrink: 0;
  font-size: 12px;
}

.credit-loading {
  margin-top: 20px;
}

.credit-message {
  padding: 26px 0 14px;

  strong {
    font-size: 14px;
    font-weight: 600;
    color: var(--ev-c-text-1);
  }

  p {
    margin: 6px 0 14px;
    font-size: 13px;
    line-height: 1.6;
    color: var(--ev-c-text-3);
  }
}

.credit-alert {
  margin-top: 18px;
  padding: 9px 12px;
  font-size: 12px;
  line-height: 1.6;
  color: var(--el-color-warning-dark-2);
  background: color-mix(in srgb, var(--el-color-warning) 10%, transparent);
  border: 1px solid color-mix(in srgb, var(--el-color-warning) 20%, transparent);
  border-radius: 8px;
}

.credit-overview {
  display: flex;
  align-items: flex-end;
  gap: 24px;
  padding-top: 22px;
}

.credit-stat {
  display: flex;
  flex-direction: column;
  gap: 5px;
  min-width: 0;
}

.credit-stat__label {
  font-size: 12px;
  color: var(--ev-c-text-3);
}

.credit-stat__value {
  font-size: 28px;
  line-height: 1.2;
  font-weight: 650;
  font-variant-numeric: tabular-nums;
  letter-spacing: -0.02em;
  color: var(--ev-c-text-1);
}

.credit-stat--primary .credit-stat__value {
  color: var(--el-color-primary);
}

.credit-separator {
  width: 1px;
  height: 38px;
  margin-bottom: 4px;
  background: var(--el-border-color-lighter);
}

.credit-updated {
  margin-left: auto;
  padding-bottom: 5px;
  font-size: 11px;
  color: var(--ev-c-text-3);
  white-space: nowrap;
}

.interval-section {
  margin-top: 20px;
  padding-top: 18px;
  border-top: 1px solid var(--el-border-color-lighter);
}

.interval-section__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 12px;
  font-size: 12px;
  color: var(--ev-c-text-3);

  span:first-child {
    font-weight: 550;
    color: var(--ev-c-text-2);
  }
}

.interval-list {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 12px;
}

.interval-item {
  padding: 14px 15px;
  background: var(--el-fill-color-lighter);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 10px;
}

.interval-item__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;

  strong {
    overflow: hidden;
    font-size: 13px;
    font-weight: 600;
    color: var(--ev-c-text-1);
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  span {
    flex-shrink: 0;
    font-size: 11px;
    color: var(--ev-c-text-3);
  }
}

.interval-item__numbers {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  margin-top: 14px;
  font-size: 12px;
  color: var(--ev-c-text-3);

  strong {
    margin-left: 4px;
    font-size: 14px;
    font-variant-numeric: tabular-nums;
    font-weight: 600;
    color: var(--ev-c-text-1);
  }
}

.interval-progress {
  height: 6px;
  margin-top: 10px;
  overflow: hidden;
  background: color-mix(in srgb, var(--el-border-color) 55%, transparent);
  border-radius: 999px;

  span {
    display: block;
    height: 100%;
    background: var(--el-color-primary);
    border-radius: inherit;
    transition: width 0.3s ease;
  }

  &.is-warning span {
    background: var(--el-color-warning);
  }

  &.is-danger span {
    background: var(--el-color-danger);
  }
}

.interval-item__foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 9px;
  font-size: 11px;
  line-height: 1.5;
  color: var(--ev-c-text-3);

  span:last-child {
    text-align: right;
  }
}

.interval-empty {
  margin-top: 18px;
  padding: 12px 14px;
  font-size: 12px;
  line-height: 1.6;
  color: var(--ev-c-text-3);
  background: var(--el-fill-color-lighter);
  border-radius: 8px;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  margin-top: 16px;
}

.detail-card,
.action-card {
  background: var(--ui-surface);
  border: 1px solid var(--ui-card-border);
  border-radius: 14px;
  box-shadow: 0 1px 2px rgb(0 0 0 / 3%);
}

.detail-card {
  padding: 22px 24px;
}

.detail-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;

  h3 {
    font-size: 15px;
    font-weight: 600;
    color: var(--ev-c-text-1);
  }

  p {
    margin-top: 2px;
    font-size: 12px;
    color: var(--ev-c-text-3);
  }
}

.detail-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 36px;
  height: 36px;
  color: var(--el-color-primary);
  background: color-mix(in srgb, var(--el-color-primary) 8%, transparent);
  border-radius: 9px;

  svg {
    width: 19px;
    height: 19px;
    fill: none;
    stroke: currentColor;
    stroke-width: 1.7;
    stroke-linecap: round;
    stroke-linejoin: round;
  }
}

.detail-list {
  margin: 0;
}

.detail-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  min-height: 42px;
  padding: 10px 0;

  & + & {
    border-top: 1px solid var(--el-border-color-lighter);
  }

  dt {
    flex-shrink: 0;
    font-size: 13px;
    color: var(--ev-c-text-3);
  }

  dd {
    min-width: 0;
    margin: 0;
    overflow: hidden;
    font-size: 13px;
    color: var(--ev-c-text-1);
    text-align: right;
    text-overflow: ellipsis;
    white-space: nowrap;

    &.is-empty {
      color: var(--ev-c-text-3);
    }
  }
}

.status-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 2px 8px;
  font-size: 12px;
  color: var(--el-color-success);
  background: color-mix(in srgb, var(--el-color-success) 9%, transparent);
  border-radius: 999px;
}

.status-pill__dot {
  width: 5px;
  height: 5px;
  background: currentColor;
  border-radius: 50%;
}

.action-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  margin-top: 16px;
  padding: 20px 24px;

  h3 {
    font-size: 14px;
    font-weight: 600;
    color: var(--ev-c-text-1);
  }

  p {
    margin-top: 4px;
    font-size: 12px;
    line-height: 1.6;
    color: var(--ev-c-text-3);
  }
}

.logout-button {
  flex-shrink: 0;
}

@media (max-width: 680px) {
  .identity-card {
    padding: 24px 22px 0;
  }

  .identity-main {
    align-items: flex-start;
  }

  .identity-meta {
    grid-template-columns: 1fr;
  }

  .meta-item {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
    padding: 11px 0;

    & + & {
      padding-left: 0;
      border-top: 1px solid var(--el-border-color-lighter);
      border-left: 0;
    }
  }

  .meta-label {
    margin-bottom: 0;
  }

  .credit-overview {
    flex-wrap: wrap;
    gap: 18px;
  }

  .credit-updated {
    width: 100%;
    margin-left: 0;
    padding-bottom: 0;
  }

  .interval-item__foot {
    align-items: flex-start;
    flex-direction: column;
    gap: 3px;

    span:last-child {
      text-align: left;
    }
  }

  .detail-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 480px) {
  .page-header {
    flex-direction: column;
    gap: 12px;
  }

  .account-state {
    margin-top: 0;
  }

  .identity-main {
    gap: 16px;
  }

  .identity-avatar {
    width: 68px !important;
    height: 68px !important;
    font-size: 26px;
    line-height: 60px;
  }

  .identity-title h2 {
    font-size: 20px;
  }

  .credit-card {
    padding: 20px 18px;
  }

  .credit-stat__value {
    font-size: 23px;
  }

  .action-card {
    align-items: flex-start;
    flex-direction: column;
    gap: 16px;
  }
}
</style>
