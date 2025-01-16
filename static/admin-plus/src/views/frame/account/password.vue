<script setup lang="ts">
import { useUserStore } from '@/stores/user';
import { useRouter } from 'vue-router';
import DateUtil from '@/utils/DateUtil';
import { ref } from 'vue';
import type { FormInstance } from 'element-plus';
import UserApi from '@/api/member/UserApi';
import ApiUtil from '@/utils/ApiUtil';

const user = useUserStore()
const router = useRouter()

const form: any =  ref({})
const formRef = ref<FormInstance>()
const rules = ref({
  passwordOld: [{ required: true, message: '请输入当前登录密码' }],
  password: [{ required: true, message: '请输入新的登录密码' }],
  passwordNew: [{ required: true, message: '请确认新的登录密码' }]
})
const loading = ref(false)
const handleSubmit = () => {
  formRef.value && formRef.value.validate(valid => {
    if (!valid || loading.value) return
    loading.value = true
    UserApi.password(form.value).then(result => {
      if (ApiUtil.succeed(result)) {
        router.go(0)
      }
      loading.value = false
    })
  })
}
</script>

<template>
  <div class="flex-center">
    <el-card class="password" shadow="never">
      <template #header>
        <div class="card-header">
          <span>修改密码</span>
        </div>
      </template>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="130px" label-suffix=":">
        <el-form-item label="账号">{{ user.info.serial }}</el-form-item>
        <el-form-item label="名称">{{ user.info.name }}</el-form-item>
        <el-form-item label="原密码" prop="passwordOld">
          <el-input v-model="form.passwordOld" placeholder="请输入当前登录密码" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密码" prop="password">
          <el-input v-model="form.password" placeholder="请输入新的登录密码" type="password" show-password />
        </el-form-item>
        <el-form-item label="确认密码" prop="passwordNew">
          <el-input v-model="form.passwordNew" placeholder="请确认新的登录密码" type="password" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleSubmit">提交</el-button>
          <el-button @click="router.go(-1)">返回</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<style lang="scss" scoped>
.password {
  width: 600px;
}
</style>
