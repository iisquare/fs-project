package com.iisquare.fs.base.core.util;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class HttpUtil {

    private static final String DEFAULT_CHARSET = "UTF-8";

    public static String cookie(String... cookies) {
        Map<String, String> map = new LinkedHashMap<>();
        for (String cookie : cookies) {
            if (DPUtil.empty(cookie)) continue;
            String[] items = DPUtil.explode(cookie, ";");
            for (String item : items) {
                String[] kv = DPUtil.explode(item, "=");
                if (kv.length < 2) continue;
                map.put(kv[0], DPUtil.implode("=", kv, 1, kv.length));
            }
        }
        List<String> list = new ArrayList<>();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            list.add(entry.getKey() + "=" + entry.getValue());
        }
        return DPUtil.implode("; ", list.toArray(new String[list.size()]), 0, list.size());
    }

    public static String get(String url, Map<String, String> queryParas, String charset) {
        return get(url, queryParas, null, charset);
    }

    public static String get(String url, Map<String, String> queryParas) {
        return get(url, queryParas, null, DEFAULT_CHARSET);
    }

    public static String get(String url, String charset) {
        return get(url, null, null, charset);
    }

    public static String get(String url) {
        return get(url, null, null, DEFAULT_CHARSET);
    }

    public static String get(String url, Map<String, String> queryParas, Map<String, String> headers) {
        return get(url, queryParas, headers, DEFAULT_CHARSET);
    }

    public static String get(String url, Map<String, String> queryParas, Map<String, String> headers, String charset) {
        HttpURLConnection conn = null;
        try {
            //获取HttpURLConnection连接并设置参数
            conn = getHttpConnection(buildUrlWithQueryString(url, queryParas), "GET", headers);
            // 建立HttpURLConnection实际的连接
            conn.connect();
            //返回  定义BufferedReader输入流来读取URL的响应
            return readResponseString(conn, charset);
        } catch (Exception e) {
            return null;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    public static String post(String url, String data, Map<String, String> headers, String charset) {
        return post(url, null, data, headers, charset);
    }

    public static String post(String url, String data, Map<String, String> headers) {
        return post(url, null, data, headers, DEFAULT_CHARSET);
    }

    public static String post(String url, Map<String, String> queryParas, String data, Map<String, String> headers) {
        return post(url, queryParas, data, headers, DEFAULT_CHARSET);
    }

    public static String post(String url, Map<String, String> queryParas, String data, Map<String, String> headers, String charset) {
        HttpURLConnection conn = null;
        try {
            //获取HttpURLConnection连接并设置参数
            conn = getHttpConnection(buildUrlWithQueryString(url, queryParas), "POST", headers);
            // 建立HttpURLConnection实际的连接
            conn.connect();
            // 获取URLConnection对象对应的输出流
            OutputStream out = conn.getOutputStream();
            // 发送请求参数
            out.write(data.getBytes(charset));
            // flush输出流的缓冲
            out.flush();
            //关闭输出流
            out.close();
            //返回  定义BufferedReader输入流来读取URL的响应
            return readResponseString(conn, charset);
        } catch (Exception e) {
            return null;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private static HttpURLConnection getHttpConnection(String url, String method, Map<String, String> headers)
        throws IOException, NoSuchAlgorithmException, NoSuchProviderException, KeyManagementException {
        //将url转变为URL对象
        URL _url = new URL(url);
        //打开URL连接
        HttpURLConnection conn = (HttpURLConnection) _url.openConnection();
        //设置请求方式
        conn.setRequestMethod(method);
        // 发送POST请求必须设置如下两行
        conn.setDoOutput(true);
        conn.setDoInput(true);

        //连接时长
        conn.setConnectTimeout(1000);
        //读取时长
        conn.setReadTimeout(10000);

        // 设置通用的请求属性
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.3; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/33.0.1750.146 Safari/537.36");
        if (headers != null && !headers.isEmpty()) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                conn.setRequestProperty(entry.getKey(), entry.getValue());
            }
        }
        return conn;
    }

    private static String buildUrlWithQueryString(String url, Map<String, String> queryParas) throws UnsupportedEncodingException {
        if (queryParas == null || queryParas.isEmpty()) {
            return url;
        }
        StringBuilder sb = new StringBuilder(url);
        boolean isFirst;
        //判断URL是否有 ?
        if (url.indexOf("?") == -1) {
            isFirst = true;
            sb.append("?");
        } else {
            isFirst = false;
        }
        //遍历请求参数
        for (Map.Entry<String, String> entry : queryParas.entrySet()) {
            if (isFirst) {
                isFirst = false;
            } else {
                sb.append("&");
            }

            String key = entry.getKey();
            String value = entry.getValue();
            if (!DPUtil.empty(value)) {
                value = URLEncoder.encode(value, "utf-8"); // 对value进行转码
            }
            sb.append(key).append("=").append(value);
        }
        return sb.toString();
    }

    private static String readResponseString(HttpURLConnection conn, String charset) throws IOException {
        StringBuilder sb = new StringBuilder();
        InputStream inputStream = null;
        try {
            //获取响应内容
            inputStream = conn.getInputStream();
            // 定义BufferedReader输入流来读取URL的响应
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, charset));
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                }
            }
        }
    }

}
