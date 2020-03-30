package com.iisquare.fs.app.crawler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.FileUtil;
import com.iisquare.fs.base.core.util.HttpUtil;
import com.iisquare.fs.app.crawler.assist.VerifyAssist;
import com.iisquare.fs.app.crawler.helper.XlabHelper;
import com.iisquare.fs.app.crawler.web.Configuration;
import com.iisquare.fs.app.crawler.web.HttpHandler;
import com.iisquare.fs.app.crawler.web.HttpServer;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.Base64;

public class CrawlerApplication {

    protected final static Logger logger = LoggerFactory.getLogger(CrawlerApplication.class);

    public static JsonNode config(String[] args) throws ParseException, FileNotFoundException, UnsupportedEncodingException {
        String filepath = "conf/crawler.yml"; // -c=app/crawler/conf/crawler.yml
        CommandLineParser parser = new DefaultParser();
        Options options = new Options();
        options.addOption("h", "help", false, "print this usage information");
        options.addOption("c", "conf", true, "set configuration file (default \"" + filepath + "\")");
        CommandLine commandLine = parser.parse(options, args);
        if(commandLine.hasOption("help")) {
            new HelpFormatter().printHelp("java -jar crawler-{version}.jar -c /path/to/config/yml", options);
            System.exit(0);
        }
        if(commandLine.hasOption("conf")) {
            filepath = commandLine.getOptionValue("conf");
        }
        Yaml yaml = new Yaml();
        ObjectNode config = (ObjectNode) DPUtil.convertJSON(yaml.load(new FileInputStream(new File(filepath))));
        if(config.has("kvConsulUrl")) {
            String content = HttpUtil.get(config.get("kvConsulUrl").asText());
            if (null == content) {
                throw new FileNotFoundException("can not get config from kvConsulUrl");
            }
            JsonNode kv = DPUtil.convertJSON(yaml.load(content));
            String kvContent = new String(Base64.getDecoder().decode(kv.get(0).get("Value").asText()), "UTF-8");
            config.setAll((ObjectNode) DPUtil.convertJSON(yaml.load(kvContent)));
        }
        return config;
    }

    public static void shutdown(Closeable... args) {
        FileUtil.close(args);
        System.out.println("Bye Bye!");
    }

    public static void prepare(JsonNode config) {
        String webDriver = config.at("/webdriver/chrome/driver").asText("");
        System.setProperty("webdriver.chrome.driver", webDriver);
        XlabHelper.server = config.at("/xlab/server").asText("");
        VerifyAssist.headless = config.at("/webdriver/chrome/headless").asBoolean(false);
    }

    public static void main(String[] args) {
        JsonNode config = null;
        try {
            config = config(args);
        } catch (Exception e) {
            logger.error("failed to load config", e);
            System.exit(2);
        }
        prepare(config);
        Configuration configuration = Configuration.getInstance();
        configuration.config(config);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> shutdown(configuration)));
        String host =configuration.serverHost();
        int port = configuration.serverPort();
        HttpServer server = new HttpServer();
        logger.info("routes on {}", HttpHandler.routes());
        System.out.println("server start on " + host + ":" + port + "...");
        try {
            server.start(host, port);
        } catch (InterruptedException e) {
            logger.error("server interrupted", e);
        }
    }

}
