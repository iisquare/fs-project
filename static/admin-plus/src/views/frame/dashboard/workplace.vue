<script setup lang="ts">
import DateUtil from '@/utils/DateUtil';
import { useUserStore } from '@/stores/user';
const user = useUserStore()
</script>

<template>
  <div class="card flex-between">
    <div class="flex-start left">
      <img src="@/assets/logo.svg" />
      <div class="flex-column">
        <div class="title">{{ DateUtil.hello() }}，{{ user.info.name }}</div>
        <div class="description">技术中台、数据中台、AI中台、业务中台一体化服务平台</div>
      </div>
    </div>
    <div class="flex-end right">
      <el-space :size="60">
        <el-statistic title="应用" :value="user.menu.length - 1"></el-statistic>
        <el-statistic title="权限" :value="Object.keys(user.resource).length"></el-statistic>
      </el-space>
    </div>
  </div>
  <div class="apps">
    <el-row :gutter="20">
      <template v-for="item in user.menu" :key="item.id">
        <el-col :span="8" v-if="item.url !== '/'">
          <router-link :to="item.url" :target="item.target">
            <el-card shadow="hover">
              <div class="main">
                <div class="logo">
                  <el-avatar :size="50"><LayoutIcon :name="item.icon" :size="36" /></el-avatar>
                </div>
                <div class="text flex-column">
                  <div class="title">{{ item.name }}</div>
                  <div class="description">{{ item.description }}</div>
                </div>
              </div>
              <template #footer>进入</template>
            </el-card>
          </router-link>
        </el-col>
      </template>
    </el-row>
  </div>
</template>

<style lang="scss" scoped>
.card {
  width: 100%;
  height: 120px;
  background-color: #fff;
  border-bottom: 1px solid #e8e8e8;
  .left {
    height: 100%;
    width: 50%;
    padding: 0 10px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    img {
      margin: 0 30px;
      width: 72px;
    }
    .title {
      font-size: 20px;
      line-height: 28px;
      font-weight: 500;
      color: rgba(0, 0, 0, 0.85);
    }
    .description {
      padding: 10px 0;
      color: rgb(153 153 153);
    }
  }
  .right {
    height: 100%;
    width: 50%;
    padding: 0 60px;
    :deep(.el-statistic) {
      text-align: center;
      --el-statistic-title-font-size: 14px;
      --el-statistic-content-font-size: 24px;
    }
  }
}
.apps {
  padding: 30px;
  :deep(.el-card) {
    margin-bottom: 20px;
    .el-card__footer {
      text-align: center;
      background: #fafafa;
    }
    &:hover {
      .el-card__footer {
        color: var(--el-color-primary);
      }
    }
  }
  .title {
    overflow: hidden;
    color: var(--el-color-primary);
    font-weight: 500;
    font-size: 16px;
    white-space: nowrap;
    text-overflow: ellipsis;
    margin-bottom: 10px;
  }
  .description {
    font-size: 14px;
    color: rgba(0, 0, 0, 0.45);
    height: 40px;
    line-height: 20px;
    vertical-align: middle;
    overflow: hidden;
  }
  .main {
    display: flex;
    align-items: start;
    justify-content: space-between;
  }
  .logo {
    flex: 0 0 80px;
  }
  .text {
    flex: 1 1 auto;
  }
}
</style>
