<template>
  <div class="fs-layout-csv">
    <a-form-model layout="inline">
      <a-form-model-item label="分隔符">
        <a-input v-model="glue" placeholder="避免在内容中出现分隔符" />
      </a-form-model-item>
      <a-form-model-item label="编码格式">
        <a-auto-complete v-model="charset" optionLabelProp="value" :allowClear="true" :filterOption="UIUtil.filterOption">
          <template slot="dataSource">
            <a-select-option :key="k" :value="v.value" v-for="(v, k) in charsets">{{ v.label }}</a-select-option>
          </template>
        </a-auto-complete>
      </a-form-model-item>
      <a-form-model-item>
        <a-upload
          accept=".csv,.txt"
          :action="proxyService.uploadAction()"
          :data="file => proxyService.uploadData('Govern', '/modelCompare/csv', { glue, charset })"
          :withCredentials="true"
          :showUploadList="false"
          @change="uploadChange">
          <a-button icon="cloud-upload" :loading="uploading">解析文件</a-button>
        </a-upload>
      </a-form-model-item>
    </a-form-model>
    <a-divider>说明</a-divider>
    <a-alert type="info" show-icon message="表头字段：code,name,type,size,digit,nullable,description">
      <template slot="description">
        <p>code：字段编码。</p>
        <p>name：字段名称。</p>
        <p>type：字段类型。</p>
        <p>size：字段长度。</p>
        <p>digit：小数位数。</p>
        <p>nullable：允许为空，0代表不允许，1代表允许。</p>
        <p>description：字段描述，用于详细描述当前字段作用、取值范围、关联关系等。</p>
      </template>
    </a-alert>
  </div>
</template>

<script>
import UIUtil from '@/utils/ui'
import proxyService from '@/service/admin/proxy'

export default {
  name: 'ModelCompareDefault',
  props: {
    value: { type: Array, required: true },
    form: { type: Object, required: true },
    type: { type: String, required: true },
    config: { type: Object, required: true },
    menus: { type: Array, required: true }
  },
  data () {
    return {
      UIUtil,
      proxyService,
      glue: '	',
      charset: 'GBK',
      uploading: false,
      charsets: [
        { value: 'GBK', label: 'GBK' },
        { value: 'UTF-8', label: 'UTF-8' },
        { value: 'GB2312', label: 'GB2312' }
      ]
    }
  },
  methods: {
    uploadChange ({ file, fileList, event }) {
      switch (file.status) {
        case 'uploading':
          this.uploading = true
          break
        case 'done':
          this.uploading = false
          const result = file.response
          if (result.code === 0) {
            this.$emit('input', Object.values(result.data))
            this.$emit('update:menus', ['column'])
          } else {
            this.$notification.warning({ message: '状态：' + result.code, description: '消息:' + result.message })
          }
          break
        case 'error':
          this.uploading = false
          this.$notification.error({ message: '请求异常', description: `${file.name} file upload failed.` })
          break
      }
    }
  },
  mounted () {
    this.$emit('input', [])
  }
}
</script>

<style lang="less" scoped>
.fs-layout-csv {
  width: 700px;
  padding: 15px;
  margin: 0 auto;
}
</style>
