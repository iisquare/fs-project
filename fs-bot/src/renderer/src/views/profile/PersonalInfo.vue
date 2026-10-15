<script setup lang="ts">
import { inject } from 'vue'
import { useUserStore } from '@renderer/stores/user'

const user = useUserStore()
const logout = inject<() => Promise<void>>('logout')
</script>

<template>
  <div class="personal-info">
    <h2>个人信息</h2>
    <div class="info-card">
      <div class="info-avatar">
        <el-avatar :size="80" :src="user.info.avatar">
          {{ user.info.name?.charAt(0) || 'U' }}
        </el-avatar>
      </div>
      <el-descriptions :column="1" border>
        <el-descriptions-item label="账号">
          {{ user.info.serial || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="昵称">
          {{ user.info.name || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="邮箱">
          {{ user.info.email || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="手机号">
          {{ user.info.phone || '-' }}
        </el-descriptions-item>
      </el-descriptions>
      <div class="info-actions">
        <el-button type="danger" @click="logout">退出登录</el-button>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.personal-info {
  padding: 40px;

  h2 {
    font-size: 20px;
    color: var(--ev-c-text-1);
    margin-bottom: 24px;
  }
}

.info-card {
  background: var(--el-bg-color);
  border-radius: 8px;
  padding: 32px;
  border: 1px solid var(--el-border-color-light);
}

.info-avatar {
  text-align: center;
  margin-bottom: 24px;
}

.info-actions {
  margin-top: 24px;
  text-align: center;
}
</style>
