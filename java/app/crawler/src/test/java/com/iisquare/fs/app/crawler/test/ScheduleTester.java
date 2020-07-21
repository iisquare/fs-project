package com.iisquare.fs.app.crawler.test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.FileUtil;
import com.iisquare.fs.app.crawler.assist.Assist;
import com.iisquare.fs.app.crawler.assist.VerifyAssist;
import com.iisquare.fs.app.crawler.fetch.HttpFetcher;
import com.iisquare.fs.app.crawler.helper.XlabHelper;
import com.iisquare.fs.app.crawler.output.JDBCOutput;
import com.iisquare.fs.app.crawler.parse.JsoupParser;
import com.iisquare.fs.app.crawler.parse.Parser;
import com.iisquare.fs.app.crawler.schedule.Scheduler;
import com.iisquare.fs.app.crawler.schedule.Task;
import com.iisquare.fs.app.crawler.schedule.Template;
import com.iisquare.fs.app.crawler.schedule.Worker;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Test;

import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CyclicBarrier;

public class ScheduleTester {

    @Test
    public void proxyTest() throws IOException {
        HttpHost proxy = new HttpHost("123.122.157.53", 33128, "HTTP");
        RequestConfig config = RequestConfig.custom().setProxy(proxy).build();
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpGet get = new HttpGet("https://localhost/community/");
        get.setConfig(config);
        CloseableHttpResponse response = httpclient.execute(get);
        System.out.println(response);
        response.close();
    }

    @Test
    public void fetcherTest() throws IOException {
        HttpFetcher fetcher = new HttpFetcher();
        fetcher.headers(Template.defaultHeaders());
//        fetcher.charset("gb2312");
        fetcher.url("https://jxjump.58.com/service?target=FCADV8oV3os7xtAj_6pMK7rUlr8m2e7FXY32X6v-rH7bm2mLZpk1zEffDjpc6uQhhfOQnEtf3gzeNV-jJ5TvgYxPcOQYYBskL6cGfx_7k2_EqtKpcnMTll8_L--70lgTnUpplpM3b6VbDKBdWE-x667tWzurulw_dwCLVZtAUEnEhkw6JUkjq55FvfMJrHh6wPXgYxP_vAxXwMhjYOMT_AoJw0uQ79RoTEs7zZ9Bg38s5j8w2Cc3b92c-5m1ZBfktKv35&pubid=0&apptype=0&psid=063ccbc4-aad2-464d-a747-a1e5e073bc73&entinfo=38951774699149_0&cookie=%7C%7C%7C&fzbref=1&key=&params=busitime^desc&cookie=%7C%7C%7C&fzbref=1&key=").get();
        System.out.println(fetcher.getUrl());
        System.out.println(fetcher.getLastStatus());
        System.out.println(fetcher.getLastResult());
        Exception exception = fetcher.getLastException();
        if (null != exception) exception.printStackTrace();
        fetcher.close();
    }

    @Test
    public void slideTest() throws Exception {
        System.setProperty("webdriver.chrome.driver", "D:\\openservices\\selenium\\chromedriver.exe");
        XlabHelper.server = "http://127.0.0.1:8714";
        ObjectNode config = DPUtil.objectNode();
        config.put("url", "https://www.anjuke.com/captcha-verify/?callback=shield&from=antispam&serialID=4c61a8b6748bf9cae6b7f8fd8447af8f_c132dadca844491c94636e30d406694e&history=aHR0cHM6Ly9zaGFuZ2hhaS5hbmp1a2UuY29tL3NhbGUv");
        Assist assist = Assist.assist("verify", config);
        if (!assist.open()) {
            System.out.println("尝试打开失败");
        }
        try {
            System.out.println(assist.run());
        } finally {
            assist.close();
        }
    }

    @Test
    public void roadTest() throws Exception {
        System.setProperty("webdriver.chrome.driver", "D:\\openservices\\selenium\\chromedriver.exe");
        XlabHelper.server = "http://127.0.0.1:7805";
        ObjectNode config = DPUtil.objectNode();
        config.put("url", "https://www.anjuke.com/captcha-verify/?callback=shield&from=antispam&namespace=anjuke_c_pc&serialID=b9d53a5e2c63ae33dd607a7f3c176404_86e197c9e569488cb694d65f0a319d0f&history=aHR0cHM6Ly94aW5jaGVuZGljaGFuMTEuYW5qdWtlLmNvbS9nb25nc2ktampyLTczNzY3OTAvP2Zyb209ZXNmX2xpc3Rfc2tmeWZkZ2w%3D");
        config.put("type", "road").put("site", "58");
        Assist assist = Assist.assist("verify", config);
        if (!assist.open()) {
            System.out.println("尝试打开失败");
        }
        try {
            System.out.println(assist.run());
        } finally {
            assist.close();
        }
    }

    @Test
    public void scriptTest() throws ScriptException {
        ScriptEngineManager manager = new ScriptEngineManager();
        List<ScriptEngineFactory> factories = manager.getEngineFactories();
        for (ScriptEngineFactory factory: factories) {
            System.out.printf("Name: %s%n" +
                    "Version: %s%n" +
                    "Language name: %s%n" +
                    "Language version: %s%n" +
                    "Extensions: %s%n" +
                    "Mime types: %s%n" +
                    "Names: %s%n",
                factory.getEngineName(),
                factory.getEngineVersion(),
                factory.getLanguageName(),
                factory.getLanguageVersion(),
                factory.getExtensions(),
                factory.getMimeTypes(),
                factory.getNames());
        }
        String code = FileUtil.getContent(Worker.class.getClassLoader().getResource("crawler-script.js"), false, "UTF-8");
        String base64 = FileUtil.getContent(getClass().getClassLoader().getResource("ttf-base64.txt"), false, "UTF-8");
        code += "var decoder = Java.type(\"java.net.URLDecoder\");\n" +
                "var encoder = Java.type(\"java.net.URLEncoder\");\n" +
                "encoder.encode(decoder.decode(\"%d3%e0%b5%a4521\", \"gb2312\"), \"UTF-8\");\n" +
                "\n";
        System.out.println(code);
        Object result = manager.getEngineByName("js").eval(code);
        System.out.println("result: " + result);
    }

    @Test
    public void parserTest() throws Exception {
        String html = FileUtil.getContent(getClass().getClassLoader().getResource("spider-test.html"), false, "UTF-8");
        String expression = FileUtil.getContent(getClass().getClassLoader().getResource("expression-jsoup.txt"), false, "UTF-8");
        expression = expression.replaceFirst(JsoupParser.PROTOCOL, "");
        Parser parser = new JsoupParser().load(expression);
        System.out.println(parser.parse(html));
    }

    @Test
    public void mapperTest() throws Exception {
        String html = FileUtil.getContent(getClass().getClassLoader().getResource("spider-test.html"), false, "UTF-8");
        String expression = FileUtil.getContent(getClass().getClassLoader().getResource("expression-jsoup.txt"), false, "UTF-8");
        String code = FileUtil.getContent(getClass().getClassLoader().getResource("expression-mapper.txt"), false, "UTF-8");
        expression = expression.replaceFirst(JsoupParser.PROTOCOL, "");
        Parser parser = new JsoupParser().load(expression);
        JsonNode data = parser.parse(html);
        Scheduler scheduler = new Scheduler(1, 10, 1000, null, null);
        Task task = new Task();
        task.setUrl("safasf?xxxx");
        task.setParam(new LinkedHashMap<>());
        Worker worker = new Worker();
        Map<String, Object> context = worker.context(html, data, "OK", new Exception("xxxxx"), null);
        JsonNode result = worker.eval(context, code);
        System.out.println(result);
    }

    @Test
    public void jsoupTest() throws Exception {
        String html = FileUtil.getContent(getClass().getClassLoader().getResource("spider-test.html"), false, "UTF-8");
        Document document = Jsoup.parse(html);
        String result = document.select("script").eq(0).html();
        System.out.println(result);
    }

    @Test
    public void outputTest() throws Exception {
        ObjectNode parameters = DPUtil.objectNode();
        parameters.put("driver", "com.mysql.jdbc.Driver");
        parameters.put("url", "jdbc:mysql://127.0.0.1:3306/spider?characterEncoding=utf-8");
        parameters.put("username", "root");
        parameters.put("password", "admin888");
        parameters.put("table", "crawler_fetch");
        ObjectNode data = DPUtil.objectNode();
        data.put("url", "asf:fasf");
        JDBCOutput output = new JDBCOutput();
        output.configure(parameters);
        output.open();
        output.record(data);
        output.close();
    }

    @Test
    public void strTest() {
        System.out.println("\t\t\t   \r  \\n   \r\n  ".matches("^\\s*$"));
    }

    @Test
    public void zookeeperTest() throws Exception {
        System.out.println(" asf-asf asf-asf -fas ".replaceAll("\\s", ""));
    }

    @Test
    public void threadTest() throws Exception {
        System.setProperty("webdriver.chrome.driver", "D:\\openservices\\selenium\\chromedriver.exe");
        XlabHelper.server = "http://127.0.0.1:8714";
        VerifyAssist.headless = true;
        ObjectNode config = DPUtil.objectNode();
        config.put("url", "https://www.anjuke.com/captcha-verify/?callback=shield&from=antispam&serialID=4c61a8b6748bf9cae6b7f8fd8447af8f_c132dadca844491c94636e30d406694e&history=aHR0cHM6Ly9zaGFuZ2hhaS5hbmp1a2UuY29tL3NhbGUv");
        CyclicBarrier barrier = new CyclicBarrier(30);
        for (int i = 0; i < barrier.getParties(); i++) {
            int index = i;
            Thread.sleep(2000);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Assist assist = Assist.assist("verify", config);
                    try {
                        if (assist.open()) {
                            try {
                                System.out.println("index" + index + ":" + assist.run());
                            } catch (Exception e) {
                                e.printStackTrace();
                            } finally {
                                assist.close();
                            }
                        } else {
                            System.out.println("index" + index + ":" + "尝试打开失败");
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        barrier.await();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
        barrier.await();
        System.out.println("done");
    }

}
