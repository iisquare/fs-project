<script setup lang="ts">
/**
 * HTTP请求节点属性 - 通过 HTTP 协议发送服务器请求。
 */
import { ref } from 'vue'
import MetadataTable from '@/components/Data/MetadataTable.vue'
import NodeSlice from './NodeSlice.vue'
import OutputSlice from './OutputSlice.vue'

const active = ref('property')
const model: any = defineModel()
const tips: any = defineModel('tips', { type: null })
defineProps<{
  config?: any,
  instance?: any,
}>()
</script>

<template>
  <el-tabs v-model="active" class="tab-property">
    <el-tab-pane label="节点属性" name="property">
      <el-form :model="model" label-position="top">
        <NodeSlice v-model="model" :instance="$props.instance" :config="$props.config" :tips="tips" />
        <el-form-item label="" class="title">请求配置</el-form-item>
        <el-form-item label="请求方式">
          <el-select v-model="model.data.method" placeholder="请选择">
            <el-option :key="item.value" :value="item.value" :label="item.label" v-for="item in $props.config.httpMethods" />
          </el-select>
        </el-form-item>
        <el-form-item label="请求地址">
          <el-input v-model="model.data.url" placeholder="HTTP/HTTPS 链接地址，可插入变量" />
        </el-form-item>
        <el-form-item label="超时时间(秒)">
          <el-input-number v-model="model.data.timeout" :min="1" :max="600" :controls="false" />
        </el-form-item>
        <el-form-item label="SSL校验" class="fs-form-inline">
          <el-switch v-model="model.data.sslVerify" />
        </el-form-item>
        <el-form-item label="" class="title">授权认证</el-form-item>
        <el-form-item label="认证方式">
          <el-select v-model="model.data.authorization.type" placeholder="请选择">
            <el-option :key="item.value" :value="item.value" :label="item.label" v-for="item in $props.config.httpAuthTypes" />
          </el-select>
        </el-form-item>
        <template v-if="'apiKey' === model.data.authorization.type">
          <el-form-item label="认证标识">
            <el-input v-model="model.data.authorization.apiKey" placeholder="API Key 值" />
          </el-form-item>
          <el-form-item label="请求头字段">
            <el-input v-model="model.data.authorization.header" placeholder="如 X-API-Key" />
          </el-form-item>
        </template>
        <template v-if="'bearer' === model.data.authorization.type">
          <el-form-item label="令牌">
            <el-input v-model="model.data.authorization.apiKey" placeholder="Bearer 令牌" />
          </el-form-item>
        </template>
        <template v-if="'basic' === model.data.authorization.type">
          <el-form-item label="用户名">
            <el-input v-model="model.data.authorization.username" />
          </el-form-item>
          <el-form-item label="密码">
            <el-input v-model="model.data.authorization.password" type="password" show-password />
          </el-form-item>
        </template>
        <el-form-item label="" class="title">请求头</el-form-item>
        <el-form-item label="">
          <metadata-table v-model="model.data.headers" :editable="true" />
        </el-form-item>
        <el-form-item label="" class="title">查询参数</el-form-item>
        <el-form-item label="">
          <metadata-table v-model="model.data.params" :editable="true" />
        </el-form-item>
        <el-form-item label="" class="title">请求体</el-form-item>
        <el-form-item label="请求体类型">
          <el-select v-model="model.data.body.type" placeholder="请选择">
            <el-option :key="item.value" :value="item.value" :label="item.label" v-for="item in $props.config.httpBodyTypes" />
          </el-select>
        </el-form-item>
        <el-form-item label="JSON 内容" v-if="'json' === model.data.body.type">
          <el-input v-model="model.data.body.json" type="textarea" :rows="5" placeholder="请输入 JSON 内容，可插入变量" />
        </el-form-item>
        <el-form-item label="表单内容" v-if="'form' === model.data.body.type">
          <metadata-table v-model="model.data.body.form" :editable="true" />
        </el-form-item>
        <OutputSlice :data="model.data" />
      </el-form>
    </el-tab-pane>
  </el-tabs>
</template>

<style lang="scss" scoped>
</style>
