package com.iisquare.fs.app.crawler;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.FileUtil;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.After;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.*;

public class ZhipinTests {

    CloseableHttpClient client;
    final RequestConfig config;

    public ZhipinTests() {
        client = HttpClients.custom().build();
        config = RequestConfig.custom()
                .setSocketTimeout(1000)
                .setConnectTimeout(3000)
                .setConnectionRequestTimeout(15000)
                .build();
    }

    @After
    public void close() {
        FileUtil.close(client);
    }

    public String get(String url, Map<String, String> headers) {
        HttpGet http = new HttpGet(url);
        if (headers != null && headers.size() > 0) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                http.addHeader(entry.getKey(), entry.getValue());
            }
        }
        return this.request(http);
    }

    public String post(String url, String params, Map<String, String> headers) {
        HttpPost http = new HttpPost(url);
        if (headers != null && headers.size() > 0) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                http.addHeader(entry.getKey(), entry.getValue());
            }
        }
        StringEntity entity = new StringEntity(params, StandardCharsets.UTF_8);
        entity.setContentType("application/json");
        http.setEntity(entity);
        return this.request(http);
    }

    public String request(HttpRequestBase http) {
        CloseableHttpResponse response = null;
        try {
            http.setConfig(this.config);
            response = client.execute(http);
            HttpEntity entity = response.getEntity();
            return EntityUtils.toString(entity, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        } finally {
            FileUtil.close(response);
        }
    }

    @Test
    public void salaryxcTest() {
        int checkpoint = 0;
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7");
        headers.put("accept-language", "zh-CN,zh;q=0.9,en;q=0.8");
        headers.put("cache-control", "no-cache");
        headers.put("cookie", "");
        headers.put("pragma", "no-cache");
        headers.put("priority", "u=0, i");
        headers.put("sec-ch-ua", "\"Chromium\";v=\"128\", \"Not;A=Brand\";v=\"24\", \"Google Chrome\";v=\"128\"");
        headers.put("sec-ch-ua-mobile", "?0");
        headers.put("sec-ch-ua-platform", "\"Windows\"");
        headers.put("sec-fetch-dest", "document");
        headers.put("sec-fetch-mode", "navigate");
        headers.put("sec-fetch-site", "same-origin");
        headers.put("sec-fetch-user", "?1");
        headers.put("upgrade-insecure-requests", "1");
        headers.put("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36");
        String output = "zhipin-position.csv";
        String separator = "|";
        List<String> title = Arrays.asList(
                "P1", "P2", "P3", "样本数", "平均月薪", "链接",
                "1年以内", "1-3年", "3-5年", "5-10年", "10年以上",
                "24岁以下", "25岁-29岁", "30岁-34岁", "35岁-39岁", "40岁-44岁", "45岁以上"
        );
        if (checkpoint <= 0) {
            FileUtil.putContent(output, DPUtil.implode(separator, title) + "\n", true, false, StandardCharsets.UTF_8);
        }
        String content = FileUtil.getContent(getClass().getClassLoader().getResource("zhipin-position.json"), false, StandardCharsets.UTF_8);
        JsonNode json = DPUtil.parseJSON(content);
        for (JsonNode l1 : json.at("/zpData")) {
            String p1 = l1.at("/name").asText();
            for (JsonNode l2 : l1.at("/subLevelModelList")) {
                String p2 = l2.at("/name").asText();
                for (JsonNode l3 : l2.at("/subLevelModelList")) {
                    List<String> data = new ArrayList<>();
                    data.add(p1);
                    data.add(p2);
                    data.add(l3.at("/name").asText());
                    int code = l3.at("/code").asInt();
                    if (checkpoint > 0 && code != checkpoint) {
                        continue;
                    } else {
                        checkpoint = 0;
                    }
                    String url = String.format("https://www.zhipin.com/salaryxc/c101120100_p%d.html", code);
                    String html;
                    try {
                        html = get(url, headers);
                    } catch (Exception e) {
                        System.out.println(url);
                        throw new RuntimeException("请求失败", e);
                    }
                    try {
                        Document document = Jsoup.parse(html);
                        data.add(document.getElementsByClass("example-num").text());
                        data.add(document.getElementsByClass("salary-num").text());
                        data.add(url);
                        Elements charts = document.getElementsByClass("salary-chart-average").get(0).getElementsByClass("chart-header");
                        List<String> texts = new ArrayList<>();
                        for (Element chart : charts) {
                            Element tips = chart.getElementsByClass("tips-text").get(0);
                            if (tips.children().size() < 2) break;
                            Elements elements = tips.child(1).children();
                            for (Element element : elements) {
                                texts.add(element.text());
                            }
                        }
                        for (int i = 6; i < title.size(); i++) {
                            String t = title.get(i);
                            String text = "";
                            for (String s : texts) {
                                if (s.startsWith(t)) {
                                    text = s;
                                    break;
                                }
                            }
                            data.add(text);
                        }
                        FileUtil.putContent(output, DPUtil.implode(separator, data) + "\n", false, true, StandardCharsets.UTF_8);
                        Thread.sleep(3000);
                    } catch (Exception e) {
                        System.out.println(url);
                        System.out.println(html);
                        throw new RuntimeException("解析失败", e);
                    }
                }
            }
        }
    }

}
