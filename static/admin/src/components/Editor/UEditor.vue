<template>
  <textarea :id="domId" />
</template>

<script>
import '../../../../resources/plugins/ueditor/ueditor.config.js'
import '../../../../resources/plugins/ueditor/ueditor.all.js'
import '../../../../resources/plugins/ueditor/lang/zh-cn/zh-cn.js'

import proxyService from '@/service/admin/proxy'

export default {
  name: 'UEditor',
  props: {
    value: { type: String, default: '' },
    height: { type: Number, default: 500 }
  },
  data () {
    return {
      domId: 'ueditor',
      editor: null
    }
  },
  methods: {
    setContent (content) {
      this.editor.setContent(content)
    },
    getContent () { // 获取内容方法
      return this.editor.getContent()
    },
    load () {
      this.editor = window.UE.getEditor(this.domId, Object.assign({ // 初始化UE
        UEDITOR_HOME_URL: this.$assets.url('/plugins/ueditor/'),
        serverUrl: proxyService.ueditorAction(),
        toolbars: [[
          'fullscreen', 'source', '|', 'undo', 'redo', '|',
          'bold', 'italic', 'underline', 'fontborder', 'strikethrough', 'superscript', 'subscript', 'removeformat', 'formatmatch', 'autotypeset', 'blockquote', 'pasteplain', '|', 'forecolor', 'backcolor', 'insertorderedlist', 'insertunorderedlist', 'selectall', 'cleardoc', '|',
          'rowspacingtop', 'rowspacingbottom', 'lineheight', '|',
          'customstyle', 'paragraph', 'fontfamily', 'fontsize', '|',
          'directionalityltr', 'directionalityrtl', 'indent', '|',
          'justifyleft', 'justifycenter', 'justifyright', 'justifyjustify', '|', 'touppercase', 'tolowercase', '|',
          'link', 'unlink', 'anchor', '|', 'imagenone', 'imageleft', 'imageright', 'imagecenter', '|',
          'simpleupload', 'insertimage', 'insertvideo', 'attachment', 'map', 'insertframe', '|',
          'horizontal', 'pagebreak', 'background', '|',
          'inserttable', 'deletetable', 'insertparagraphbeforetable', 'insertrow', 'deleterow', 'insertcol', 'deletecol', 'mergecells', 'mergeright', 'mergedown', 'splittocells', 'splittorows', 'splittocols', 'charts', '|',
          'print', 'preview', 'searchreplace', 'drafts', 'insertcode'
        ]],
        zIndex: 1,
        scaleEnabled: true,
        autoFloatEnabled: false,
        enableAutoSave: false,
        catchRemoteImageEnable: false
      }))
      this.editor.addListener('ready', () => {
        this.editor.setHeight(this.height)
        this.editor.setContent(this.value) // 确保UE加载完成后，放入内容。
      })
    }
  },
  beforeMount () {
    this.domId += '-' + new Date().getTime()
  },
  mounted () {
    this.$nextTick(() => { this.load() })
  },
  destroyed () {
    this.$emit('input', this.getContent())
    this.editor.destroy()
  }
}
</script>

<style lang="less">
.edui-default {
  box-sizing: revert;
  line-height: normal;
}
</style>
