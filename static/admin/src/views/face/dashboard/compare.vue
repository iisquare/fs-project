<template>
  <section>
    <a-layout>
      <a-layout-sider theme="light" width="402">
        <face-select v-model="faceLeft" :maxFaceNumber="15" />
      </a-layout-sider>
      <a-layout-content id="result">
        <a-spin size="large" v-if="this.loading" />
        <a-avatar v-else :size="100" @click="onCompare">{{ similarity ? similarity : '对比' }}</a-avatar>
      </a-layout-content>
      <a-layout-sider theme="light" width="402">
        <face-select v-model="faceRight" :maxFaceNumber="15" />
      </a-layout-sider>
    </a-layout>
  </section>
</template>

<script>
import FaceSelect from '@/views/face/dashboard/select'
import faceService from '@/service/face/face'

export default {
  components: { FaceSelect },
  data () {
    return {
      faceLeft: {},
      faceRight: {},
      loading: false,
      similarity: null
    }
  },
  watch: {
    faceLeft: function (val) {
      this.similarity = null
    },
    faceRight: function (val) {
      this.similarity = null
    }
  },
  methods: {
    onCompare () {
      if (!this.faceLeft.eigenvalue) {
        this.$message.error('请选择左侧面部区域')
        return false
      }
      if (!this.faceRight.eigenvalue) {
        this.$message.error('请选择右侧面部区域')
        return false
      }
      this.loading = true
      faceService.compare({ va: this.faceLeft.eigenvalue, vb: this.faceRight.eigenvalue }).then(result => {
        if (result.code === 0) {
          this.similarity = (result.data * 100).toFixed(2) + '%'
        } else {
          this.$message.error('对比失败' + result.message)
        }
        this.loading = false
      })
    }
  }
}
</script>

<style lang="less" scoped>
#result {
  text-align: center;
  .ant-avatar {
    cursor: pointer;
    margin-top: 170px;
  }
  .ant-spin {
    margin-top: 200px;
  }
}
</style>
