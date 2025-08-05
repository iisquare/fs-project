import base from './Api'

export default {
  createIndex (param: any, tips = {}) {
    return base.post('/neo4j/createIndex', param, tips)
  },
  showIndex (param: any, tips = {}) {
    return base.post('/neo4j/showIndex', param, tips)
  },
  dropIndex (param: any, tips = {}) {
    return base.post('/neo4j/dropIndex', param, tips)
  },
  createConstraint (param: any, tips = {}) {
    return base.post('/neo4j/createConstraint', param, tips)
  },
  showConstraint (param: any, tips = {}) {
    return base.post('/neo4j/showConstraint', param, tips)
  },
  dropConstraint (param: any, tips = {}) {
    return base.post('/neo4j/dropConstraint', param, tips)
  },
}
