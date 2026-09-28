package com.iisquare.fs.web.agent.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.web.util.RpcUtil;
import com.iisquare.fs.web.core.rpc.FileRpc;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * 文件服务取件工具：
 * /file/download 返回的是带时效签名的下载地址（JSON 信封），并非文件字节流，
 * 因此需先取出地址，再请求该地址取回文件内容。
 */
public class FileFetcher {

    /** 下载连接超时，单位毫秒 */
    private static final int CONNECT_TIMEOUT = 10000;
    /** 下载读取超时，单位毫秒 */
    private static final int READ_TIMEOUT = 300000;

    /**
     * 获取带时效签名的下载地址，失败时抛出便于排查的异常
     */
    public static String url(FileRpc fileRpc, String id, String name) {
        JsonNode json = RpcUtil.json(fileRpc.get("/file/download", DPUtil.buildMap("id", id)));
        String url = null == json ? "" : json.at("/data").asText("");
        if (null == json || 0 != json.at("/code").asInt() || DPUtil.empty(url)) {
            String message = null == json ? "" : json.at("/message").asText("");
            throw new IllegalStateException("获取文件下载地址失败：" + name + "（" + id + "）" + message);
        }
        return url;
    }

    /**
     * 按下载地址取回文件字节
     */
    public static byte[] bytes(String url, String name) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        try {
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(CONNECT_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);
            int status = connection.getResponseCode();
            if (200 != status) throw new IllegalStateException("下载文件失败：" + status + " " + name);
            try (InputStream stream = connection.getInputStream()) {
                return stream.readAllBytes();
            }
        } finally {
            connection.disconnect();
        }
    }

    /**
     * 下载地址形如 /file/{id}{suffix}?time=..&expire=..&token=..，取末段作为文件名
     */
    public static String filename(String url) {
        if (DPUtil.empty(url)) return "";
        String path = url.split("\\?")[0];
        return path.substring(path.lastIndexOf("/") + 1);
    }

}
