<script setup lang="ts">
import ProxyApi from '@/api/lm/ProxyApi';
import ApiUtil from '@/utils/ApiUtil';
import { onMounted, ref, watch } from 'vue';
import * as ElementPlusIcons from '@element-plus/icons-vue';
import DataUtil from '@/utils/DataUtil';
import FetchEventSource from '@/core/FetchEventSource';
import { useUserStore } from '@/stores/user';
import type { FormInstance } from 'element-plus';
import ChatLoading from '@/components/Chat/ChatLoading.vue';

const form: any = ref({
  editing: true,
})
const formRef: any = ref<FormInstance>()
const loading = ref(false)
const agents: any = ref([])
const rules = ref({
  agentId: [{ required: true, message: '请选择智能体', trigger: 'change' }]
})

const handleAgent = (success = false) => {
  loading.value = true
  ProxyApi.agents({ success }).then(result => {
    agents.value = ApiUtil.data(result)
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}

const handleSubmit = () => {
  loading.value = !loading.value
}

onMounted(() => {
  handleAgent()
})
</script>

<template>
  <el-container>
    <el-aside class="aside">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="">
          <el-button :icon="ElementPlusIcons.ChatDotRound" class="btn-menu">开启新对话</el-button>
        </el-form-item>
        <el-form-item label="智能体" prop="agentId">
          <el-space>
            <el-select v-model="form.agentId" placeholder="请选择" filterable clearable style="width: 200px;">
              <el-option v-for="agent in agents" :key="agent.id" :value="agent.id" :label="agent.name" />
            </el-select>
            <el-button :icon="ElementPlusIcons.Refresh" title="重新载入智能体" :loading="loading" @click="handleAgent(true)" />
          </el-space>
        </el-form-item>
        <el-divider />
      </el-form>
    </el-aside>
    <el-main class="main">
      <div class="box">
        <div class="dialog">
          <div class="user">
            <div class="avatar"></div>
            <div class="content">
              <div class="message">你好</div>
              <div class="toolbar">
                <el-space>
                  <LayoutIcon name="CopyDocument" />
                  <LayoutIcon name="EditPen" />
                </el-space>
              </div>
            </div>
            <el-avatar class="avatar" :icon="ElementPlusIcons.UserFilled" />
          </div>
          <div class="assistant">
            <el-avatar class="avatar" :icon="ElementPlusIcons.MagicStick" />
            <div class="content">
              <ChatLoading />
              <el-space class="think-btn">
                <LayoutIcon name="Stopwatch" />
                <span>思考中...</span>
                <LayoutIcon name="ArrowDown" />
              </el-space>
              <div class="thinking">好的，用户，好的，用户，好的，用户，好的，用户，好的，用户，好的，用户，好的，用户，好的，用户，好的，用户，好的，用户，好的，用户，好的，用户，好的，用户，好的，用户，好的，用户，好的，用户，好的，用户，好的，用户，好的，用户，好的，用户，好的，用户，好的，用户，好的，用户，好的，用户，好的，用户，好的，用户，好的，用户，好的，用户，好的，用户，好的，用户，好的，用户，好的，用户，</div>
              <div class="message">好的</div>
              <div class="toolbar">
                <el-space>
                  <div class="pagination">
                    <LayoutIcon name="ArrowLeft" />
                    <span>1 / 3</span>
                    <LayoutIcon name="ArrowRight" />
                  </div>
                  <LayoutIcon name="CopyDocument" />
                  <LayoutIcon name="Refresh" />
                  <LayoutIcon name="chat.thumbUp" />
                  <LayoutIcon name="chat.thumbDown" />
                </el-space>
              </div>
            </div>
            <div class="avatar"></div>
          </div>
        </div>
        <div class="bottom">
          <div class="input">
            <div class="state" v-if="form.editing">
              <el-space>
                <el-alert title="正在编辑会话" type="success" style="width: 140px;" @close="form.editing = false" />
              </el-space>
            </div>
            <div class="edit">
              <textarea name="input" v-model="form.input"></textarea>
              <div v-text="form.input"></div>
            </div>
            <div class="tools">
              <el-space>
                <el-dropdown placement="top-start" trigger="click">
                  <el-button circle :icon="ElementPlusIcons.Plus" class="btn-more" />
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item :icon="ElementPlusIcons.UploadFilled">上传文件</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </el-space>
              <el-space>
                <el-button circle class="btn-submit" :disabled="!loading && !form.input" @click="handleSubmit">
                  <LayoutIcon :name="loading ? 'chat.stop' : 'Promotion'" />
                </el-button>
              </el-space>
            </div>
          </div>
        </div>
      </div>
    </el-main>
  </el-container>
</template>

<style lang="scss" scoped>
.aside {
  width: 300px;
  height: calc(100vh - var(--fs-layout-header-height));
  overflow-x: hidden;
  overflow-y: auto;
  border-right: solid 1px var(--fs-layout-border-color);
  box-sizing: border-box;
  padding: 20px;
  .btn-menu {
    border: none;
    width: 100%;
    border-radius: 15px;
  }
}
.main {
  padding: 0;
  height: calc(100vh - var(--fs-layout-header-height));
  overflow: auto;
  .box {
    margin: 0 auto;
    min-width: 500px;
    max-width: 800px;
    min-height: calc(100vh - var(--fs-layout-header-height));;
    box-sizing: border-box;
    display: flex;
    flex-direction: column;
    justify-content: space-between;
    align-items: center;
  }
  .dialog {
    flex: 1 1 auto;
    width: 100%;
    overflow-y: visible;
    padding: 40px 0;
    .user, .assistant {
      display: flex;
      align-items: start;
      justify-content: space-between;
      gap: 10px;
      .avatar {
        flex: 0 0 40px;
      }
      .content {
        flex: 1 1 auto;
      }
      .toolbar {
        margin: 10px 0;
        .pagination {
          font-size: 14px;
          @include flex-center();
        }
        .el-icon {
          cursor: pointer;
          &:hover {
            color: var(--el-color-primary);
          }
        }
      }
    }
    .user {
      .content {
        display: flex;
        align-items: self-end;
        justify-content: end;
        flex-direction: column;
      }
      .message {
        width: fit-content;
        padding: 10px;
        border-radius: 10px;
        background-color: rgb(239, 246, 255);
;
      }
    }
    .assistant {
      .think-btn {
        padding: 10px;
        font-size: 12px;
        border-radius: 10px;
        background-color: rgb(237 237 237);
        margin-bottom: 10px;
        cursor: pointer;
      }
      .thinking {
        font-size: 14px;
        color: rgb(139, 139, 139);
        line-height: 26px;
        position: relative;
        white-space: pre-wrap;
        margin: 5px 0;
        padding: 0px 0px 0px 13px;
        &::before {
          content: "";
          height: calc(100% - 14px);
          margin-top: 7px;
          position: absolute;
          top: 0px;
          left: 0px;
          border: 1px solid rgb(229, 229, 229);
        }
      }
    }
  }
  .bottom {
    width: 100%;
    height: fit-content;
    position: sticky;
    bottom: 0px;
    background-color: var(--fs-layout-background-color);
    padding-bottom: 10px;
  }
  .input {
    width: 100%;
    height: fit-content;
    border: solid 1px var(--fs-layout-border-color);
    border-radius: 15px;
    padding: 10px;
    box-sizing: border-box;
    .btn-submit {
      border: none;
      color: white;
      background-color: var(--el-color-primary);
    }
    .btn-submit.is-disabled {
      color: #4b4b5b;
      background-color: rgb(113 113 122 / 50%);
    }
    .btn-more {
      border: none;
    }
    .state {
      padding-bottom: 5px;
    }
    .edit {
      min-height: 60px;
      max-height: 320px;
      position: relative;
      textarea {
        width: 100%;
        height: 100%;
        border: none;
        resize: none;
        position: absolute;
        background-color: transparent;
        font-size: inherit;
        line-height: inherit;
        word-break: break-word;
        white-space: pre-wrap;
        font-family: inherit;
        overflow: auto;
      }
      div {
        visibility: hidden;
        pointer-events: none;
        height: 100%;
        font-size: inherit;
        line-height: inherit;
        word-break: break-word;
        white-space: pre-wrap;
        font-family: inherit;
        overflow: auto;
      }
    }
    .tools {
      height: 35px;
      margin-top: 5px;
      @include flex-between();
    }
  }
}
</style>
