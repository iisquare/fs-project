<template>
  <a-card :body-style="{padding: '24px 32px'}" :bordered="false">
    <a-form-model
      ref="form"
      :model="form"
      :rules="rules"
      @submit="submit"
      v-bind="UIUtil.formLayoutFlex()">
      <a-collapse v-model="activeKey">
        <a-collapse-panel key="basic" header="基础信息">
          <a-row :gutter="48">
            <a-col :md="8" :sm="24">
              <a-form-model-item label="标识" prop="id"><a-input v-model="form.id" /></a-form-model-item>
            </a-col>
            <a-col :md="8" :sm="24">
              <a-form-model-item label="所属栏目" prop="catalogId">
                <a-tree-select
                  v-model="form.catalogId"
                  placeholder="请选择所属栏目"
                  allow-clear
                  tree-default-expand-all
                  :treeData="config.catalog"
                />
              </a-form-model-item>
            </a-col>
          </a-row>
          <a-row :gutter="16">
            <a-col :md="24" :sm="24">
              <a-form-model-item label="标题" prop="title"><a-input v-model="form.title" /></a-form-model-item>
            </a-col>
          </a-row>
          <a-row :gutter="16">
            <a-col :md="16" :sm="24">
              <a-form-model-item label="关键词" prop="keyword"><a-input v-model="form.keyword" /></a-form-model-item>
            </a-col>
            <a-col :md="8" :sm="24">
              <a-form-model-item label="阅读密码" prop="password"><a-input v-model="form.password" /></a-form-model-item>
            </a-col>
          </a-row>
          <a-row :gutter="16">
            <a-col :md="24" :sm="24">
              <a-form-model-item label="描述" prop="description"><a-textarea v-model="form.description" /></a-form-model-item>
            </a-col>
          </a-row>
        </a-collapse-panel>
        <a-collapse-panel key="cite" header="标记信息">
          <a-row :gutter="48">
            <a-col :md="8" :sm="24">
              <a-form-model-item label="系统标签" prop="label">
                <a-select v-model="form.label" mode="multiple" placeholder="请选择" :allowClear="true">
                  <a-select-option v-for="(value, key) in config.label" :key="key" :value="key">{{ value }}</a-select-option>
                </a-select>
              </a-form-model-item>
            </a-col>
            <a-col :md="16" :sm="24">
              <a-form-model-item label="内容标签" prop="tag">
                <a-select v-model="form.tag" mode="multiple" placeholder="请输入" :allowClear="true"></a-select>
              </a-form-model-item>
            </a-col>
          </a-row>
          <a-row :gutter="48">
            <a-col :md="8" :sm="24">
              <a-form-model-item label="引用标识" prop="citeName"><a-input v-model="form.citeName" /></a-form-model-item>
            </a-col>
            <a-col :md="8" :sm="24">
              <a-form-model-item label="引用作者" prop="citeAuthor"><a-input v-model="form.citeAuthor" /></a-form-model-item>
            </a-col>
          </a-row>
          <a-row :gutter="16">
            <a-col :md="24" :sm="24">
              <a-form-model-item label="引用地址" prop="citeUrl"><a-input v-model="form.citeUrl" /></a-form-model-item>
            </a-col>
          </a-row>
        </a-collapse-panel>
        <a-collapse-panel key="state" header="状态信息">
          <a-row :gutter="48">
            <a-col :md="6" :sm="24">
              <a-form-model-item label="创建者">{{ form.createdUidName }}</a-form-model-item>
            </a-col>
            <a-col :md="6" :sm="24">
              <a-form-model-item label="创建时间">{{ form.createdTime|date }}</a-form-model-item>
            </a-col>
            <a-col :md="6" :sm="24">
              <a-form-model-item label="修改者">{{ form.updatedUidName }}</a-form-model-item>
            </a-col>
            <a-col :md="6" :sm="24">
              <a-form-model-item label="修改时间">{{ form.updatedTime|date }}</a-form-model-item>
            </a-col>
          </a-row>
          <a-row :gutter="16">
            <a-col :md="8" :sm="24">
              <a-form-model-item label="封面图片" prop="cover"><fs-input-image v-model="form.cover" bucket="cms" /></a-form-model-item>
            </a-col>
            <a-col :md="5" :sm="24">
              <a-form-model-item label="发布时间" prop="publishTime">
                <s-date-picker
                  v-model="form.publishTime"
                  :showTime="DateUtil.showTime(0)"
                  :format="DateUtil.dateFormat()"
                  placeholder="页面展示的发布时间"
                />
              </a-form-model-item>
            </a-col>
            <a-col :md="5" :sm="24">
              <a-form-model-item label="状态" prop="status">
                <a-select v-model="form.status" placeholder="请选择" :allowClear="true">
                  <a-select-option v-for="(value, key) in config.status" :key="key" :value="key">{{ value }}</a-select-option>
                </a-select>
              </a-form-model-item>
            </a-col>
            <a-col :md="5" :sm="24">
              <a-form-model-item label="排序" prop="sort"><a-input-number v-model="form.sort" :min="0" :max="200"></a-input-number></a-form-model-item>
            </a-col>
          </a-row>
        </a-collapse-panel>
        <a-collapse-panel key="detail" header="详细信息">
          <a-row :gutter="48">
            <a-col :md="6" :sm="24">
              <a-form-model-item label="格式" prop="format">
                <a-select v-model="form.format" placeholder="内容格式" :allowClear="true">
                  <a-select-option v-for="(value, key) in config.format" :key="key" :value="key">{{ value }}</a-select-option>
                </a-select>
              </a-form-model-item>
            </a-col>
          </a-row>
          <a-row :gutter="16">
            <a-col :md="24" :sm="24">
              <a-form-model-item label="内容" prop="content">
                <template v-if="form.format === 'html'">
                  <u-editor v-model="form.content" />
                </template>
                <template v-else-if="form.format === 'markdown'">
                  <tui-editor v-model="form.content" />
                </template>
                <template v-else-if="form.format === 'text'">
                  <code-editor v-model="form.content" />
                </template>
                <template v-else>
                  <a-textarea v-model="form.content" rows="16" />
                </template>
              </a-form-model-item>
            </a-col>
          </a-row>
        </a-collapse-panel>
      </a-collapse>
      <a-form-model-item :wrapperCol="{ span: 24 }" style="text-align: center;">
        <a-button type="primary" html-type="submit" :loading="loading">提交</a-button>
        <a-button @click.native="$router.go(-1)" :style="{ marginLeft: '8px' }">返回</a-button>
      </a-form-model-item>
    </a-form-model>
  </a-card>
</template>

<script>
import UIUtil from '@/utils/ui'
import DateUtil from '@/utils/date'
import articleService from '@/service/cms/article'

export default {
  components: {
    FsInputImage: () => import('@/components/Upload/InputImage'),
    UEditor: () => import('@/components/Editor/UEditor'),
    TuiEditor: () => import('@/components/Editor/TuiEditor'),
    CodeEditor: () => import('@/components/Editor/CodeEditor')
  },
  data () {
    return {
      UIUtil,
      DateUtil,
      activeKey: ['basic', 'cite', 'state', 'detail'],
      loading: false,
      form: { id: '', label: [], tag: [] },
      rules: {
        title: [{ required: true, message: '文章标题不能为空', trigger: 'blur' }],
        catalogId: [{ required: true, message: '请选择所属栏目', trigger: 'change' }],
        status: [{ required: true, message: '请选择状态', trigger: 'change' }]
      },
      config: { ready: false, status: {}, label: {}, format: {}, catalog: [] }
    }
  },
  methods: {
    formatted (form) {
      return Object.assign({}, form, {
        id: form.id + '',
        label: form.label ? form.label.split(',') : [],
        tag: form.tag ? form.tag.split(',') : [],
        publishTime: DateUtil.dateRender(form.publishTime),
        status: form.status + ''
      })
    },
    load () {
      this.form.id = this.$route.query.id
      if (!this.form.id) return
      this.loading = true
      articleService.info({ id: this.form.id }).then(result => {
        if (result.code !== 0) return
        this.form = this.formatted(result.data)
        this.loading = false
      })
    },
    submit (e) {
      e.preventDefault()
      this.$refs.form.validate(valid => {
        if (!valid || this.loading) return false
        this.loading = true
        const data = Object.assign({}, this.form, {
          label: this.form.label.join(','),
          tag: this.form.tag.join(',')
        })
        articleService.save(data, { success: true }).then(result => {
          if (result.code === 0) {
            this.form = this.formatted(result.data)
          }
          this.loading = false
        })
      })
    }
  },
  mounted () {
    this.load()
    articleService.config().then((result) => {
      this.config.ready = true
      if (result.code === 0) {
        const catalog = UIUtil.treeSelect(result.data.catalog)
        Object.assign(this.config, result.data, { catalog: catalog.tree })
      }
    })
  }
}
</script>
