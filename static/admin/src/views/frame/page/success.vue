<template>
  <a-card :bordered="false" style="margin: -24px -24px 0px;">
    <result type="success" :description="description" :title="title">
      <template slot="action">
        <a-button type="primary">返回列表</a-button>
        <a-button style="margin-left: 8px">查看项目</a-button>
        <a-button style="margin-left: 8px">打印</a-button>
      </template>
    </result>
  </a-card>
</template>

<script>
import { Result } from '@/components'
import { mixinDevice } from '@/utils/mixin.js'

const directionType = {
  horizontal: 'horizontal',
  vertical: 'vertical'
}

export default {
  components: { Result },
  mixins: [mixinDevice],
  data () {
    return {
      title: '提交成功',
      description: '提交结果页用于反馈一系列操作任务的处理结果，\n' +
          ' 如果仅是简单操作，使用 Message 全局提示反馈即可。\n' +
          ' 本文字区域可以展示简单的补充说明，如果有类似展示\n' +
          ' “单据”的需求，下面这个灰色区域可以呈现比较复杂的内容。',
      directionType
    }
  }
}
</script>

<style scoped>

</style>
