<script lang="tsx">
import { defineComponent, h } from 'vue'

const render = (props: any, context: any) => {
  
}

export default defineComponent({
  props: {
    columns: Array<any>,
    keyPrefix: {
      type: String,
      default: 'fs',
      required: false,
    },
  },
  setup(props, context) {
    return () => render(props, context)
  }
})
</script>

<template>
  
</template>

<style lang="scss" scoped>
</style>
