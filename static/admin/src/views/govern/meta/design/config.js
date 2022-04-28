const config = {
}

export default Object.assign(config, {
  modelCompareComponents: {
    default: { component: () => import('./ModelCompareDefault') },
    mysql: { component: () => import('./ModelCompareMySQL') },
    csv: { component: () => import('./ModelCompareCSV') }
  },
  modelCompareTypes: [
    { type: 'mysql', title: 'MySQL', logo: '/logo/mysql.png' },
    { type: 'csv', title: 'CSV / TXT', logo: '/logo/csv.png' }
  ]
})
