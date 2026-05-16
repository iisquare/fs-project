## 节点信息
- 节点状态
    - 接口地址：/node/state
	- 请求方式：POST
	- 功能说明：获取节点状态
	- 接口参数：无
	- 返回结果：
	```
    {
        "id": "0.0.0.0:8433",
        "state": "STARTED",
        "leader": "0.0.0.0:8433",
        "leadership": true,
        "counter": {},
        "pool": {
            "minIdle": 1,
            "maxIdle": 5,
            "maxTotal": 500
        },
        "fetcher": {
            "numActive": 1,
            "numIdle": 4
        },
        "worker": {
            "numActive": 2,
            "numIdle": 3
        }
    }
    ```
- 接口映射
    - 接口地址：/node/mapping
	- 请求方式：POST
	- 功能说明：获取接口映射列表
	- 接口参数：无
	- 返回结果：暂无描述
## 集群信息
- 集群状态
    - 接口地址：/nodes/state
	- 请求方式：POST
	- 功能说明：获取集群状态信息
	- 接口参数：无
	- 返回结果：
	```
    {
        "code": 0,
        "message": "操作成功",
        "data": {
            "nodes": {
                "0.0.0.0:8433": {...}
            },
            "channel": {
                "size": 5,
                "overstock": 0,
                "top": {
                    "score": 1567482495122,
                    "item": {
                        "id": "3e27bbd6-cacb-41c2-b2ce-9ee529c943e4",
                        "halt": 0,
                        "priority": 0,
                        "groupName": "lianjia",
                        "scheduleId": "27",
                        "doneCheckCount": 0,
                        "version": 1564707019425
                    }
                }
            }
        }
    }
    ```
## 作业调度
- 语法解析
    - 接口地址：/schedule/parser
	- 请求方式：POST
	- 功能说明：检测解析器语法是否存在异常
	- 接口参数：Request Payload
	- 返回结果：
	```
    {
        "code": 0,
        "message": "提示信息",
        "data": {...} // 解析结果
    }
    ```
- 保存配置
    - 接口地址：/schedule/save
	- 请求方式：POST
	- 功能说明：将配置信息同步到ZK中
	- 接口参数：Request Payload

	| 名称 | 类型 | 必填 | 默认 | 说明 |
	| :----- | :-----  | :----- | :----- | :----- |
	| type | String | Y | 无 | 信息类型(schedule-作业模板,group-分组，proxy-代理) |
	| content | String | Y | 无 | 配置内容 |

	- 返回结果：暂无描述
- 删除配置
    - 接口地址：/schedule/remove
	- 请求方式：POST
	- 功能说明：根据标识删除对应配置
	- 接口参数：Request Payload

	| 名称 | 类型 | 必填 | 默认 | 说明 |
	| :----- | :-----  | :----- | :----- | :----- |
	| type | String | Y | 无 | 信息类型 |
	| id | String | Y | 无 | 标识 |

	- 返回结果：暂无描述
- 作业列表
    - 接口地址：/schedule/list
	- 请求方式：POST
	- 功能说明：获取作业配置信息列表
	- 接口参数：无
	- 返回结果：暂无描述
- 分组列表
    - 接口地址：/schedule/group
	- 请求方式：POST
	- 功能说明：获取分组配置信息列表
	- 接口参数：无
	- 返回结果：
    ```
    {
        "code": 0,
        "message": "操作成功",
        "data": [
            {
                "name": "58",
                "target": "broadcast",
                "interval": 300,
                "concurrent": 1
            }
        ]
    }
    ```
- 代理列表
    - 接口地址：/schedule/proxy
	- 请求方式：POST
	- 功能说明：获取代理配置信息列表
	- 接口参数：无
	- 返回结果：
	```
    {
        "code": 0,
        "message": "操作成功",
        "data": [
            {
                "name": "node1",
                "target": "0.0.0.0:8433",
                "schema": "HTTP",
                "host": "127.0.0.1",
                "port": 80,
                "connectTimeout": 1000,
                "socketTimeout": 10000
            }
        ]
    }
    ```
- 调度历史
    - 接口地址：/schedule/history
	- 请求方式：POST
	- 功能说明：获取作业执行状态信息
	- 接口参数：无
	- 返回结果：
	```
    {
        "code": 0,
        "message": "操作成功",
        "data": [
            {
                "scheduleId": "29",
                "dispatch": 1565053978740,
                "version": 1567391110432,
                "status": "RUNNING",
                "token": 1,
                "limit": 0,
                "channel": 1723630,
                "top": {
                    "score": 0,
                    "item": {
                        "id": "6f89734c-8944-4e1b-a6e3-84fb91693737",
                        "url": "https://shanghai.anjuke.com/prop/view/A1699931197?from=filter&spread=filtersearch_p&uniqid=pc5d5b5d5de2bfe4.49586524&trading_area_ids=7074&region_ids=18&position=1385&kwtype=filter&now_time=1566268765",
                        "scheduleId": "29",
                        "templateKey": "sale_house",
                        "dispatchTime": 1566268820995,
                        "priority": 0,
                        "dispatchInterval": 0,
                        "retryCount": 0,
                        "iterateCount": 0,
                        "param": {
                            "citycode": "shanghai",
                            "name": "金山万达华府 景观房，南北通透毛坯，看房钥匙在手 带产权车位",
                            "cityen": "sh",
                            "citycn": "上海",
                            "href": "https://shanghai.anjuke.com/prop/view/A1699931197?from=filter&spread=filtersearch_p&uniqid=pc5d5b5d5de2bfe4.49586524&trading_area_ids=7074&region_ids=18&position=1385&kwtype=filter&now_time=1566268765"
                        }
                    }
                }
            }
        ]
    }
    ```
- 数据输出
    - 接口地址：/schedule/output
	- 请求方式：POST
	- 功能说明：获取数据输出器列表
	- 接口参数：无
	- 返回结果：暂无描述
- 调度器状态
    - 接口地址：/schedule/state
	- 请求方式：POST
	- 功能说明：获取调度器状态信息
	- 接口参数：无
	- 返回结果：暂无描述
    ```
- 调度器开启
    - 接口地址：/schedule/start
	- 请求方式：POST
	- 功能说明：启用调度器
	- 接口参数：无
	- 返回结果：暂无描述
- 调度器关闭
    - 接口地址：/schedule/stop
	- 请求方式：POST
	- 功能说明：关闭调度器
	- 接口参数：无
	- 返回结果：暂无描述
- 更改状态
    - 接口地址：/schedule/change
	- 请求方式：POST
	- 功能说明：切换作业执行状态
	- 接口参数：Request Payload

	| 名称 | 类型 | 必填 | 默认 | 说明 |
	| :----- | :-----  | :----- | :----- | :----- |
	| id | String | Y | 无 | 作业标识 |
	| status | String | Y | 无 | 状态(start-启动,pause-暂停,stop-结束) |

	- 返回结果：
	```
    {
        "code": 0,
        "message": "操作成功",
        "data": {
            "scheduleId": "27",
            "dispatch": 1564643326078,
            "version": 1567492261484,
            "status": "PAUSE",
            "token": 5,
            "limit": 0
        }
    }
    ```
- 清空列表
    - 接口地址：/schedule/clear
	- 请求方式：POST
	- 功能说明：清空单个作业的任务积压队列
	- 接口参数：Request Payload

	| 名称 | 类型 | 必填 | 默认 | 说明 |
	| :----- | :-----  | :----- | :----- | :----- |
	| id | String | Y | 无 | 作业标识 |

	- 返回结果：
	```
    {
        "code": 0,
        "message": "操作成功",
        "data": true
    }
    ```
- 提交作业
    - 接口地址：/schedule/submit
	- 请求方式：POST
	- 功能说明：将作业加入到调度队列
	- 接口参数：Request Payload

	| 名称 | 类型 | 必填 | 默认 | 说明 |
	| :----- | :-----  | :----- | :----- | :----- |
	| id | String | Y | 无 | 作业标识 |

	- 返回结果：暂无描述
- 提交任务
    - 接口地址：/schedule/execute
	- 请求方式：POST
	- 功能说明：触发作业的单个任务
	- 接口参数：Request Payload

	| 名称 | 类型 | 必填 | 默认 | 说明 |
	| :----- | :-----  | :----- | :----- | :----- |
	| scheduleId | String | Y | 无 | 作业标识 |
	| templateKey | String | Y | 无 | 页面描述 |
	| parameters | String | Y | 无 | 任务参数 |

	- 返回结果：暂无描述
    ```
