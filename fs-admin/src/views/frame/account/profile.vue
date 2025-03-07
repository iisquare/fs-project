<script setup lang="ts">
import { useUserStore } from '@/stores/user';
import { useRouter } from 'vue-router';
import DateUtil from '@/utils/DateUtil';

const user = useUserStore()
const router = useRouter()
</script>

<template>
  <div class="flex-center">
    <el-card class="profile" shadow="never">
      <template #header>
        <div class="card-header">
          <span>个人信息</span>
        </div>
      </template>
      <el-form label-width="130px" label-suffix=":">
        <el-form-item label="账号">{{ user.info.serial }}</el-form-item>
        <el-form-item label="名称">{{ user.info.name }}</el-form-item>
        <el-form-item label="描述">{{ user.info.description || '暂无' }}</el-form-item>
        <el-form-item label="注册IP">{{ user.info.createdIp }}</el-form-item>
        <el-form-item label="注册时间">{{ DateUtil.format(user.info.createdTime) }}</el-form-item>
        <el-form-item label="登录IP">{{ user.info.loginIp }}</el-form-item>
        <el-form-item label="登录时间">{{ DateUtil.format(user.info.loginTime) }}</el-form-item>
        <el-form-item>
          <el-button @click="router.go(-1)">返回</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<style lang="scss" scoped>
.profile {
  width: 600px;
}
</style>
