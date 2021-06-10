<template>
  <a-form-model :label-col="{ span: 5 }" :wrapper-col="{ span: 18 }">
    <a-form-model-item label="账号">{{ form.serial }}</a-form-model-item>
    <a-form-model-item label="名称">{{ form.name }}</a-form-model-item>
    <a-form-model-item label="描述">{{ form.description ? form.description : "暂无" }}</a-form-model-item>
    <a-form-model-item label="注册IP">{{ form.createdIp }}</a-form-model-item>
    <a-form-model-item label="注册时间">{{ form.createdTime|date }}</a-form-model-item>
    <a-form-model-item label="登录IP">{{ form.loginedIp }}</a-form-model-item>
    <a-form-model-item label="登录时间">{{ form.loginedTime|date }}</a-form-model-item>
    <a-form-model-item :wrapper-col="{ span: 12, offset: 5 }">
      <a-button @click.native="$router.go(-1)">返回</a-button>
    </a-form-model-item>
  </a-form-model>
</template>

<script>
export default {
  data () {
    return {}
  },
  computed: {
    form () {
      return this.$store.state.user.data.info
    }
  }
}
</script>
