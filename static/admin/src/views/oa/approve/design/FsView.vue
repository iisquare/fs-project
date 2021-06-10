<template>
  <section class="fs-form">
    <a-form-model :model="value" v-bind="formLayout">
      <fs-view-item v-model="value" :config="config" :widgets="frame.widgets" />
    </a-form-model>
  </section>
</template>

<script>
import FsViewItem from './FsViewItem'

export default {
  name: 'FsView',
  components: { FsViewItem },
  props: {
    value: { type: Object, required: true },
    config: { type: Object, required: true },
    frame: { type: Object, required: true }
  },
  data () {
    return {}
  },
  computed: {
    formLayout () { return this.config.exhibition.formLayout(this.frame) }
  },
  methods: {
  },
  mounted () {
  }
}
</script>

<style lang="less">
@import './design.less';
</style>
