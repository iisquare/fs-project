package com.iisquare.etl.test.spark;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import com.iisquare.etl.spark.config.Configuration;
import com.iisquare.etl.spark.flow.Node;
import com.iisquare.jwframe.utils.DPUtil;
import com.iisquare.jwframe.utils.FileUtil;

public class Tester {

	public String loadJSON() {
		return FileUtil.getContent("src/main/webapp/WEB-INF/template/frontend/flow/test.json");
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
		// 执行任务（可尝试采用线程池并发执行）
		for (Node node : list) {
			JavaRDD<?> result = null;
			try {
				result = node.process();
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
			node.setResult(result);
			node.setReady(true);
		}
		return !list.isEmpty();
	}
	
	public static void main(String[] args) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		Tester tester = new Tester();
		Configuration config = Configuration.getInstance();
		SparkConf sparkConf = config.getSparkConf();
		JSONObject flow = DPUtil.parseJSON(tester.loadJSON());
		// 解析节点
		JSONObject nodes = flow.getJSONObject("nodes");
		Map<String, Node> nodeMap = new LinkedHashMap<>();
		for (Object obj1 : nodes.values()) {
			JSONObject item = (JSONObject) obj1;
			JSONArray property = item.getJSONArray("property");
			Properties properties = new Properties();
			for (Object obj2 : property) {
				JSONObject prop = (JSONObject) obj2;
				properties.setProperty(prop.getString("key"), prop.getString("value"));
			}
			Node node = (Node) Class.forName(item.getString("parent")).newInstance();
			node.setSparkConf(sparkConf);
			node.setProperties(properties);
			nodeMap.put(item.getString("id"), node);
		}
		// 解析连线
		JSONArray connections = flow.getJSONArray("connections");
		for (Object obj1 : connections) {
			JSONObject connection = (JSONObject) obj1;
			Node source = nodeMap.get(connection.getString("sourceId"));
			Node target = nodeMap.get(connection.getString("targetId"));
			source.getTarget().add(target);
			target.getSource().add(source);
		}
		// 查找入度为零的节点并执行
		while(tester.process(nodeMap)) {}
	}

}
