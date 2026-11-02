package com.iisquare.fs.web.spider;

import com.iisquare.fs.web.spider.core.PageTypeClassifier;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PageTypeClassifierTests {

    @Test
    public void listPageTest() {
        Document document = Jsoup.parse("""
                <html><head><title>新闻中心</title></head><body>
                <header><a href="/">首页</a><a href="/about">关于</a></header>
                <main>
                  <ul class="news-list">
                    <li><a href="/news/1">第一条新闻</a></li>
                    <li><a href="/news/2">第二条新闻</a></li>
                    <li><a href="/news/3">第三条新闻</a></li>
                    <li><a href="/news/4">第四条新闻</a></li>
                    <li><a href="/news/5">第五条新闻</a></li>
                  </ul>
                  <div class="pagination"><a rel="next" href="/news?page=2">下一页</a></div>
                </main>
                </body></html>
                """);
        assertEquals(PageTypeClassifier.LIST, PageTypeClassifier.classify(document));
    }

    @Test
    public void detailPageTest() {
        Document document = Jsoup.parse("""
                <html><head><title>政策解读</title></head><body>
                <nav><a href="/">首页</a><a href="/news">新闻</a></nav>
                <main>
                  <article>
                    <h1>关于推进数据要素市场化配置改革的政策解读</h1>
                    <time datetime="2026-10-30">2026-10-30</time>
                    <p>为深入贯彻落实数据要素市场化配置改革要求，进一步完善数据基础制度体系，推动数据资源高效流通和开发利用，结合工作实际制定本政策。</p>
                    <p>本政策围绕数据产权、流通交易、收益分配和安全治理等方面作出系统安排，明确了相关部门职责、重点任务和实施路径。</p>
                    <p>各地区各部门要加强统筹协调，细化工作措施，及时总结推广实践经验，确保各项任务落地见效。</p>
                  </article>
                </main>
                </body></html>
                """);
        assertEquals(PageTypeClassifier.DETAIL, PageTypeClassifier.classify(document));
    }

    @Test
    public void loginPageTest() {
        Document document = Jsoup.parse("""
                <html><head><title>用户登录</title></head><body>
                <form action="/login" method="post">
                  <input name="username">
                  <input name="password" type="password">
                  <input name="captcha">
                  <button>登录</button>
                </form>
                </body></html>
                """);
        assertEquals(PageTypeClassifier.OTHER, PageTypeClassifier.classify(document));
    }
}
