const api = (uri: string, params: Record<string, any> = {}) => ({
  app: 'govern',
  uri,
  method: 'POST',
  params: {
    time: Date.now(),
    ...params,
  },
})

export default {
  reindexES(params: Record<string, any> = {}) {
    return api('/maintain/reindexES', params)
  },
  reindexNeo4j(params: Record<string, any> = {}) {
    return api('/maintain/reindexNeo4j', params)
  },
}
