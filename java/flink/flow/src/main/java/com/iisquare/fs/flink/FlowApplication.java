package com.iisquare.fs.flink;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.flink.flow.Event;
import com.iisquare.fs.flink.flow.Runner;
import com.iisquare.fs.base.core.util.DPUtil;

import java.io.*;
import java.net.URL;
import java.util.Base64;

public class FlowApplication {

    public static String readFromUri(String uri) throws IOException {
        if(uri.startsWith("http")) return readFromUrl(uri);
        if (uri.matches("^([A-Za-z0-9+/]{4})*([A-Za-z0-9+/]{4}|[A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{2}==)$")) {
            return new String(Base64.getDecoder().decode(uri));
        }
        return uri;
    }

    public static String readFromUrl(String url) throws IOException {
        InputStream inputStream = null;
        InputStreamReader inputReader = null;
        BufferedReader bufferReader = null;
        String output = "";
        try {
            inputStream = new URL(url).openStream();
            inputReader = new InputStreamReader(inputStream);
            bufferReader = new BufferedReader(inputReader);
            StringBuilder sb = new StringBuilder();
            String text;
            while ((text = bufferReader.readLine()) != null) {
                sb.append(text);
            }
            int length = sb.length();
            output = sb.toString();
        } catch (IOException ioException) {
            throw ioException;
        } finally {
            close(bufferReader, inputReader, inputStream);
        }
        return output;
    }

    public static void close(Closeable...args) {
        try {
            for (Closeable arg : args) {
                if(null != arg) arg.close();
            }
        } catch (Exception e) {}
    }

    public static void main(String[] args) throws Exception {
        JsonNode flow = DPUtil.parseJSON(readFromUri(args[0]));
        Runner runner = new Runner(new Event() {});
        runner.execute(flow.has("appname") ? flow.get("appname").asText() : FlowApplication.class.getSimpleName(), flow);
    }

}
