<template>
  <div class="wrapper" ref="wrapper" :style="{ width: `${width}px`, height: `${height}px`}">
    <RulerSketch
      :lang="lang"
      :thick="thick"
      :scale="scale"
      :width="width - thick"
      :height="height - thick"
      :startX="startX"
      :startY="startY"
      :shadow="shadow"
      :horLineArr="value.lines.h"
      :verLineArr="value.lines.v"
      :cornerActive="true"
      :isShowReferLine.sync="value.isShowReferLine"
      @handleLine="handleLine"
    >
    </RulerSketch>
    <div ref="screen" class="screen" @wheel="handleWheel" @scroll="handleScroll">
      <div ref="container" class="container" @click="$emit('click')">
        <div ref="canvas" class="canvas" :style="canvasStyle"><slot></slot></div>
      </div>
    </div>
  </div>
</template>
<script>
import RulerSketch from './RulerSketch'
import { triggerWindowResizeEvent } from '@/utils/util'

export default {
  name: 'Ruler',
  components: { RulerSketch },
  props: {
    value: {
      type: Object,
      default: () => {
        return {
          zoom: 100,
          lines: {
            h: [],
            v: []
          },
          isShowReferLine: true
        }
      }
    },
    canvas: {
      type: Object,
      default: () => {
        return {
          width: 160,
          height: 200
        }
      }
    }
  },
  data () {
    return {
      width: 600,
      height: 500,
      scale: 1,
      startX: 0,
      startY: 0,
      thick: 20,
      lang: 'zh-CN', // 中英文
      isShowRuler: true // 显示标尺
    }
  },
  computed: {
    shadow () {
      return {
        x: 0,
        y: 0,
        width: this.canvas.width,
        height: this.canvas.height
      }
    },
    canvasStyle () {
      return {
        width: `${this.canvas.width}px`,
        height: `${this.canvas.height}px`,
        transform: `scale(${this.scale})`
      }
    }
  },
  watch: {
    'value.zoom': {
      handler (val) {
        this.scale = Number.parseFloat((val / 100).toFixed(2))
        this.$nextTick(() => {
          this.handleScroll()
        })
      },
      deep: true
    },
    canvas: {
      handler (val) {
        this.$nextTick(() => {
          this.handleResize()
        })
      },
      deep: true
    }
  },
  methods: {
    handleLine (lines) {
      this.lines = lines
    },
    handleScroll () {
      const screen = this.$refs.screen.getBoundingClientRect()
      const canvas = this.$refs.canvas.getBoundingClientRect()
      // 标尺开始的刻度
      const startX = (screen.left + this.thick - canvas.left) / this.scale
      const startY = (screen.top + this.thick - canvas.top) / this.scale
      this.startX = startX
      this.startY = startY
    },
    // 控制缩放值
    handleWheel (e) {
      if (e.ctrlKey || e.metaKey) {
        e.preventDefault()
        this.value.zoom = Math.max(10, Number.parseInt(this.value.zoom - e.deltaY / 50))
      }
    },
    handleResize () {
      this.width = this.$refs.wrapper.parentElement.offsetWidth
      this.height = this.$refs.wrapper.parentElement.offsetHeight
      const container = this.$refs.container.getBoundingClientRect()
      const screen = this.$refs.screen.getBoundingClientRect()
      const canvas = this.$refs.canvas.getBoundingClientRect()
      this.$refs.screen.scrollLeft = (container.width + canvas.width - screen.width) / 2
    }
  },
  mounted () {
    window.onresize = () => { this.handleResize() }
    triggerWindowResizeEvent()
  }
}
</script>

<style lang="less">
.wrapper {
  background-color: #f5f5f5;
  position: absolute;
  width: 100%;
  height: 100%;
}

.screen {
  position: absolute;
  width: 100%;
  height: 100%;
  overflow: auto;
}

.container {
  position: absolute;
  width: 5000px;
  height: 3000px;
}

.scale-value {
  position: absolute;
  left: 0;
  bottom: 100%;
}

.button {
  position: absolute;
  left: 100px;
  bottom: 100%;
}

.button-ch {
  position: absolute;
  left: 200px;
  bottom: 100%;
}
.button-en {
  position: absolute;
  left: 230px;
  bottom: 100%;
}
.canvas {
  position: absolute;
  top: 80px;
  left: 50%;
  width: 160px;
  height: 200px;
  background: lightblue;
  transform-origin: 50% 0;
  overflow: visible;
  box-shadow: 0 0 15px #e9edf3;
}
</style>
