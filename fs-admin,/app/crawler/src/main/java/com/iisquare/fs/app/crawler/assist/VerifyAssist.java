package com.iisquare.fs.app.crawler.assist;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.app.crawler.helper.XlabHelper;
import com.iisquare.fs.app.crawler.helper.XlabHelper;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.FileUtil;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 验证码校验
 * @see(https://github.com/SeleniumHQ/selenium/wiki/ChromeDriver)
 */
public class VerifyAssist extends Assist {

    public static boolean headless;
    public static String codeSlideAnjuke = FileUtil.getContent(
            VerifyAssist.class.getClassLoader().getResource("verify-slide-anjuke.txt"), false, StandardCharsets.UTF_8);
    protected final static Logger logger = LoggerFactory.getLogger(VerifyAssist.class);
    private static Hashtable<String, ReentrantLock> toggleLockTable = new Hashtable<>();
    private static Hashtable<String, AtomicInteger> toggleCounterTable = new Hashtable<>();
    private String type;
    private String site;
    private String url;
    private String proxy;
    private WebDriver driver;

    @Override
    public void configure(JsonNode parameters) {
        type = parameters.at("/type").asText("slide");
        site = parameters.at("/site").asText("anjuke");
        url = parameters.at("/url").asText("");
        proxy = parameters.at("/proxy").asText("");
    }

    private String key() {
        return type + "@" + site;
    }

    private ReentrantLock toggleLock() {
        String key = key();
        ReentrantLock toggle = toggleLockTable.get(key);
        if (null != toggle) return toggle;
        synchronized (VerifyAssist.class) {
            toggle = toggleLockTable.get(key);
            if (null == toggle) {
                toggle = new ReentrantLock();
                toggleLockTable.put(key, toggle);
            }
        }
        return toggle;
    }

    private AtomicInteger toggleCounter() {
        String key = key();
        AtomicInteger toggle = toggleCounterTable.get(key);
        if (null != toggle) return toggle;
        synchronized (VerifyAssist.class) {
            toggle = toggleCounterTable.get(key);
            if (null == toggle) {
                toggle = new AtomicInteger(0); // 同一时刻只有一个任务执行
                toggleCounterTable.put(key, toggle);
            }
        }
        return toggle;
    }

    @Override
    public boolean open() throws IOException {
        ReentrantLock toggleLock = toggleLock();
        boolean toggle = toggleCounter().compareAndSet(0, 1);
        toggleLock.lock();
        if (!toggle) {
            toggleLock.unlock();
            return false;
        }
        ChromeDriverService.Builder builder = new ChromeDriverService.Builder();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--disable-web-security"); // 忽略安全验证，解决图片跨域问题
        // 定义window.navigator.webdriver = 'undefined'，防止反爬
        options.setExperimentalOption("excludeSwitches", Arrays.asList("enable-automation"));
        if (!DPUtil.empty(proxy)) {
            options.addArguments("--proxy-server=" + proxy);
        }
        if (headless) {
            options.addArguments("--headless");
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-gpu");
            options.addArguments("--disable-dev-shm-usage");
        }
        driver = new ChromeDriver(builder.build(), options);
        driver.get(url);
        return true;
    }

    private String slide() throws InterruptedException {
        if (!"anjuke".equals(site)) return "目标站点暂不支持";
        WebElement slide;
        try {
            slide = driver.findElement(By.id("ISDCaptcha")).findElement(By.className("dvc-slider__handler"));
        } catch (Exception e) {
            return "未找到滑块" + e.getMessage();
        }
        JavascriptExecutor executor = (JavascriptExecutor) driver;
        String data = DPUtil.parseString(executor.executeScript(codeSlideAnjuke));
        logger.info("code take data: {}", data);
        JsonNode json = XlabHelper.verifySlide(data);
        if (null == json) return "访问XLab接口失败";
        logger.info("xlab slide data: {}", json);
        if (null == json || json.get("code").asInt() != 0 || json.get("data").size() == 0) {
            return "XLab未提取到有效缺口：" + json + "," + data;
        }
        json = json.get("data").get(0);
        long interval = json.get("interval").asLong();
        Actions action = new Actions(driver);
        action.moveToElement(slide, json.get("startX").asInt(), json.get("startY").asInt()).clickAndHold(slide).perform();
        Iterator<JsonNode> iterator = json.get("move").iterator();
        while (iterator.hasNext()) { // 移动
            JsonNode item = iterator.next();
            action.moveByOffset(item.get("x").asInt(), item.get("y").asInt()).perform();
            Thread.sleep(interval);
        }
        iterator = json.get("align").iterator();
        while (iterator.hasNext()) { // 对齐
            JsonNode item = iterator.next();
            action.moveByOffset(item.get("x").asInt(), item.get("y").asInt()).perform();
            Thread.sleep(interval);
        }
        action.moveByOffset(json.get("stopX").asInt(), json.get("stopY").asInt()).perform();
        Thread.sleep(500);
        action.release(slide).perform();
        Thread.sleep(1300);
        return driver.getTitle();
    }

    private String road() throws InterruptedException {
        if (!"58".equals(site)) return "目标站点暂不支持";
        try {
            WebElement btnSubmit = driver.findElement(By.id("btnSubmit"));
            btnSubmit.click();
        } catch (Exception e) {}
        Thread.sleep(500);
        WebElement canvas;
        try {
            canvas = driver.findElement(By.id("dvc-captcha__canvas"));
        } catch (Exception e) {
            return "未找到验证画布" + e.getMessage();
        }
        String base64 = canvas.getScreenshotAs(OutputType.BASE64);
        base64 = "data:image/jpeg;base64," + base64;
        String data = DPUtil.parseString(DPUtil.objectNode().put("image", base64).put("type", "58"));
        logger.info("screenshot take data: {}", data);
        JsonNode json = XlabHelper.verifyRoad(data);
        if (null == json || json.get("code").asInt() != 0 || json.get("data").size() == 0) {
            return "XLab未提取到有效路径：" + json + "," + data;
        }
        Actions action = new Actions(driver);
        Iterator<JsonNode> iterator = json.get("data").elements();
        if (!iterator.hasNext()) return "标记点数据异常";
        JsonNode item = iterator.next();
        action.moveToElement(canvas, item.get("x").asInt(), item.get("y").asInt()).clickAndHold().perform();
        while (iterator.hasNext()) {
            Thread.sleep(17);
            JsonNode next = iterator.next();
            int x = next.get("x").asInt() - item.get("x").asInt();
            int y = next.get("y").asInt() - item.get("y").asInt();
            action.moveByOffset(x, y).perform();
            item = next;
        }
        Thread.sleep(500);
        action.release().perform();
        Thread.sleep(1300);
        return driver.getTitle();
    }

    @Override
    public String run() throws Exception {
        switch (type) {
            case "slide":
                return slide();
            case "road":
                return road();
            default:
                return "目标类型暂不支持";
        }
    }

    @Override
    public void close() throws IOException {
        try {
            if (null != driver) {
                driver.quit(); // diver.close()只会关闭当前窗口，不退出驱动，在池化时可搭配使用
                driver = null;
            }
        } finally {
            toggleCounter().set(0);
            toggleLock().unlock();
        }
    }

}
