<script setup lang="ts">
import { ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

const route = useRoute()
const router = useRouter()
const title = ref(route.meta.title)
const handleToHome = () => {
  router.push('/')
}
</script>

<template>
  <div class="workplace">
    <div class="logo">
      <div class="img"></div>
    </div>
    <div class="content">
      <h1>{{ title }}</h1>
      <div class="desc">抱歉，你访问的页面不存在或仍在开发中</div>
      <div class="actions">
        <el-button type="primary" @click="handleToHome">返回首页</el-button>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.workplace {
  display: flex;
  align-items: center;
  height: 80%;
  min-height: 500px;

  .logo {
    flex: 0 0 55%;
    width: 55%;
    zoom: 1;
    &::before,
    &::after {
      content: ' ';
      display: table;
    }
    &::after {
      clear: both;
      height: 0;
      font-size: 0;
      visibility: hidden;
    }
  }

  .img {
    float: right;
    width: 100%;
    max-width: 700px;
    height: 500px;
    background-repeat: no-repeat;
    background-position: 50% 50%;
    background-size: contain;
    background-image: url('/images/astronaut.svg');
  }

  .content {
    flex: auto;

    h1 {
      margin-bottom: 24px;
      color: #434e59;
      font-weight: 600;
      font-size: 72px;
      line-height: 72px;
    }

    .desc {
      margin-bottom: 16px;
      color: rgba(0, 0, 0, 0.45);
      font-size: 20px;
      line-height: 28px;
    }

    .actions {
      button:not(:last-child) {
        margin-right: 8px;
      }
    }
  }
}
</style>
