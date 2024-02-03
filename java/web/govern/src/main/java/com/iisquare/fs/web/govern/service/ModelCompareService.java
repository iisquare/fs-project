package com.iisquare.fs.web.govern.service;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.FileUtil;
import com.iisquare.fs.base.jpa.util.JDBCUtil;
import com.iisquare.fs.base.web.mvc.ServiceBase;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class ModelCompareService extends ServiceBase {

    public Map<String, Object> jdbc(Map<?, ?> param) {

        String url = DPUtil.parseString(param.get("url"));
        if (DPUtil.empty(url)) return ApiUtil.result(1001, "连接串不能为空", url);
        String username = DPUtil.parseString(param.get("username"));
        String password = DPUtil.parseString(param.get("password"));
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            return ApiUtil.result(5001, "加载驱动失败", e.getMessage());
        }
        Connection connection = null;
        try {
            connection = DriverManager.getConnection(url, username, password);
            ObjectNode tables = JDBCUtil.tables(connection);
            return ApiUtil.result(0, null, tables);
        } catch (SQLException e) {
            return ApiUtil.result(5005, "获取信息失败", e.getMessage());
        } finally {
            JdbcUtils.closeConnection(connection);
        }
    }

    public Map<String, Object> csv(MultipartFile file, Map<?, ?> param) {
        String glue = DPUtil.parseString(param.get("glue"));
        if (glue.length() < 1) return ApiUtil.result(1001, "请配置分隔符", glue);
        String charset = DPUtil.parseString(param.get("charset"));
        if (DPUtil.empty(charset)) return ApiUtil.result(1002, "请配置编码格式", charset);
        if (null == file) return ApiUtil.result(1003, "获取文件句柄失败", null);
        InputStream stream = null;
        InputStreamReader reader = null;
        BufferedReader buffer = null;
        try {
            stream = file.getInputStream();
            reader = new InputStreamReader(stream, charset);
            buffer = new BufferedReader(reader);
            String line = buffer.readLine();
            // code,name,type,size,digit,nullable,description
            String[] strings = DPUtil.explode(glue, line);
            int length = strings.length;
            if (length != 7) return ApiUtil.result(1005, "表头字段数量不匹配", line);
            Map<String, Integer> columns = new LinkedHashMap<>();
            for (int i = 0; i < strings.length; i++) {
                columns.put(strings[i], i);
            }
            int index = 0;
            ObjectNode result = DPUtil.objectNode();
            while ((line = buffer.readLine()) != null) {
                index++;
                strings = DPUtil.explode(glue, line, null, false);
                if (strings.length != length) {
                    return ApiUtil.result(1006, "第" + index + "行数据异常", line);
                }
                ObjectNode item = DPUtil.objectNode();
                item.put("code", DPUtil.trim(strings[columns.get("code")]));
                item.put("name", DPUtil.trim(strings[columns.get("name")]));
                item.put("type", DPUtil.trim(strings[columns.get("type")]));
                item.put("size", DPUtil.parseInt(strings[columns.get("size")]));
                item.put("digit", DPUtil.parseInt(strings[columns.get("digit")]));
                item.put("nullable", DPUtil.parseInt(strings[columns.get("nullable")]) != 0);
                item.put("description", strings[columns.get("description")]);
                result.replace(item.get("code").asText(), item);
            }
            if (result.size() != index) {
                return ApiUtil.result(1007, "存在重复字段编码", result.size());
            }
            return ApiUtil.result(0, null, result);
        } catch (Exception e) {
            return ApiUtil.result(5005, "获取文件失败", e.getMessage());
        } finally {
            FileUtil.close(stream, reader, buffer);
        }
    }

}
