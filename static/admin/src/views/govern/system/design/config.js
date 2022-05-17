const config = {
}

export default Object.assign(config, {
  sourceArgComponents: {
    default: { component: () => import('./SourceArgDefault') },
    mysql: { component: () => import('./SourceArgMySQL') }
  },
  sourceTypes: [
    { type: 'mysql', title: 'MySQL' }
  ]
})
