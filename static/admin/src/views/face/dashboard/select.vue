<template>
  <section id="box">
    <div id="draw" :style="'background-image: url(' + image.src + ');'">
      <a-empty v-if="status === 'unselect'" />
      <a-spin v-else-if="status === 'uploading'" tip="上传中..." />
      <div
        v-else
        v-for="(item, key) in faces"
        :key="key"
        :value="key"
        :class="item.class"
        :style="item.style"
        @click="onSelect(item.data, key)"></div>
    </div>
    <div id="tool">
      <a-upload
        accept="image/*"
        :withCredentials="true"
        :showUploadList="false"
        :before-upload="beforeUpload">
        <a-button icon="picture" type="primary">选择文件</a-button>
      </a-upload>
      <a-input-search
        v-model="url"
        placeholder="输入图片URL地址"
        style="width: 260px; float: right;"
        enter-button="检测"
        @search="onDetect" />
    </div>
  </section>
</template>

<script>
import faceService from '@/service/face/face'

export default {
  name: 'FaceSelect',
  props: {
    value: {
      type: Object,
      required: true
    },
    maxFaceNumber: {
      type: Number,
      default: 0
    }
  },
  computed: {
    faces () {
      const faces = []
      for (let i = 0; i < this.image.faces.length; i++) {
        const face = this.image.faces[i]
        const rect = {
          top: this.image.scale * face.square.top,
          left: this.image.scale * face.square.left,
          width: this.image.scale * (face.square.right - face.square.left),
          height: this.image.scale * (face.square.bottom - face.square.top)
        }
        if (this.image.width > this.image.height) {
          rect.top += (400 - this.image.scale * this.image.height) / 2
        } else {
          rect.left += (400 - this.image.scale * this.image.width) / 2
        }
        faces[i] = {
          class: this.image.selected === i ? 'face-selected' : 'face-item',
          style: {
            top: rect.top - 1 + 'px',
            left: rect.left - 1 + 'px',
            width: rect.width - 2 + 'px',
            height: rect.height - 2 + 'px'
          },
          data: face
        }
      }
      return faces
    }
  },
  data () {
    return {
      status: 'unselect',
      url: '',
      image: this.defaultImage()
    }
  },
  methods: {
    onSelect (data, key) {
      this.image.selected = key
      this.$emit('input', data)
    },
    defaultImage () {
      return {
        src: '',
        scale: 1,
        width: 400,
        height: 400,
        selected: -1,
        faces: []
      }
    },
    detect (url) {
      Object.assign(this.image, this.defaultImage())
      this.onSelect({}, -1)
      this.status = 'uploading'
      faceService.detect({ url, maxFaceNumber: this.maxFaceNumber }).then(result => {
        if (result.code === 0) {
          const image = new Image()
          image.onerror = error => {
            this.$message.error('展示图片失败：' + error)
          }
          image.onload = () => {
            this.$set(this, 'image', Object.assign({}, this.image, {
              src: url,
              scale: 400 / Math.max(image.width, image.height),
              faces: result.data,
              width: image.width,
              height: image.height
            }))
            this.$set(this, 'status', 'done')
            if (result.data.length > 0) {
              this.onSelect(result.data[0], 0)
            }
          }
          image.src = url
        } else {
          this.$message.error('识别失败：' + result.message)
          this.$set(this, 'status', 'unselect')
        }
      })
    },
    onDetect () {
      if (this.url.startsWith('http')) {
        this.detect(this.url)
      } else {
        this.$message.error('请输入正确的图片地址')
      }
    },
    beforeUpload (file) {
      const isJpgOrPng = file.type === 'image/jpeg' || file.type === 'image/png'
      if (!isJpgOrPng) {
        this.$message.error('仅支持JPG和PNG图像')
      }
      const isLt2M = file.size / 1024 / 1024 < 2
      if (!isLt2M) {
        this.$message.error('图片大小不能超过2MB!')
      }
      if (isJpgOrPng && isLt2M) {
        const reader = new FileReader()
        reader.onload = () => this.detect(reader.result)
        reader.onerror = error => this.$message.error('读取图像失败：' + error)
        reader.readAsDataURL(file)
      }
      return false
    }
  }
}
</script>

<style lang="less" scoped>
#box {
  position: relative;
  width: 402px;
  #draw {
    background-size: contain;
    background-repeat: no-repeat;
    background-position: center;
    width: 402px;
    height: 402px;
    line-height: 400px;
    border: 1px solid #d5d5d5;
    text-align: center;
    .ant-empty {
      margin-top: 150px;
    }
    img {
      border: none;
      vertical-align: middle;
    }
    .face-selected {
      position: absolute;
      border: green solid 1px;
    }
    .face-item {
      position: absolute;
      border: red dotted 1px;
      cursor: pointer;
    }
  }
  #tool {
    margin: 5px 0px 0px 0px;
    width: 100%;
  }
}
</style>
