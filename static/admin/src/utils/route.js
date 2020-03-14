const RouteUtil = {
  query2filter (obj, filter) {
    return Object.assign({}, filter, obj.$route.query)
  },
  filter2query (obj, filter) {
    return obj.$router.push({
      path: obj.$route.fullPath,
      query: filter
    }).catch(err => err)
  }
}

export default RouteUtil
