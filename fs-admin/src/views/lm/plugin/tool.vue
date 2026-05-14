<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import config from '@/designer/Agentic/config'
import * as ElementPlusIcons from '@element-plus/icons-vue';
import ToolApi from '@/api/lm/ToolApi';
import ApiUtil from '@/utils/ApiUtil';
import TableUtil from '@/utils/TableUtil';

const loading = ref(false)
const rows: any = ref([])
const handleRefresh = (filter2query: boolean, keepPage: boolean) => {
  loading.value = true
  ToolApi.all({}).then((result: any) => {
    rows.value = result.data
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}
onMounted(() => {
  handleRefresh(false, true)
  ToolApi.config().then((result: any) => {
    Object.assign(config, ApiUtil.data(result))
  }).catch(() => {})
})

const filter = ref({ status: 'all', query: '', })
const tools = computed(() => {
  const result: any = []
  for (const tool of config.tools) {
    const data = rows.value[tool.type] ?? { status: 1 }
    data.status = '' + data.status
    const item = Object.assign({}, tool, { data })
    if (tool.name.toLowerCase().indexOf(filter.value.query.toLowerCase()) === -1) continue
    if (filter.value.status === 'all'
      || (filter.value.status === 'enabled' && data.status === '1')
      || (filter.value.status === 'disbaled' && data.status !== '1')) {
      result.push(item)
    }
    item.actions.forEach((action: any) => {
      action.parent = item
    })
  }
  return result
})
const active: any = ref({})
const formVisible = ref(false)
const formLoading = ref(false)
const form: any = ref({})
const handleSetting = (tool: any) => {
  form.value = Object.assign({}, tool.data, {
    name: tool.type,
    content: Object.assign(tool.options(), tool.data?.content || {}),
  })
  formVisible.value = true
}
const handleSubmit = () => {
  if (formLoading.value) return
  formLoading.value = true
  ToolApi.save(form.value, { success: true }).then(result => {
    const data = ApiUtil.data(result)
    Object.assign(active.value.data, data, { status: data.status + '' })
    handleRefresh(false, true)
    formVisible.value = false
  }).catch(() => {}).finally(() => {
    formLoading.value = false
  })
}
const handleDelete = (tool: any) => {
  TableUtil.confirm().then(() => {
    loading.value = true
    ToolApi.delete(tool.type, { success: true }).then(() => {
      handleRefresh(false, true)
    }).catch(() => {})
  }).catch(() => {})
}
</script>

<template>
  <div class="box">
    <div class="left">
       <div class="header">
          <el-space>
            <el-radio-group v-model="filter.status">
              <el-radio-button label="全部" value="all" />
              <el-radio-button label="启用" value="enabled" />
              <el-radio-button label="禁用" value="disbaled" />
            </el-radio-group>
          </el-space>
          <el-space>
            <el-input v-model="filter.query" placeholder="搜索" :prefix-icon="ElementPlusIcons.Search" clearable />
            <el-button @click="handleRefresh(false, true)" :icon="ElementPlusIcons.Refresh" :loading="loading" />
          </el-space>
        </div>
        <div class="container">
          <el-card shadow="hover" class="tool" v-for="tool in tools" :key="tool.type" @click="active = tool">
            <div class="info">
              <div class="logo" :style="`background-image: url('${tool.icon}');`"></div>
              <div class="text">
                <div class="title">{{ tool.name }}</div>
                <div class="url">{{ tool.url }}</div>
              </div>
              <div class="state">
                <el-tag v-if="loading" type="primary">载入中</el-tag>
                <template v-else>
                  <el-tag v-if="!tool.data.name" type="info">未配置</el-tag>
                  <el-tag v-else-if="tool.data.status === '1'" type="success">已启用</el-tag>
                  <el-tag v-else type="warning">已禁用</el-tag>
                </template>
              </div>
            </div>
            <div class="description">{{ tool.description }}</div>
            <el-divider />
            <el-space class="tools">
              <el-tag type="info" v-for="action in tool.actions" :key="action.type" @click.stop="active = action">{{ action.name }}</el-tag>
            </el-space>
          </el-card>
          <el-empty v-if="tools.length === 0" />
        </div>
    </div>
    <div class="right" v-if="active.setting">
      <div class="tool">
        <div class="info">
          <div class="logo" :style="`background-image: url('${active.icon}');`"></div>
          <div class="text">
            <div class="title">{{ active.name }}</div>
            <div class="url">{{ active.url }}</div>
          </div>
          <div class="menu">
            <el-space>
              <el-dropdown trigger="click">
                <el-button :icon="ElementPlusIcons.MoreFilled" link />
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item @click="handleSetting(active)">配置</el-dropdown-item>
                    <el-dropdown-item @click="handleDelete(active)">清除</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
              <el-button :icon="ElementPlusIcons.Close" link @click="active = {}" />
            </el-space>
          </div>
        </div>
        <div class="description">{{ active.description }}</div>
      </div>
      <el-divider />
      <div class="count">包含  {{ active.actions.length }} 个执行器</div>
      <div class="action" v-for="action in active.actions" :key="action.type" @click="active = action">
        <div class="title">{{ action.name }}</div>
        <div class="description">{{ action.description }}</div>
      </div>
    </div>
    <div class="right" v-if="active.output">
      <div class="flex-between">
        <el-space>
          <el-button :icon="ElementPlusIcons.Back" link @click="active = active.parent">返回</el-button>
        </el-space>
        <el-space>
          <el-button :icon="ElementPlusIcons.Close" link @click="active = {}" />
        </el-space>
      </div>
      <div class="section">
        <div class="title">{{ active.name }}</div>
        <div class="description">{{ active.description }}</div>
      </div>
      <div class="section">
        <div class="title">输入参数</div>
      </div>
      <div class="parameter" v-for="(parameter, index) in active.input" :key="index">
        <div class="field">
          <div class="name">{{ parameter.name }}</div>
          <div class="type">{{ parameter.type }}</div>
          <div class="required" v-if="parameter.required">必填</div>
        </div>
        <div class="description">{{ parameter.description }}</div>
      </div>
      <div class="section">
        <div class="title">输出参数</div>
      </div>
      <div class="parameter" v-for="(parameter, index) in active.output" :key="index">
        <div class="field">
          <div class="name">{{ parameter.name }}</div>
          <div class="type">{{ parameter.type }}</div>
          <div class="required" v-if="parameter.required">必填</div>
        </div>
        <div class="description">{{ parameter.description }}</div>
      </div>
    </div>
  </div>
  <el-dialog v-model="formVisible" title="工具配置" :close-on-click-modal="false" :destroy-on-close="true">
    <template #footer>
      <el-form :model="form">
        <div class="parameter">
          <div class="field">
            <div class="name">状态</div>
            <div class="required">必填</div>
          </div>
          <el-form-item label="">
            <el-radio-group v-model="form.status">
              <el-radio v-for="(value, key) in config.status" :key="key" :value="key">{{ value }}</el-radio>
            </el-radio-group>
          </el-form-item>
        </div>
        <div class="parameter" v-for="(parameter, index) in active.setting" :key="index">
          <div class="field">
            <div class="name">{{ parameter.name }}</div>
            <div class="type">{{ parameter.type }}</div>
            <div class="required" v-if="parameter.required">必填</div>
          </div>
          <div class="description">{{ parameter.description }}</div>
          <el-form-item label="" v-if="parameter.type === 'String'">
            <el-input v-model="form.content[parameter.name]" type="password" show-password v-if="parameter.secrecy" />
            <el-input v-model="form.content[parameter.name]" v-else />
          </el-form-item>
        </div>
      </el-form>
      <div class="dialog-footer">
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确认</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<style lang="scss" scoped>
.box {
  width: 100%;
  height: 100%;
  display: flex;
  justify-content: space-between;
}
.left {
  flex: 1;
  height: 100%;
  position: relative;;
  overflow-y: auto;
  .header {
    position: sticky;
    top: 0px;
    width: 100%;
    height: 60px;
    padding: 0 15px;
    box-sizing: border-box;
    @include flex-between();
    background-color: var(--fs-layout-background-color);
    .el-input {
      width: 200px;
    }
  }
  .container {
    width: 100%;
    padding: 0 15px;
    box-sizing: border-box;
    .el-card {
      cursor: pointer;
      margin-bottom: 15px;
      .el-divider {
        margin: 15px 0;
      }
      .tools {
        .el-tag:hover {
          color: var(--el-color-primary);
        }
      }
    }
    .description {
      white-space: nowrap;
      text-overflow: ellipsis;
      overflow: hidden;
    }
  }
}
.right {
  width: 600px;
  height: 100%;
  overflow-y: auto;
  border-left: solid 1px var(--fs-layout-border-color);
  padding: 15px;
  box-sizing: border-box;
  .el-divider {
    margin: 15px 0;
  }
  .count {
    font-size: 14px;
    margin-bottom: 15px;
  }
  .section {
    .title {
      font-weight: 600;
      padding: 20px 0;
    }
    .description {
      font-size: 14px;
      color: #7A7F90;
    }
  }
}
.parameter {
  .field {
    @include flex-start();
    gap: 10px;
    margin-bottom: 10px;
    .name {
      color: #354052;
      font-size: 13px;
      font-weight: 600;
      line-height: 1.5;
    }
    .type {
      color: #676f83;
      font-size: 12px;
      font-weight: 400;
      line-height: 16px;
      padding: 2px 5px;
      background-color: #F2F3F8;
    }
    .required {
      color: #f79009;
      font-size: 12px;
      font-weight: 500;
      line-height: 16px;
    }
  }
  .description {
    font-size: 14px;
    color: #7A7F90;
    margin-bottom: 20px;
    text-align: left;
  }
}
.tool {
  .info {
    width: 100%;
    gap: 15px;
    @include flex-between();
    .logo {
      flex: 0 0 60px;
      height: 60px;
      background: center no-repeat;
      background-size: contain;
    }
    .text {
      flex: 1 1 auto;
      justify-content: start;
      .title {
        font-weight: 600;
      }
      .url {
        font-size: 14px;
        color: rgb(107 114 128);
      }
    }
    .state {
      flex: 0 0 60px;
    }
    .menu {
      flex: 0 0 60px;
      text-align: right;
    }
  }
  .description {
    font-size: 14px;
    color: #7A7F90;
  }
}
.action {
  cursor: pointer;
  border: solid 1px rgb(16 24 40/0.08);
  border-radius: 5px;
  padding: 15px;
  box-shadow: 0px 1px 2px 0px rgba(16,24,40,0.05);
  margin-bottom: 15px;
  .title {
    font-size: 14px;
    font-weight: 600;
    line-height: 30px;
  }
  .description {
    font-size: 12px;
    font-weight: 400;
    line-height: 16px;
    color: #676f83;
  }
}
.action:hover {
  background-color: #f9fafb;
}
</style>
