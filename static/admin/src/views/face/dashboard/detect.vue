<template>
  <section>
    <a-layout>
      <a-layout-sider theme="light" width="402">
        <face-select v-model="face" />
      </a-layout-sider>
      <a-layout-content id="result">
        <a-textarea v-model="json" />
      </a-layout-content>
    </a-layout>
  </section>
</template>

<script>
import FaceSelect from '@/views/face/dashboard/select'

export default {
  components: { FaceSelect },
  data () {
    return {
      face: {}
    }
  },
  computed: {
    json () {
      return JSON.stringify(this.face, null, 4)
    }
  },
  methods: {
  }
}
</script>

<style lang="less" scoped>
#result {
  padding-left: 15px;
  textarea {
    width: 100%;
    height: 100%;
    resize: none;
  }
}
</style>
