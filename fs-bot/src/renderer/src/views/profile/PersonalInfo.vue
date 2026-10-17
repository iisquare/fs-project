<script setup lang="ts">
import { computed, inject } from 'vue'
import { useUserStore } from '@renderer/stores/user'

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

  .action-card {
    align-items: flex-start;
    flex-direction: column;
    gap: 16px;
  }
}
</style>
