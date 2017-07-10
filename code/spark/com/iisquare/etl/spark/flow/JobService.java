package com.iisquare.etl.spark.flow;

import java.io.Closeable;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;
import com.iisquare.jwframe.utils.DPUtil;
import com.iisquare.jwframe.utils.PropertiesUtil;

public class JobService implements Closeable {

	private Connection conn = null;
	private Map<String, Object> dataMap;
	private int flowId;
	private String tablePrefix;
	private Logger logger = Logger.getLogger(getClass().getName());
	
	public JobService(Map<String, Object> dataMap) {
		this.dataMap = dataMap;
	}
	
	public Map<String, Object> getDataMap() {
		return dataMap;
	}

	public boolean init(boolean reload) {
		flowId = DPUtil.parseInt(dataMap.get("flowId"));
		if(flowId < 1) return false;
		if(!reload) return true;
		try {
			Statement stmt = getConnection().createStatement();
			String sql = "select * from " + tablePrefix + "flow where id = " + flowId + " limit 1";
			ResultSet rs = stmt.executeQuery(sql);
			if(rs.next()) {
				dataMap.put("flowContent", rs.getString("content"));
				dataMap.put("flowStatus", rs.getInt("status"));
			}
			rs.close();
			stmt.close();
			if(!dataMap.containsKey("content")) return false;
		} catch (SQLException e) {
			return false;
		}
		
		return true;
	}
	
	public boolean nodeStart(String nodeId) {
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("job_id", dataMap.get("flowId"));
		data.put("node_id", dataMap.get("flowContent"));
		data.put("content", "");
		data.put("start_time", System.currentTimeMillis());
		String sql = "insert into " + tablePrefix + "job_node ("
				+ DPUtil.implode(",", DPUtil.collectionToStringArray(data.keySet())) + ") values ("
				+ DPUtil.implode(",", DPUtil.getFillArray(data.size(), "?")) + ")";
		try {
			PreparedStatement statement = getConnection().prepareStatement(sql);
			bindPendingParams(statement, data);
			boolean result = statement.execute();
			statement.close();
			return result;
		} catch (SQLException e) {
			return false;
		}
	}
	
	public boolean nodeEnd(String nodeId, String content) {
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("content", content);
		data.put("end_time", System.currentTimeMillis());
		data.put("job_id", DPUtil.parseInt(dataMap.get("jobId")));
		data.put("node_id", nodeId);
		String sql = "update " + tablePrefix + "job_node set content=?, end_time=? where job_id=? and node_id=? limit 1";
		try {
			PreparedStatement statement = getConnection().prepareStatement(sql);
			bindPendingParams(statement, data);
			boolean result = statement.execute();
			statement.close();
			return result;
		} catch (SQLException e) {
			return false;
		}
	}
	
	private void bindPendingParams(PreparedStatement statement, Map<String, Object> data) throws SQLException {
		List<Object> list = Arrays.asList(DPUtil.collectionToArray(data.values()));
		int size = list.size();
		for (int index = 0; index < size;) {
			Object param = list.get(index++);
			if (null == param) {
				statement.setObject(index, param);
			} else if (param instanceof String) {
				statement.setString(index, param.toString());
			} else if (param instanceof Date) {
				statement.setDate(index, Date.valueOf(param.toString()));
			} else if (param instanceof Boolean) {
				statement.setBoolean(index, (Boolean) (param));
			} else if (param instanceof Integer) {
				statement.setInt(index, (Integer) param);
			} else if (param instanceof Float) {
				statement.setFloat(index, (Float) param);
			} else if (param instanceof Double) {
				statement.setDouble(index, (Double) param);
			} else {
				statement.setObject(index, param);
			}
		}
	}
	
	public boolean update(String step) {
		int status = 0;
		switch (step) {
		case "dispatch":status = 2;break;
		case "dispatched":status = 5;break;
		case "complete":status = 6;break;
		default:return false;
		}
		try {
			Statement statement = getConnection().createStatement();
			String sql = "update " + tablePrefix + "job set status=" + status + ", "
					+ step + "_time=" + System.currentTimeMillis() + " where id="
					+ DPUtil.parseInt(dataMap.get("jobId")) + " limit 1";
			boolean result = statement.execute(sql);
			statement.close();
			return result;
		} catch (SQLException e) {
			return false;
		}
	}
	
	public boolean record() {
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("flow_id", dataMap.get("flowId"));
		data.put("flow_content", dataMap.get("flowContent"));
		data.put("status", 1);
		data.put("trigger_time", System.currentTimeMillis());
		String sql = "insert into " + tablePrefix + "job ("
				+ DPUtil.implode(",", DPUtil.collectionToStringArray(data.keySet())) + ") values ("
				+ DPUtil.implode(",", DPUtil.getFillArray(data.size(), "?")) + ")";
		try {
			PreparedStatement statement = getConnection().prepareStatement(sql);
			bindPendingParams(statement, data);
			if(statement.execute()) {
				ResultSet keys = statement.executeQuery("SELECT last_insert_id();");
				if (keys.next()) {
					dataMap.put("jobId", ((Number) keys.getObject(1)).intValue());
				}
				keys.close();
			}
			statement.close();
			if(!dataMap.containsKey("jobId")) return false;
		} catch (SQLException e) {
			return false;
		}
		return true;
	}
	
	public Connection getConnection() {
		if(null != conn) {
			try {
				if(conn.isValid(3)) {
					return conn;
				} else {
					conn.close();
				}
			} catch (SQLException e) {}
		}
		try {
			Class.forName("com.mysql.jdbc.Driver");
		} catch (ClassNotFoundException e) {
			logger.error("load mysql driver failed", e);
			return null;
		}
		Properties prop = PropertiesUtil.load(getClass().getClassLoader(), "jdbc.properties");
		String url = "jdbc:mysql://" + prop.getProperty("mysql.master", "127.0.0.1:3306") + "/"
				+ prop.getProperty("mysql.dbname") + "?characterEncoding=" + prop.getProperty("mysql.charset", "utf8");
		try {
			conn = DriverManager.getConnection(url, prop.getProperty("mysql.username", "root"), prop.getProperty("mysql.password", "root"));
			tablePrefix = prop.getProperty("mysql.tablePrefix", "");
		} catch (SQLException e) {
			logger.error("get mysql connection", e);
			conn = null;
		}
		return conn;
	}

	@Override
	public void close() {
		if(null == conn) return;
		try {
			conn.close();
		} catch (SQLException e) {}
	}
	
}
