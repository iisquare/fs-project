package com.iisquare.etl.spark.flow;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.apache.spark.SparkConf;
import org.apache.spark.sql.SparkSession;

import com.iisquare.etl.spark.flow.Node;
import com.iisquare.jwframe.utils.DPUtil;

public class TaskRunner {

	private JobService jobService;
	
	public TaskRunner(JobService jobService) {
		this.jobService = jobService;
	}
	
	public boolean process(Map<String, Node> nodeMap) {
		// 查找入度为零的全部节点
		List<Node> list = new ArrayList<>();
		for (Entry<String, Node> entry : nodeMap.entrySet()) {
			Node node = entry.getValue();
			if(node.isReady()) continue;
			Set<Node> set = node.getSource();
			boolean sourceReady = true;
			for (Node source : set) {
				if(source.isReady()) continue;
				sourceReady = false;
				break;
			}
			if(sourceReady) {
				list.add(node);
			}
		}
		// 执行任务
		for (Node node : list) {
			String nodeId = node.getId();
			jobService.nodeStart(nodeId);
			Object result = node.process();
			jobService.nodeEnd(nodeId, null == result ? "" : result.toString());
			if(null != result) return false;
			node.setReady(true);
		}
		return !list.isEmpty();
	}
	
	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		Map<String, Object> dataMap = DPUtil.parseJSON(args[0], Map.class);
		JobService jobService = new JobService(dataMap);
		Map<?, ?> flow = DPUtil.parseJSON(DPUtil.parseString(dataMap.get("flowContent")), Map.class);
		SparkConf sparkConf = new SparkConf();
		SparkSession session = SparkSession.builder().config(sparkConf).getOrCreate();
		jobService.updateApplicationId(session.sparkContext().applicationId());
		// 解析节点
		Map<?, ?> nodes = (Map<?, ?>) flow.get("nodes");
		Map<String, Node> nodeMap = new LinkedHashMap<>();
		for (Object obj1 : nodes.values()) {
			Map<?, ?> item = (Map<?, ?>) obj1;
			List<?> property = (List<?>) item.get("property");
			Properties properties = new Properties();
			for (Object obj2 : property) {
				Map<?, ?> prop = (Map<?, ?>) obj2;
				properties.setProperty(prop.get("key").toString(), prop.get("value").toString());
			}
			String id = DPUtil.parseString(item.get("id"));
			Node node = (Node) Class.forName(item.get("parent").toString()).newInstance();
			node.setSparkConf(sparkConf);
			node.setId(id);
			node.setProperties(properties);
			nodeMap.put(id, node);
		}
		// 解析连线
		List<?> connections = (List<?>) flow.get("connections");
		for (Object obj1 : connections) {
			Map<?, ?> connection = (Map<?, ?>) obj1;
			Node source = nodeMap.get(connection.get("sourceId").toString());
			Node target = nodeMap.get(connection.get("targetId").toString());
			source.getTarget().add(target);
			target.getSource().add(source);
		}
		// 查找入度为零的节点并执行
		TaskRunner taskRunner = new TaskRunner(jobService);
		while(taskRunner.process(nodeMap)) {}
		session.close();
		jobService.update("complete");
	}

}
