<template>
  <div class="footer">
    <div class="links">
      <a href="https://github.com/iisquare/fs-project" target="_blank"><a-icon type="github" /></a>
      <a href="https://github.com/sendya/ant-design-pro-vue" target="_blank">Pro</a>
      <a href="https://preview.pro.antdv.com/" target="_blank">Preview</a>
      <a href="https://1x.antdv.com/docs/vue/introduce-cn/" target="_blank">Antd</a>
    </div>
    <div class="copyright">
      <a href="https://cn.vuejs.org/v2/guide/" target="_blank"> Vue </a>
      <a-icon type="copyright" />
      <span> IISquare.com </span>
    </div>
  </div>
</template>

<script>
export default {
  name: 'GlobalFooter',
  data () {
    return {}
  }
}
</script>

<style lang="less" scoped>
.footer {
  padding: 0 16px;
  margin: 48px 0 24px;
  text-align: center;
  .links {
    margin-bottom: 8px;
    a:not(:last-child) {
      margin-right: 40px;
    }
  }
  .copyright {
    color: rgba(0, 0, 0, 0.45);
    font-size: 14px;
  }
  a {
    color: rgba(0, 0, 0, 0.45);
    &:hover {
      color: rgba(0, 0, 0, 0.65);
    }
  }
}
</style>
