<template>
  <section>
    <a-form-model ref="schedule" :model="schedule" @submit="submit" :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
      <a-card title="基础信息">
        <a-row class="form-row">
          <a-col :md="8" :sm="24">
            <a-form-model-item label="名称">
              <a-input type="text" v-model="schedule.name" auto-complete="off" placeholder="计划名称"></a-input>
            </a-form-model-item>
          </a-col>
          <a-col :md="8" :sm="24">
            <a-form-model-item label="分组">
              <a-input type="text" v-model="schedule.type" auto-complete="on"></a-input>
            </a-form-model-item>
          </a-col>
        </a-row>
        <a-row class="form-row" :gutter="16">
          <a-col :md="8" :sm="24">
            <a-form-model-item label="排序">
              <a-input-number v-model="schedule.sort" :min="0" :max="200"></a-input-number>
            </a-form-model-item>
          </a-col>
          <a-col :md="8" :sm="24">
            <a-form-model-item label="线程资源">
              <a-input-number v-model="schedule.maxThread" :min="1"></a-input-number>
            </a-form-model-item>
          </a-col>
          <a-col :md="8" :sm="24">
            <a-form-model-item label="每节点数">
              <a-input-number v-model="schedule.maxPerNode" :min="0"></a-input-number>
            </a-form-model-item>
          </a-col>
          <a-col :md="8" :sm="24">
            <a-form-model-item label="等级">
              <a-input-number v-model="schedule.priority" :min="0" placeholder="作业优先级"></a-input-number>
            </a-form-model-item>
          </a-col>
          <a-col :md="8" :sm="24">
            <a-form-model-item label="最小停顿">
              <a-input-number v-model="schedule.minHalt" :min="0"></a-input-number>
            </a-form-model-item>
          </a-col>
          <a-col :md="8" :sm="24">
            <a-form-model-item label="最大停顿">
              <a-input-number v-model="schedule.maxHalt" :min="0"></a-input-number>
            </a-form-model-item>
          </a-col>
        </a-row>
        <a-row class="form-row" :gutter="16">
          <a-col :md="24" :sm="24">
            <a-form-model-item label="选项" :label-col="{ span: 2 }" :wrapper-col="{ span: 22 }">
              <a-checkbox v-model="schedule.dealRequestHeader">处理请求头</a-checkbox>
              <a-checkbox v-model="schedule.dealResponseHeader">处理响应头</a-checkbox>
            </a-form-model-item>
          </a-col>
        </a-row>
        <a-row class="form-row" :gutter="16">
          <a-col :md="24" :sm="24">
            <a-form-model-item label="描述" :label-col="{ span: 2 }" :wrapper-col="{ span: 22 }">
              <a-textarea v-model="schedule.description" />
            </a-form-model-item>
          </a-col>
        </a-row>
      </a-card>
      <a-card title="初始信息" style="margin-top: 24px">
        <a-row class="form-row" :gutter="16">
          <a-col :md="8" :sm="24">
            <a-form-model-item label="初始页面">
              <a-input type="text" v-model="schedule.initTask" auto-complete="on"></a-input>
            </a-form-model-item>
          </a-col>
        </a-row>
        <a-row class="form-row" :gutter="16">
          <a-col :md="24" :sm="24">
            <a-form-model-item label="初始参数" :label-col="{ span: 2 }" :wrapper-col="{ span: 22 }">
              <a-textarea v-model="schedule.initParams" placeholder="JSON数组字符串" />
            </a-form-model-item>
          </a-col>
        </a-row>
      </a-card>
      <a-card title="页面模板" style="margin-top: 24px">
        <a-table
          :columns="templateColumns"
          :dataSource="schedule.templates"
          :rowKey="(record, index) => index"
          :pagination="false"
          :bordered="true">
          <span slot="action" slot-scope="text, record, index">
            <a-button type="link" size="small" @click="templateAppend(text, record, index)">添加</a-button>
            <a-button type="link" size="small" @click="templateRemove(text, record, index)">删除</a-button>
          </span>
          <template slot="expandedRowRender" slot-scope="record">
            <a-form-model :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
              <a-row class="form-row">
                <a-col :md="8" :sm="24">
                  <a-form-model-item label="页面">
                    <a-input type="text" v-model="record.page" auto-complete="off"></a-input>
                  </a-form-model-item>
                </a-col>
                <a-col :md="8" :sm="24">
                  <a-form-model-item label="名称">
                    <a-input type="text" v-model="record.name" auto-complete="on"></a-input>
                  </a-form-model-item>
                </a-col>
                <a-col :md="8" :sm="24">
                  <a-form-model-item label="优先级">
                    <a-tooltip title="数小优先，设置间隔后无效"><a-input-number v-model="record.priority" :min="0"></a-input-number></a-tooltip>
                  </a-form-model-item>
                </a-col>
              </a-row>
              <a-row class="form-row" :gutter="16">
                <a-col :md="24" :sm="24">
                  <a-form-model-item label="描述" :label-col="{ span: 2 }" :wrapper-col="{ span: 22 }">
                    <a-textarea v-model="record.description" />
                  </a-form-model-item>
                </a-col>
              </a-row>
              <a-row class="form-row" :gutter="16">
                <a-col :md="24" :sm="24">
                  <a-form-model-item label="链接地址" :label-col="{ span: 2 }" :wrapper-col="{ span: 22 }">
                    <a-input v-model="record.url" placeholder="链接地址{参数名称}"></a-input>
                  </a-form-model-item>
                </a-col>
              </a-row>
              <a-row class="form-row" :gutter="16">
                <a-col :md="8" :sm="24">
                  <a-form-model-item label="请求类型">
                    <a-input v-model="record.requestType"></a-input>
                  </a-form-model-item>
                </a-col>
                <a-col :md="8" :sm="24">
                  <a-form-model-item label="最小间隔">
                    <a-input v-model="record.minInterval" placeholder="毫秒"></a-input>
                  </a-form-model-item>
                </a-col>
                <a-col :md="8" :sm="24">
                  <a-form-model-item label="迭代次数">
                    <a-input v-model="record.maxIterate"></a-input>
                  </a-form-model-item>
                </a-col>
                <a-col :md="8" :sm="24">
                  <a-form-model-item label="页面编码">
                    <a-input v-model="record.charset"></a-input>
                  </a-form-model-item>
                </a-col>
                <a-col :md="8" :sm="24">
                  <a-form-model-item label="最大间隔">
                    <a-input v-model="record.maxInterval" placeholder="毫秒"></a-input>
                  </a-form-model-item>
                </a-col>
                <a-col :md="8" :sm="24">
                  <a-form-model-item label="重试次数">
                    <a-input v-model="record.maxRetry"></a-input>
                  </a-form-model-item>
                </a-col>
              </a-row>
              <a-row class="form-row" :gutter="16">
                <a-col :md="24" :sm="24">
                  <a-form-model-item label="请求头部" :label-col="{ span: 2 }" :wrapper-col="{ span: 22 }">
                    <a-textarea v-model="record.headers" placeholder="JSON键值对字符串" />
                  </a-form-model-item>
                </a-col>
              </a-row>
              <a-row class="form-row" :gutter="16">
                <a-col :md="24" :sm="24">
                  <a-form-model-item label="解析器" :label-col="{ span: 2 }" :wrapper-col="{ span: 22 }">
                    <a-textarea v-model="record.parser" />
                  </a-form-model-item>
                </a-col>
              </a-row>
              <a-row class="form-row" :gutter="16">
                <a-col :md="24" :sm="24">
                  <a-form-model-item label="映射器" :label-col="{ span: 2 }" :wrapper-col="{ span: 22 }">
                    <a-textarea v-model="record.mapper" placeholder="JSON键值对字符串" />
                  </a-form-model-item>
                </a-col>
              </a-row>
              <a-row class="form-row">
                <a-col :md="8" :sm="24">
                  <a-form-model-item label="输出类型">
                    <a-input type="text" v-model="record.outputType" auto-complete="on"></a-input>
                  </a-form-model-item>
                </a-col>
              </a-row>
              <a-row class="form-row" :gutter="16">
                <a-col :md="24" :sm="24">
                  <a-form-model-item label="输出配置" :label-col="{ span: 2 }" :wrapper-col="{ span: 22 }">
                    <a-textarea v-model="record.outputProperty" placeholder="JSON键值对字符串" />
                  </a-form-model-item>
                </a-col>
              </a-row>
            </a-form-model>
          </template>
        </a-table>
      </a-card>
      <a-card title="拦截器" style="margin-top: 24px">
        <a-table
          :columns="interceptColumns"
          :dataSource="schedule.intercepts"
          :rowKey="(record, index) => index"
          :pagination="false"
          :bordered="true">
          <span slot="action" slot-scope="text, record, index">
            <a-button type="link" size="small" @click="interceptAppend(text, record, index)">添加</a-button>
            <a-button type="link" size="small" @click="interceptRemove(text, record, index)">删除</a-button>
          </span>
          <template slot="expandedRowRender" slot-scope="record">
            <a-row class="form-row">
              <a-col :md="8" :sm="24">
                <a-form-model-item label="状态码">
                  <a-input type="text" v-model="record.code" auto-complete="off"></a-input>
                </a-form-model-item>
              </a-col>
              <a-col :md="8" :sm="24">
                <a-form-model-item label="名称">
                  <a-input type="text" v-model="record.name" auto-complete="on"></a-input>
                </a-form-model-item>
              </a-col>
            </a-row>
            <a-row class="form-row" :gutter="16">
              <a-col :md="24" :sm="24">
                <a-form-model-item label="描述" :label-col="{ span: 2 }" :wrapper-col="{ span: 22 }">
                  <a-textarea v-model="record.description" />
                </a-form-model-item>
              </a-col>
            </a-row>
            <a-row class="form-row" :gutter="16">
              <a-col :md="24" :sm="24">
                <a-form-model-item label="解析器" :label-col="{ span: 2 }" :wrapper-col="{ span: 22 }">
                  <a-textarea v-model="record.parser" />
                </a-form-model-item>
              </a-col>
            </a-row>
            <a-row class="form-row" :gutter="16">
              <a-col :md="24" :sm="24">
                <a-form-model-item label="辅助器" :label-col="{ span: 2 }" :wrapper-col="{ span: 22 }">
                  <a-textarea v-model="record.assistor" />
                </a-form-model-item>
              </a-col>
            </a-row>
            <a-row class="form-row" :gutter="16">
              <a-col :md="24" :sm="24">
                <a-form-model-item label="映射器" :label-col="{ span: 2 }" :wrapper-col="{ span: 22 }">
                  <a-textarea v-model="record.mapper" placeholder="JSON键值对字符串" />
                </a-form-model-item>
              </a-col>
            </a-row>
            <a-row class="form-row">
              <a-col :md="8" :sm="24">
                <a-form-model-item label="输出类型">
                  <a-input type="text" v-model="record.outputType" auto-complete="on"></a-input>
                </a-form-model-item>
              </a-col>
            </a-row>
            <a-row class="form-row" :gutter="16">
              <a-col :md="24" :sm="24">
                <a-form-model-item label="输出配置" :label-col="{ span: 2 }" :wrapper-col="{ span: 22 }">
                  <a-textarea v-model="record.outputProperty" placeholder="JSON键值对字符串" />
                </a-form-model-item>
              </a-col>
            </a-row>
          </template>
        </a-table>
      </a-card>
      <a-card style="margin-top: 24px">
        <a-button type="primary" html-type="submit" :loading="loading">提交</a-button>
        <a-divider type="vertical" />
        <a-button @click.native="$router.go(-1)">返回</a-button>
      </a-card>
    </a-form-model>
  </section>
</template>

<script>
import templateService from '@/service/spider/template'

export default {
  data () {
    return {
      loading: false,
      schedule: {
        name: '',
        type: '',
        sort: 0,
        priority: 0,
        maxThread: 1,
        maxPerNode: 0,
        minHalt: 0,
        maxHalt: 0,
        dealRequestHeader: false,
        dealResponseHeader: false,
        description: '',
        templates: [],
        intercepts: [],
        initTask: '',
        initParams: ''
      },
      templateColumns: [
        { title: '页面', dataIndex: 'page' },
        { title: '名称', dataIndex: 'name' },
        { title: '描述', dataIndex: 'description' },
        { title: '操作', scopedSlots: { customRender: 'action' } }
      ],
      interceptColumns: [
        { title: '状态码', dataIndex: 'code' },
        { title: '名称', dataIndex: 'name' },
        { title: '描述', dataIndex: 'description' },
        { title: '操作', scopedSlots: { customRender: 'action' } }
      ]
    }
  },
  watch: {
    'schedule.templates' (templates) {
      if (templates.length < 1) {
        this.schedule.templates.push(this.templateItem())
      }
    },
    'schedule.intercepts' (intercepts) {
      if (intercepts.length < 1) {
        this.schedule.intercepts.push(this.interceptItem())
      }
    }
  },
  methods: {
    interceptItem () {
      return {
        code: '',
        name: '',
        description: '',
        parser: '',
        assistor: '',
        mapper: '',
        outputType: '',
        outputProperty: ''
      }
    },
    interceptAppend (text, record, index) {
      this.schedule.intercepts.splice(index + 1, 0, this.interceptItem())
    },
    interceptRemove (text, record, index) {
      this.schedule.intercepts.splice(index, 1)
    },
    templateItem () {
      return {
        page: '',
        name: '',
        description: '',
        url: '',
        requestType: '',
        charset: '',
        priority: 0,
        minInterval: '',
        maxInterval: '',
        maxRetry: '',
        maxIterate: '',
        headers: '',
        parser: '',
        mapper: '',
        outputType: '',
        outputProperty: ''
      }
    },
    templateAppend (text, record, index) {
      this.schedule.templates.splice(index + 1, 0, this.templateItem())
    },
    templateRemove (text, record, index) {
      this.schedule.templates.splice(index, 1)
    },
    load () {
      templateService.info({ id: this.$route.query.id }).then((result) => {
        if (result.code === 0) {
          const info = result.data
          const data = Object.assign({}, this.schedule, info.content ? JSON.parse(info.content) : {})
          this.schedule = {
            name: info.name,
            type: info.type,
            sort: info.sort,
            description: info.description,
            priority: data.priority,
            maxThread: data.maxThread,
            maxPerNode: data.maxPerNode,
            minHalt: data.minHalt,
            maxHalt: data.maxHalt,
            dealRequestHeader: data.dealRequestHeader,
            dealResponseHeader: data.dealResponseHeader,
            templates: data.templates,
            intercepts: data.intercepts,
            initTask: data.initTask,
            initParams: data.initParams
          }
        } else if (result.code === 404) {
          this.$router.push('/exception/404')
        } else {
          this.$router.push('/exception/500')
        }
      })
    },
    submit (e) {
      e.preventDefault()
      this.loading = true
      const data = Object.assign({
        id: this.$route.query.id
      }, {
        name: this.schedule.name,
        type: this.schedule.type,
        sort: this.schedule.sort,
        description: this.schedule.description,
        content: JSON.stringify({
          priority: this.schedule.priority,
          maxThread: this.schedule.maxThread,
          maxPerNode: this.schedule.maxPerNode,
          minHalt: this.schedule.minHalt,
          maxHalt: this.schedule.maxHalt,
          dealRequestHeader: this.schedule.dealRequestHeader,
          dealResponseHeader: this.schedule.dealResponseHeader,
          templates: this.schedule.templates,
          intercepts: this.schedule.intercepts,
          initTask: this.schedule.initTask,
          initParams: this.schedule.initParams
        })
      })
      templateService.save(data, { success: true }).then((result) => { this.loading = false })
    }
  },
  mounted () {
    this.load()
  }
}
</script>
