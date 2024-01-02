# test case for es

## 最佳实践

### 类型建议
- 尽可能使用整数替代浮点数
- 尽可能使用TERM级别的匹配查询

## 类型性能对比

### 数据准备
- 映射文件
```
PUT /fs_test
{
    "settings": {
        "index": {
            "number_of_shards": "1",
            "number_of_replicas": "0",
            "store": {
                "type": "mmapfs",
                "preload": [
                    "*"
                ]
            }
        }
    },
    "mappings": {
        "_source": {
            "enabled": true
        },
        "properties": {
            "t_tt": {
                "type": "text",
                "index": true,
                "store": true
            },
            "t_ff": {
                "type": "text",
                "index": false,
                "store": false
            },
            "t_tf": {
                "type": "text",
                "index": true,
                "store": false
            },
            "t_ft": {
                "type": "text",
                "index": false,
                "store": true
            },
              "k_tt": {
                "type": "keyword",
                "index": true,
                "store": true
            },
            "k_ff": {
                "type": "keyword",
                "index": false,
                "store": false
            },
            "k_tf": {
                "type": "keyword",
                "index": true,
                "store": false
            },
            "k_ft": {
                "type": "keyword",
                "index": false,
                "store": true
            },
            "k_type": {
              "type": "keyword",
              "index": true,
              "store": true
            },
            "t_type": {
              "type": "text",
              "index": true,
              "store": true
            },
            "k_state": {
              "type": "keyword",
              "index": true,
              "store": true
            },
            "t_state": {
              "type": "text",
              "index": true,
              "store": true
            },
            "province": {
              "type": "keyword",
              "index": true,
              "store": true
            },
            "registry_time": {
              "type": "long",
              "index": true,
              "store": true
            },
            "comprehensive_score": {
              "type": "double",
              "index": true,
              "store": true
            },
            "updated_time": {
              "type": "long",
              "index": true,
              "store": true
            }
        }
    }
}
GET /fs_test/_mapping
// index: true不展示， store: false不展示
```
### 测试用例
- 模糊匹配
```
// "took" : 9315
POST /fs_test/_search
{
  "from": 0,
  "size": 1,
  "query": {
    "bool": {
      "must": [
        {
          "bool": {
            "should": [
              {
                "wildcard": {
                  "k_tt": {
                    "value": "*股份*"
                  }
                }
              }
            ]
          }
        }
      ]
    }
  }
}
```
- 短语查询
```
// "took" : 3
POST /fs_test/_search
{
  "from": 0,
  "size": 1,
  "query": {
    "bool": {
      "must": [
        {
          "bool": {
            "should": [
              {
                "match_phrase": {
                  "t_tt": {
                    "query": "股份",
                    "slop": 0
                  }
                }
              }
            ]
          }
        }
      ]
    }
  }
}
```
- 数值排序
```
// "took" : 3
POST /fs_test/_search
{
  "from": 0,
  "size": 1,
  "query": {
    "match_all": {}
  },
  "sort": [
    {
      "registry_time": {
        "order": "desc"
      }
    }
  ]
}
// "took" : 1183
POST /fs_test/_search
{
  "from": 0,
  "size": 1,
  "query": {
    "match_all": {}
  },
  "sort": [
    {
      "comprehensive_score": {
        "order": "desc"
      }
    }
  ]
}
```
- 过滤聚合
```
// "took" : 7745
POST /fs_test/_search
{
  "query": {
    "match_all": {}
  },
  "aggregations": {
    "1年内": {
      "filter": {
        "bool": {
          "must": [
            {
              "range": {
                "registry_time": {
                  "from": 1667750400000,
                  "to": null,
                  "include_lower": false,
                  "include_upper": true
                }
              }
            }
          ]
        }
      }
    },
    "1-3年": {
      "filter": {
        "bool": {
          "must": [
            {
              "range": {
                "registry_time": {
                  "from": 1604678400000,
                  "to": 1667750400000,
                  "include_lower": false,
                  "include_upper": true
                }
              }
            }
          ]
        }
      }
    },
    "3-5年": {
      "filter": {
        "bool": {
          "must": [
            {
              "range": {
                "registry_time": {
                  "from": 1541520000000,
                  "to": 1604678400000,
                  "include_lower": false,
                  "include_upper": true
                }
              }
            }
          ]
        }
      }
    },
    "5-10年": {
      "filter": {
        "bool": {
          "must": [
            {
              "range": {
                "registry_time": {
                  "from": 1383753600000,
                  "to": 1541520000000,
                  "include_lower": false,
                  "include_upper": true
                }
              }
            }
          ]
        }
      }
    },
    "10-15年": {
      "filter": {
        "bool": {
          "must": [
            {
              "range": {
                "registry_time": {
                  "from": 1225987200000,
                  "to": 1383753600000,
                  "include_lower": false,
                  "include_upper": true
                }
              }
            }
          ]
        }
      }
    },
    "15-20年": {
      "filter": {
        "bool": {
          "must": [
            {
              "range": {
                "registry_time": {
                  "from": 1068134400000,
                  "to": 1225987200000,
                  "include_lower": false,
                  "include_upper": true
                }
              }
            }
          ]
        }
      }
    },
    "20年以上": {
      "filter": {
        "bool": {
          "must": [
            {
              "range": {
                "registry_time": {
                  "from": null,
                  "to": 1068134400000,
                  "include_lower": true,
                  "include_upper": true
                }
              }
            }
          ]
        }
      }
    }
  }
}
```
- 范围聚合
```
// "took" : 2313
POST /fs_test/_search
{
  "query": {
    "match_all": {}
  },
  "aggregations": {
    "registry_time_ranges": {
      "range": {
        "field": "registry_time",
        "ranges": [
          {
            "to": 1068134400000
          },
          {
            "from": 1068134400000,
            "to": 1225987200000
          },
          {
            "from": 1225987200000,
            "to": 1383753600000
          },
          {
            "from": 1383753600000,
            "to": 1541520000000
          },
          {
            "from": 1541520000000,
            "to": 1604678400000
          },
          {
            "from": 1604678400000,
            "to": 1667750400000
          },
          {
            "from": 1667750400000
          }
        ]
      }
    }
  }
}
```
