<template>
  <a-input v-model="value">
    <a-upload
      slot="suffix"
      v-permit="'file:archive:upload'"
      accept=".jpg,.jpeg"
      :action="proxyService.uploadAction()"
      :data="file => proxyService.uploadData('File', '/archive/upload', { bucket })"
      :withCredentials="true"
      :showUploadList="false"
      @change="uploadChange">
      <a-button icon="cloud-upload" type="primary" :loading="uploading">上传</a-button>
    </a-upload>
  </a-input>
</template>

<script>
import proxyService from '@/service/admin/proxy'

export default {
  name: 'FsInputImage',
  props: {
    value: { type: String, default: '' },
    bucket: { type: String, default: 'fs-component' }
  },
  data () {
    return {
      proxyService,
      uploading: false
    }
  },
  methods: {
    uploadChange (uploader) {
      proxyService.uploadChange(this, uploader, result => {
        this.$emit('input', result.data.id)
        this.$emit('change', result)
      })
    }
  }
}
</script>

<style lang="less" scoped>
/deep/ .ant-input-affix-wrapper {
  input {
    padding-right: 90px;
  }
}
/deep/ .ant-input-suffix {
  right: 0px;
}
</style>
