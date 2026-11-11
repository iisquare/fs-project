package com.iisquare.fs.web.bi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.jpa.util.JPAUtil;
import com.iisquare.fs.base.web.mvc.ServiceBase;
import com.iisquare.fs.base.web.util.RpcUtil;
import com.iisquare.fs.web.bi.dao.DiagramDao;
import com.iisquare.fs.web.bi.entity.Cluster;
import com.iisquare.fs.web.bi.entity.Datasource;
import com.iisquare.fs.web.bi.entity.Diagram;
import com.iisquare.fs.web.bi.entity.Job;
import com.iisquare.fs.web.bi.datasource.MongoDBConnector;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.core.rpc.FlinkRpc;
import com.iisquare.fs.web.core.rpc.KGRpc;
import com.iisquare.fs.web.core.rpc.SparkRpc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class DAGService extends ServiceBase {

    @Autowired
    DiagramDao diagramDao;
    @Autowired
    ClusterService clusterService;
    @Autowired
    JobService jobService;
    @Autowired
    DatasourceService datasourceService;
    @Autowired
    AlarmService alarmService;
    @Autowired
    DefaultRbacService rbacService;
    @Autowired
    SparkRpc sparkRpc;
    @Autowired
    FlinkRpc flinkRpc;
    @Autowired
    KGRpc kgRpc;
    @Value("${fs.bi.debug.preview-limit:20}")
    int debugPreviewLimit;

    private static final Pattern SAFE_NAME = Pattern.compile("^[A-Za-z][A-Za-z0-9_]*$");

    /**
     * 兼容旧接口：按流程默认集群提交
     */
    public Map<String, Object> run(Integer id) {
        return run(id, DPUtil.buildMap(), null, Job.TRIGGER_MANUAL);
    }

    /**
     * 提交流批计算作业：根据流程配置选择计算引擎与目标集群
     */
    public Map<String, Object> run(Integer id, Map<?, ?> param, HttpServletRequest request, String triggerType) {
        Diagram diagram = JPAUtil.findById(diagramDao, id, Diagram.class);
        if (null == diagram || 1 != diagram.getStatus()) {
            return ApiUtil.result(404, "编排流程不存在或已禁用", id);
        }
        ObjectNode frame = diagram(id, DPUtil.objectNode());
        if (null == frame) return ApiUtil.result(404, "编排流程不存在或已禁用", id);
        int clusterId = DPUtil.parseInt(param.get("clusterId"));
        if (clusterId < 1) clusterId = DPUtil.parseInt(diagram.getClusterId());
        Cluster cluster = clusterService.info(clusterId);
        frame.set("cluster", clusterNode(cluster, diagram.getEngine()));
        frame.set("data", param.containsKey("data") ? DPUtil.toJSON(param.get("data")) : DPUtil.objectNode());
        if (param.containsKey("diagramUrl")) frame.put("diagramUrl", DPUtil.parseString(param.get("diagramUrl")));
        ObjectNode executionFrame = frame.deepCopy();
        String datasourceError = resolveDatasources(executionFrame);
        if (!DPUtil.empty(datasourceError)) return ApiUtil.result(1003, datasourceError, id);
        String ontologyError = resolveOntologies(executionFrame);
        if (!DPUtil.empty(ontologyError)) return ApiUtil.result(1003, ontologyError, id);
        int uid = null == request ? 0 : rbacUid(request);
        Job job = jobService.create(diagram, cluster, triggerType, frame.at("/data"), frame, uid);
        return submit(job, diagram, executionFrame, triggerType);
    }

    /**
     * 指定节点调试：仅执行至目标节点，返回各节点输出结果
     */
    public Map<String, Object> debug(Integer id, Map<?, ?> param, HttpServletRequest request) {
        Diagram diagram = JPAUtil.findById(diagramDao, id, Diagram.class);
        if (null == diagram || 1 != diagram.getStatus()) {
            return ApiUtil.result(404, "编排流程不存在或已禁用", id);
        }
        String nodeId = DPUtil.trim(DPUtil.parseString(param.get("nodeId")));
        if (DPUtil.empty(nodeId)) return ApiUtil.result(1001, "调试节点不能为空", nodeId);
        ObjectNode frame = diagram(id, DPUtil.objectNode());
        if (null == frame) return ApiUtil.result(404, "编排流程不存在或已禁用", id);
        frame.set("cluster", clusterNode(null, diagram.getEngine()));
        frame.put("debug", nodeId);
        int previewLimit = DPUtil.parseInt(param.get("previewLimit"));
        if (previewLimit < 1) previewLimit = debugPreviewLimit;
        previewLimit = Math.max(1, Math.min(previewLimit, 1000));
        frame.put("debugLimit", previewLimit);
        frame.set("data", param.containsKey("data") ? DPUtil.toJSON(param.get("data")) : DPUtil.objectNode());
        ObjectNode executionFrame = frame.deepCopy();
        String datasourceError = resolveDatasources(executionFrame);
        if (!DPUtil.empty(datasourceError)) return ApiUtil.result(1003, datasourceError, id);
        String ontologyError = resolveOntologies(executionFrame);
        if (!DPUtil.empty(ontologyError)) return ApiUtil.result(1003, ontologyError, id);
        int uid = null == request ? 0 : rbacUid(request);
        Job job = jobService.create(diagram, null, Job.TRIGGER_DEBUG, frame.at("/data"), frame, uid);
        Map<String, Object> result = submit(diagram.getEngine(), executionFrame);
        if (ApiUtil.succeed(result)) {
            JsonNode data = DPUtil.toJSON(ApiUtil.data(result, Object.class));
            if (data.path("succeeded").asBoolean(true)) {
                jobService.debugged(job, data.at("/outputs"));
            } else {
                String message = data.path("message").asText("节点调试失败");
                jobService.failed(job, message);
            }
        } else {
            jobService.failed(job, ApiUtil.message(result));
            alarmService.alarm(job, Job.TRIGGER_DEBUG, ApiUtil.message(result));
        }
        result.put("job", job.getId());
        return result;
    }

    private Map<String, Object> submit(Job job, Diagram diagram, ObjectNode frame, String triggerType) {
        Map<String, Object> result = submit(diagram.getEngine(), frame);
        if (!ApiUtil.succeed(result)) {
            jobService.failed(job, ApiUtil.message(result));
            alarmService.alarm(job, "SUBMIT_FAILED", ApiUtil.message(result));
            result.put("job", job.getId());
            return result;
        }
        JsonNode data = DPUtil.toJSON(ApiUtil.data(result, Object.class));
        String remoteId = remoteJobId(data);
        // 远程提交与流式计算均为异步执行，本地批处理同步完成后即为终态
        boolean asynchronous = "remote".equals(frame.at("/cluster/mode").asText(""))
                || "stream".equals(diagram.getModel());
        String state = asynchronous ? Job.STATE_RUNNING : Job.STATE_FINISHED;
        jobService.submitted(job, remoteId, state, data);
        result.put("job", job.getId());
        result.put("state", state);
        return result;
    }

    /**
     * 从远程提交返回结果中提取作业标识
     */
    public String remoteJobId(JsonNode data) {
        if (null == data || data.isNull()) return "";
        if (data.isTextual()) return data.asText();
        for (String field : new String[]{"JobId", "jobid", "jobId", "submissionId"}) {
            if (data.has(field) && !data.get(field).isNull()) return data.get(field).asText();
        }
        // 流式作业可能返回多个流查询，取首个查询的标识
        JsonNode queries = data.at("/queries");
        if (queries.isArray() && queries.size() > 0) {
            for (String field : new String[]{"submissionId", "jobId", "id"}) {
                if (queries.get(0).has(field) && !queries.get(0).get(field).isNull()) {
                    return queries.get(0).get(field).asText();
                }
            }
        }
        return "";
    }

    /**
     * 构造传递给计算引擎的集群配置节点
     */
    public ObjectNode clusterNode(Cluster cluster, String engine) {
        ObjectNode node = DPUtil.objectNode();
        if (null == cluster || !"remote".equals(cluster.getMode())) {
            node.put("mode", "local");
            return node;
        }
        node.put("mode", "remote");
        JsonNode content = DPUtil.parseJSON(cluster.getContent());
        if (content.isObject()) node.setAll((ObjectNode) content);
        if (DPUtil.empty(node.at("/entryClass").asText(""))) {
            node.put("entryClass", "flink".equals(engine)
                    ? "com.iisquare.fs.app.flink.FlinkApplication"
                    : "com.iisquare.fs.app.spark.SparkApplication");
        }
        return node;
    }

    /**
     * 将编排节点选择的数据源展开为计算引擎所需的连接参数。
     *
     * 该转换只作用于提交和调试时的临时 frame，不写回编排内容，也不向前端回传数据源密码。
     * 未配置 datasourceId 的旧编排继续使用节点自身的 options，保持向后兼容。
     */
    private String resolveDatasources(ObjectNode frame) {
        JsonNode nodes = frame.get("nodes");
        if (null == nodes || !nodes.isArray()) return null;
        Map<Integer, Datasource> cache = new HashMap<>();
        for (JsonNode item : nodes) {
            if (!item.isObject()) continue;
            ObjectNode node = (ObjectNode) item;
            ObjectNode options = (ObjectNode) node.get("options");
            if (null == options || !options.isObject()) continue;
            int datasourceId = options.at("/datasourceId").asInt(0);
            if (datasourceId < 1) continue;
            Datasource datasource = cache.get(datasourceId);
            if (null == datasource) {
                datasource = datasourceService.info(datasourceId);
                cache.put(datasourceId, datasource);
            }
            String nodeName = node.at("/name").asText(node.at("/id").asText("未命名节点"));
            if (null == datasource || !Integer.valueOf(1).equals(datasource.getStatus())) {
                return String.format("节点“%s”引用的数据源不存在或已禁用", nodeName);
            }
            String error = applyDatasource(options, node.at("/type").asText(), datasource);
            if (!DPUtil.empty(error)) {
                return String.format("节点“%s”%s", nodeName, error);
            }
        }
        return null;
    }

    private String applyDatasource(ObjectNode options, String nodeType, Datasource datasource) {
        JsonNode config = DPUtil.parseJSON(datasource.getContent());
        if (null == config || !config.isObject()) {
            return String.format("引用的数据源“%s”配置格式异常", datasource.getName());
        }
        String type = datasource.getType();
        switch (nodeType) {
            case "JDBCSource":
            case "JDBCSink": {
                if (!isDatasourceType(type, "mysql", "doris", "postgresql", "postgres")) {
                    return "只能引用 MySQL、Doris 或 PostgreSQL 数据源";
                }
                String host = config.at("/host").asText();
                if (DPUtil.empty(host)) return String.format("引用的数据源“%s”缺少主机地址", datasource.getName());
                boolean postgresql = "postgresql".equals(type) || "postgres".equals(type);
                String protocol = postgresql ? "postgresql" : "mysql";
                int port = config.at("/port").asInt(0);
                String url = "jdbc:" + protocol + "://" + host;
                if (port > 0) url += ":" + port;
                url += "/" + config.at("/database").asText("");
                String params = config.at("/paramsQueryString").asText("");
                if (!DPUtil.empty(params)) url += "?" + params;
                options.put("url", url);
                options.put("driver", postgresql ? "org.postgresql.Driver" : "com.mysql.cj.jdbc.Driver");
                options.put("username", config.at("/username").asText(""));
                options.put("password", config.at("/password").asText(""));
                return null;
            }
            case "MySQLCapture": {
                if (!"mysql".equals(type)) return "只能引用 MySQL 数据源";
                String host = config.at("/host").asText();
                if (DPUtil.empty(host)) return String.format("引用的数据源“%s”缺少主机地址", datasource.getName());
                options.put("hostname", host);
                options.put("port", config.at("/port").asInt(3306));
                options.put("username", config.at("/username").asText(""));
                options.put("password", config.at("/password").asText(""));
                options.put("database", config.at("/database").asText(""));
                return null;
            }
            case "PostgreSQLCapture": {
                if (!isDatasourceType(type, "postgresql", "postgres")) return "只能引用 PostgreSQL 数据源";
                String host = config.at("/host").asText();
                if (DPUtil.empty(host)) return String.format("引用的数据源“%s”缺少主机地址", datasource.getName());
                options.put("hostname", host);
                options.put("port", config.at("/port").asInt(5432));
                options.put("username", config.at("/username").asText(""));
                options.put("password", config.at("/password").asText(""));
                options.put("database", config.at("/database").asText(""));
                return null;
            }
            case "MongoCapture": {
                if (!"mongodb".equals(type)) return "只能引用 MongoDB 数据源";
                String uri = new MongoDBConnector(type, config).connectionUrl();
                String hosts = mongoHosts(uri);
                if (DPUtil.empty(hosts)) return String.format("引用的数据源“%s”缺少主机地址", datasource.getName());
                options.put("hosts", hosts);
                options.put("username", config.at("/username").asText(""));
                options.put("password", config.at("/password").asText(""));
                if (uri.startsWith("mongodb+srv://")) options.put("scheme", "mongodb+srv");
                String connectionOptions = mongoOptions(uri);
                if (!DPUtil.empty(connectionOptions)) options.put("connectionOptions", connectionOptions);
                if (DPUtil.empty(options.at("/databaseList").asText(""))) {
                    String database = config.at("/database").asText("");
                    if (DPUtil.empty(database)) database = mongoDatabase(uri);
                    if (!DPUtil.empty(database)) options.put("databaseList", database);
                }
                return null;
            }
            case "MongoSource":
            case "MongoSink": {
                if (!"mongodb".equals(type)) return "只能引用 MongoDB 数据源";
                String uri = new MongoDBConnector(type, config).connectionUrl();
                if (DPUtil.empty(uri)) return String.format("引用的数据源“%s”缺少连接地址", datasource.getName());
                options.put("uri", uri);
                String database = config.at("/database").asText("");
                if (DPUtil.empty(database)) database = mongoDatabase(uri);
                if (DPUtil.empty(database)) return String.format("引用的数据源“%s”未指定数据库", datasource.getName());
                options.put("database", database);
                return null;
            }
            case "ElasticsearchSource":
            case "ElasticsearchSink": {
                if (!"elasticsearch".equals(type)) return "只能引用 Elasticsearch 数据源";
                String uris = config.at("/uris").asText("");
                if (DPUtil.empty(uris)) return String.format("引用的数据源“%s”缺少服务地址", datasource.getName());
                options.put("servers", uris);
                options.put("username", config.at("/username").asText(""));
                options.put("password", config.at("/password").asText(""));
                if (DPUtil.empty(options.at("/collection").asText()) && options.has("index")) {
                    options.set("collection", options.get("index"));
                }
                return null;
            }
            case "Neo4jSource":
            case "Neo4jSink":
            case "OntologySource":
            case "OntologySink": {
                if (!"neo4j".equals(type)) return "只能引用 Neo4j 数据源";
                String uri = config.at("/uri").asText("");
                if (DPUtil.empty(uri)) uri = config.at("/url").asText("");
                if (DPUtil.empty(uri)) return String.format("引用的数据源“%s”缺少连接地址", datasource.getName());
                options.put("uri", uri);
                options.put("username", config.at("/username").asText(""));
                options.put("password", config.at("/password").asText(""));
                options.put("database", config.at("/database").asText(""));
                options.put("connectTimeout", config.at("/connectTimeout").asInt(3000));
                options.put("maxConnectionLifetime", config.at("/maxConnectionLifetime").asInt(3600000));
                options.put("maxSize", config.at("/maxSize").asInt(20));
                return null;
            }
            case "KafkaSource":
            case "KafkaSink": {
                if (!"kafka".equals(type)) return "只能引用 Kafka 数据源";
                String servers = config.at("/servers").asText("");
                if (DPUtil.empty(servers)) servers = config.at("/bootstrap").asText("");
                if (DPUtil.empty(servers)) return String.format("引用的数据源“%s”缺少服务地址", datasource.getName());
                options.put("servers", servers);
                options.put("bootstrap", servers);
                options.put("securityProtocol", config.at("/securityProtocol").asText(""));
                options.put("saslMechanism", config.at("/saslMechanism").asText(""));
                options.put("saslJaasConfig", config.at("/saslJaasConfig").asText(""));
                options.put("connectTimeout", config.at("/connectTimeout").asInt(5000));
                return null;
            }
            default:
                return String.format("不支持引用数据源“%s”", datasource.getName());
        }
    }

    /**
     * 知识图谱节点在提交时按本体定义生成 Cypher。
     *
     * 本体只用于解析实体标签、主键和字段约束，实际图数据仍写入节点所选的 Neo4j 数据源。
     * 生成后的 frame 只用于本次执行，不写回编排配置。
     */
    private String resolveOntologies(ObjectNode frame) {
        JsonNode nodes = frame.get("nodes");
        if (null == nodes || !nodes.isArray()) return null;
        Map<Integer, JsonNode> models = new HashMap<>();
        for (JsonNode item : nodes) {
            if (!item.isObject()) continue;
            ObjectNode node = (ObjectNode) item;
            String type = node.at("/type").asText("");
            if (!"OntologySource".equals(type) && !"OntologySink".equals(type)) continue;
            ObjectNode options = node.get("options") instanceof ObjectNode value ? value : DPUtil.objectNode();
            node.set("options", options);
            int ontologyId = options.at("/ontologyId").asInt(0);
            String nodeName = node.at("/name").asText(node.at("/id").asText("未命名节点"));
            if (ontologyId < 1) return String.format("知识图谱节点“%s”未选择本体", nodeName);
            JsonNode model = models.get(ontologyId);
            if (null == model) {
                try {
                    model = RpcUtil.data(kgRpc.post("/ontology/model", DPUtil.buildMap("id", ontologyId)), false);
                } catch (Exception e) {
                    return String.format("知识图谱节点“%s”读取本体失败：%s", nodeName, e.getMessage());
                }
                if (null != model && model.isObject()) models.put(ontologyId, model);
            }
            if (null == model || !model.isObject()) {
                return String.format("知识图谱节点“%s”引用的本体不存在", nodeName);
            }
            if (!DPUtil.empty(model.at("/status").asText(""))) {
                int status = model.at("/status").asInt(0);
                if (status > 0 && status != 1) {
                    return String.format("知识图谱节点“%s”引用的本体已禁用", nodeName);
                }
            }
            String entityKey = options.at("/entityLabel").asText("");
            if (DPUtil.empty(entityKey)) entityKey = options.at("/entity").asText("");
            JsonNode entity = findOntologyEntity(model.at("/entities"), entityKey);
            if (null == entity) {
                return String.format("知识图谱节点“%s”未选择有效的本体实体", nodeName);
            }
            String label = entity.at("/label").asText("");
            String primaryField = entity.at("/primaryField").asText("");
            if (!SAFE_NAME.matcher(label).matches() || !SAFE_NAME.matcher(primaryField).matches()) {
                return String.format("知识图谱节点“%s”的实体标签或主键字段不合法", nodeName);
            }
            options.put("ontologyName", model.at("/name").asText(""));
            options.put("entityLabel", label);
            options.put("entityName", entity.at("/name").asText(label));
            options.put("primaryField", primaryField);
            List<String> requiredFields = new ArrayList<>();
            List<String> allowedFields = new ArrayList<>();
            JsonNode fields = entity.at("/fields");
            if (fields.isArray()) {
                for (JsonNode field : fields) {
                    String name = field.at("/name").asText("");
                    if (!SAFE_NAME.matcher(name).matches()) continue;
                    allowedFields.add(name);
                    if (field.at("/required").asBoolean(false)) requiredFields.add(name);
                }
            }
            options.set("requiredFields", DPUtil.toJSON(requiredFields));
            options.set("allowedFields", DPUtil.toJSON(allowedFields));
            if ("OntologySource".equals(type)) {
                String error = ontologySourceOptions(options, entity);
                if (!DPUtil.empty(error)) return String.format("知识图谱节点“%s”%s", nodeName, error);
            } else {
                options.put("cypher", "UNWIND $rows AS row\n"
                        + "MERGE (n:" + quote(label) + " {" + quote(primaryField) + ": row." + quote(primaryField) + "})\n"
                        + "SET n += row");
            }
        }
        return null;
    }

    private JsonNode findOntologyEntity(JsonNode entities, String key) {
        if (null == entities || !entities.isArray() || DPUtil.empty(key)) return null;
        for (JsonNode entity : entities) {
            if (key.equals(entity.at("/label").asText(""))
                    || key.equals(entity.at("/code").asText(""))
                    || key.equals(entity.at("/id").asText(""))) {
                return entity;
            }
        }
        return null;
    }

    private String ontologySourceOptions(ObjectNode options, JsonNode entity) {
        JsonNode filters = options.get("filters");
        if (null == filters) filters = DPUtil.objectNode();
        if (filters.isTextual()) filters = DPUtil.parseJSON(filters.asText());
        if (null == filters || filters.isNull() || filters.isMissingNode()) filters = DPUtil.objectNode();
        if (!filters.isObject()) return "过滤条件必须是JSON对象";
        StringBuilder cypher = new StringBuilder("MATCH (n:")
                .append(quote(entity.at("/label").asText(""))).append(")");
        ObjectNode params = DPUtil.objectNode();
        int index = 0;
        Iterator<Map.Entry<String, JsonNode>> iterator = filters.fields();
        while (iterator.hasNext()) {
            Map.Entry<String, JsonNode> entry = iterator.next();
            if (!SAFE_NAME.matcher(entry.getKey()).matches()) {
                return "过滤字段[" + entry.getKey() + "]不合法";
            }
            if (null == findOntologyField(entity.at("/fields"), entry.getKey())) {
                return "过滤字段[" + entry.getKey() + "]未在本体中定义";
            }
            String parameter = "p" + index++;
            if (index == 1) cypher.append(" WHERE ");
            else cypher.append(" AND ");
            cypher.append("n.").append(quote(entry.getKey())).append(" = $").append(parameter);
            params.set(parameter, entry.getValue());
        }
        cypher.append(" RETURN n");
        String orderField = options.at("/orderField").asText("");
        if (!DPUtil.empty(orderField)) {
            if (!SAFE_NAME.matcher(orderField).matches()
                    || null == findOntologyField(entity.at("/fields"), orderField)) {
                return "排序字段[" + orderField + "]未在本体中定义";
            }
            String direction = "DESC".equalsIgnoreCase(options.at("/orderDirection").asText())
                    ? "DESC" : "ASC";
            cypher.append(" ORDER BY n.").append(quote(orderField)).append(" ").append(direction);
        }
        options.put("cypher", cypher.toString());
        options.set("params", params);
        return null;
    }

    private JsonNode findOntologyField(JsonNode fields, String key) {
        if (null == fields || !fields.isArray()) return null;
        for (JsonNode field : fields) {
            if (key.equals(field.at("/name").asText(""))) return field;
        }
        return null;
    }

    private String quote(String name) {
        return "`" + name.replace("`", "``") + "`";
    }

    private boolean isDatasourceType(String actual, String... expected) {
        for (String type : expected) {
            if (type.equals(actual)) return true;
        }
        return false;
    }

    private String mongoDatabase(String uri) {
        int scheme = uri.indexOf("://");
        String rest = scheme < 0 ? uri : uri.substring(scheme + 3);
        int slash = rest.indexOf('/');
        if (slash < 0) return "";
        int query = rest.indexOf('?', slash);
        String database = rest.substring(slash + 1, query < 0 ? rest.length() : query);
        return URLDecoder.decode(database, StandardCharsets.UTF_8);
    }

    private String mongoHosts(String uri) {
        int scheme = uri.indexOf("://");
        String rest = scheme < 0 ? uri : uri.substring(scheme + 3);
        int slash = rest.indexOf('/');
        String hosts = slash < 0 ? rest : rest.substring(0, slash);
        int at = hosts.lastIndexOf('@');
        if (at >= 0) hosts = hosts.substring(at + 1);
        int query = hosts.indexOf('?');
        if (query >= 0) hosts = hosts.substring(0, query);
        return hosts;
    }

    private String mongoOptions(String uri) {
        int query = uri.indexOf('?');
        return query < 0 ? "" : uri.substring(query + 1);
    }

    private Map<String, Object> submit(String engine, ObjectNode frame) {
        switch (engine) {
            case "flink":
                return RpcUtil.result(flinkRpc.post("/bi/dag", frame));
            case "spark":
                return RpcUtil.result(sparkRpc.post("/bi/dag", frame));
            default:
                return ApiUtil.result(1500, "计算引擎异常", engine);
        }
    }

    /**
     * 取消正在运行的作业
     */
    public Map<String, Object> cancel(Integer jobId) {
        Job job = jobService.info(jobId);
        if (null == job) return ApiUtil.result(404, "作业记录不存在", jobId);
        if (jobService.isTerminal(job.getState())) return ApiUtil.result(1002, "作业已结束，无需取消", job.getState());
        Cluster cluster = clusterService.info(job.getClusterId());
        ObjectNode content = clusterNode(cluster, job.getEngine());
        Map<String, Object> result;
        if ("flink".equals(job.getEngine())) {
            result = RpcUtil.result(flinkRpc.post("/bi/job/cancel",
                    DPUtil.buildMap("content", content, "jobId", job.getJobId())));
        } else {
            result = RpcUtil.result(sparkRpc.post("/bi/job/cancel",
                    DPUtil.buildMap("content", content, "jobId", job.getJobId())));
        }
        if (ApiUtil.succeed(result)) {
            jobService.state(job, Job.STATE_CANCELLED, null, DPUtil.stringify(result.get("data")));
        }
        return result;
    }

    /**
     * 重新提交流程，用于人工重跑与自动恢复
     */
    public Map<String, Object> restart(Integer jobId, Map<?, ?> param, HttpServletRequest request, String triggerType) {
        Job job = jobService.info(jobId);
        if (null == job) return ApiUtil.result(404, "作业记录不存在", jobId);
        Map<String, Object> options = new HashMap<>();
        for (Map.Entry<?, ?> entry : param.entrySet()) {
            options.put(DPUtil.parseString(entry.getKey()), entry.getValue());
        }
        if (DPUtil.parseInt(options.get("clusterId")) < 1) options.put("clusterId", job.getClusterId());
        return run(job.getDiagramId(), options, request, triggerType);
    }

    private int rbacUid(HttpServletRequest request) {
        return null == request ? 0 : rbacService.uid(request);
    }

    public ObjectNode diagram(Integer id, ObjectNode cached) {
        if(null == id || id < 1) return null;
        String sid = String.valueOf(id);
        if (null != cached && cached.has(sid)) return (ObjectNode) cached.get(sid);
        Diagram diagram = JPAUtil.findById(diagramDao, id, Diagram.class);
        if (null == diagram || 1 != diagram.getStatus()) {
            if (null != cached) cached.replace(sid, null);
            return null;
        }
        ObjectNode result = DPUtil.objectNode();
        result.put("id", diagram.getId()).put("name", diagram.getName());
        result.put("engine", diagram.getEngine()).put("model", diagram.getModel());
        JsonNode content = DPUtil.parseJSON(diagram.getContent());
        if (null == content || !content.isObject()) content = DPUtil.objectNode();
        ArrayNode nodes = result.putArray("nodes");
        ArrayNode edges = result.putArray("edges");
        List<String> exportIds = new ArrayList<>();
        ObjectNode importIds = DPUtil.objectNode();
        ArrayNode jars = DPUtil.arrayNode();
        ArrayNode exports = DPUtil.arrayNode();
        Iterator<JsonNode> iterator = content.at("/cells").iterator();
        while (iterator.hasNext()) {
            JsonNode json = iterator.next();
            String shape = json.at("/shape").asText("");
            if ("flow-edge".equals(shape)) {
                String source = "dag_" + diagram.getId() + "_" + json.at("/source/cell").asText();
                String target = "dag_" + diagram.getId() + "_" + json.at("/target/cell").asText();
                if (exportIds.contains(target)) {
                    exports.add(target);
                    continue;
                }
                if (importIds.has(source)) {
                    Iterator<JsonNode> it = importIds.get(source).iterator();
                    while (it.hasNext()) {
                        edges.addObject().put("source", it.next().asText()).put("target", target);
                    }
                    continue;
                }
                edges.addObject().put("source", source).put("target", target);
                continue;
            }
            String nodeId = "dag_" + diagram.getId() + "_" + json.at("/id").asText();
            String type = json.at("/data/type").asText();
            JsonNode options = json.at("/data/options");
            if (!options.isObject()) options = DPUtil.objectNode();
            if ("GroupLayout".equals(type)) continue;
            if ("ExportConfig".equals(type)) {
                exportIds.add(nodeId);
                continue;
            }
            if ("ImportConfig".equals(type)) {
                ObjectNode sub = diagram(DPUtil.parseInt(options.at("/id").asText()), cached);
                importIds.replace(nodeId, null == sub ? DPUtil.arrayNode() : sub.at("/exports"));
                nodes.addAll((ArrayNode) sub.at("/nodes"));
                jars.addAll((ArrayNode) sub.at("/jars"));
                continue;
            }
            if ("ScriptTransform".equals(type)) {
                String jar = options.at("/jarURI").asText();
                if (!DPUtil.empty(jar)) jars.add(jar);
            }
            ObjectNode node = nodes.addObject();
            node.put("id", nodeId);
            node.put("type", type);
            String name = json.at("/data/name").asText();
            if (DPUtil.empty(name)) {
                name = String.format("data_%d_node_%d", diagram.getId(), json.at("/data/index").asInt());
            }
            node.put("name", name);
            node.put("parallelism", json.at("/data/parallelism").asInt());
            String parent = json.at("/parent").asText();
            node.put("parent", DPUtil.empty(parent) ? "" : ("dag_" + diagram.getId() + "_" + parent));
            if (type.endsWith("Source") || type.endsWith("Transform") || type.endsWith("Sink")) {
                node.put("kvConfigPrefix", json.at("/data/kvConfigPrefix").asText());
            }
            node.replace("options", options);
        }
        result.replace("exports", exports);
        result.replace("jars", jars);
        if (null != cached) cached.replace(sid, result);
        return result;
    }

}
