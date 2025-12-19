import DesignUtil from '@/utils/DesignUtil'

const config = {
}

const CanvasOptions = () => {
  return {}
}

const EdgeOptions = () => {
  return {}
}

const DefaultOptions = () => {
  return {}
}

const BochaOptions = () => {
  return { token: '' }
}

const BochaWebSearchOptions = () => {
  return { query: '', count: 10 }
}

const tools = [{
  type: 'Bocha', name: '博查', icon: '/images/agentic/bocha.svg', url: 'https://open.bochaai.com/', options: BochaOptions,
  description: '博查是一个给AI用的中文搜索引擎，让你的AI应用从近百亿网页和生态内容源中获取高质量的世界知识，涵盖天气、新闻、百科、医疗、火车票、图片等多种领域。',
  setting: [{
    name: 'token', type: 'String', required: true, description: '认证标识', secrecy: true,
  }],
  actions: [{
    type: 'BochaWebSearch', name: 'Web Search API',
    description: '从全网搜索任何网页信息和网页链接，结果准确、摘要完整，更适合AI使用。',
    options: BochaWebSearchOptions, property: () => import('./BochaWebSearchProperty.vue'),
    input: [{
      name: 'query', type: 'String', required: true, description: '用户的搜索词',
    }, {
      name: 'count', type: 'Integer', required: false, default: 10 , description: '返回结果的条数',
    }],
    output: [{
      name: 'json', type: 'Array<Object>', description: '工具生成的JSON对象数组',
    }],
  }]
}]

export default Object.assign(config, {
  canvas: { options: CanvasOptions, property: () => import('./CanvasProperty.vue') },
  edge: { options: EdgeOptions, property: () => import('./EdgeProperty.vue') },
  widgets: DesignUtil.widgets(([{
    name: '基础节点',
    children: [{
      type: 'Input', label: '输入', title: '输入节点', icon: 'Notification', options: DefaultOptions, property: () => import('./DefaultProperty.vue')
    }, {
      type: 'Output', label: '输出', title: '输出节点', icon: 'Notification', options: DefaultOptions, property: () => import('./DefaultProperty.vue')
    }, {
      type: 'LLM', label: '语言模型', title: '调用大语言模型', icon: 'Notification', options: DefaultOptions, property: () => import('./DefaultProperty.vue')
    }, {
      type: 'Knowledge', label: '知识库', title: '调用知识库', icon: 'Notification', options: DefaultOptions, property: () => import('./DefaultProperty.vue')
    }]
  }, {
    name: '问题理解',
    children: [{
      type: 'Intent', label: '意图识别', title: '对用于意图进行分类', icon: 'Notification', options: DefaultOptions, property: () => import('./DefaultProperty.vue')
    }]
  }, {
    name: '逻辑处理',
    children: [{
      type: 'Switch', label: '条件分支', title: '分支判断', icon: 'Notification', options: DefaultOptions, property: () => import('./DefaultProperty.vue')
    }, {
      type: 'Iterate', label: '迭代器', title: '循环处理', icon: 'Notification', options: DefaultOptions, property: () => import('./DefaultProperty.vue')
    }]
  }, {
    name: '数据转换',
    children: [{
      type: 'Code', label: '代码执行', title: '执行代码段', icon: 'Notification', options: DefaultOptions, property: () => import('./DefaultProperty.vue')
    }, {
      type: 'Template', label: '模板转换', title: '将数据转换为模板字符串', icon: 'Notification', options: DefaultOptions, property: () => import('./DefaultProperty.vue')
    }]
  }, {
    name: '内部工具',
    children: [{
      type: 'HTTP', label: 'HTTP请求', title: '调用API接口', icon: 'Notification', options: DefaultOptions, property: () => import('./DefaultProperty.vue')
    }]
  }] as any).push(...tools.map(tool => {
    return { name: tool.name, hide: true, children: tool.actions.map(action => {
      return { type: action.type, label: action.name, icon: tool.icon, options: tool.options, property: action.property, hide: true }
    })}
  }))),
  tools,
  status: [], // 由后台服务补齐
})
