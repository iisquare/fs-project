import config from './config'

export default {
  /**
   * sorted:  x  b  f  d  g  e
   * source:  a  b  x  c  f  e  g  h
   * dest->:  a  x  b  c  f  g  e  h
   */
  mergeViewerItem: {
    sorted: [{
      field: 'x', enabled: false
    }, {
      field: 'b', enabled: false
    }, {
      field: 'f', enabled: true
    }, {
      field: 'd', enabled: true
    }, {
      field: 'g', enabled: false
    }, {
      field: 'e', enabled: true
    }],
    source: [{
      field: 'a'
    }, {
      field: 'b'
    }, {
      field: 'x'
    }, {
      field: 'c'
    }, {
      field: 'f'
    }, {
      field: 'e'
    }, {
      field: 'g'
    }, {
      field: 'h'
    }],
    test () {
      console.log('source', this.source.map(item => item.field))
      console.log('sorted', this.sorted.map(item => item.field))
      const dest = config.exhibition.mergeViewerItem(this.source, this.sorted)
      console.log('dest->', dest.map(item => item.field), dest)
    }
  }
}
