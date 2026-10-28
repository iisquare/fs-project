package com.iisquare.fs.web.agent.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.agent.ai.ToolParam;
import com.iisquare.fs.web.agent.service.AIService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.Map;

/**
 * 网页 (web) 服务工具：联网搜索网页，以及抓取单个网页的 Markdown 正文。
 *
 * 两个方法都转调模型网关的网页工具接口（{@code /v1/tools/search}、{@code /v1/tools/fetch}），
 * 鉴权、模型授权与按次计费沿用 {@link com.iisquare.fs.web.agent.service.BotService} 的同一口径：
 * - 密钥：服务级密钥 {@code fs.agent.token}（Authorization: Bearer）；
 * - 会话：调用端必须带登录会话（{@code x-auth-token} 或 Authorization: Bearer），
 *   网关据此把用量记到该用户名下；没有会话或会话无效时工具直接报错；
 * - 模型：网关里类型分别为「网页搜索」「网页抓取」的模型别名，取
 *   {@code fs.agent.web.searchModel} / {@code fs.agent.web.fetchModel}，也可由调用方在参数里指定。
 *
 * 搜索与抓取的上游是 DeepSeek Search / Fetch API：
 * 搜索返回结构化结果（标题、链接、摘要、可选的正文），抓取返回单个网页的 Markdown 正文。
 * 返回给模型前会限制单条 / 整篇正文的长度，避免一次工具调用把上下文占满。
 */
@Service
public class WebTool {

    private static final Logger logger = LoggerFactory.getLogger(WebTool.class);

    /** DeepSeek Search 的 query / ori_question 字符上限 */
    private static final int MAX_QUERY_LENGTH = 128;
    /** DeepSeek Search 的 count 上限 */
    private static final int MAX_COUNT = 50;
    /** DeepSeek Fetch 的 URL 字符上限 */
    private static final int MAX_URL_LENGTH = 8192;
    /** 搜索默认返回条数：与 DeepSeek Search 的接口默认值一致 */
    private static final int DEFAULT_COUNT = 10;

    @Autowired
    AIService aiService;
    @Autowired
    ToolSessionSupport toolSessionSupport;
    /** 默认网页搜索模型：调用方没在参数里给出 model 时用它 */
    @Value("${fs.agent.web.searchModel:deepseek-search}")
    private String searchModel;
    /** 默认网页抓取模型：调用方没在参数里给出 model 时用它 */
    @Value("${fs.agent.web.fetchModel:deepseek-fetch}")
    private String fetchModel;
    /** 抓取单篇网页返回的正文上限（字符），超出部分截断并附说明 */
    @Value("${fs.agent.web.maxContent:30000}")
    private int maxContent;
    /** 搜索单条结果 content 的上限（字符）：need_content=true 时才有内容 */
    @Value("${fs.agent.web.maxResultContent:8000}")
    private int maxResultContent;

    @Tool(name = "webSearch",
            description = "联网搜索网页，返回结构化搜索结果（标题、链接、摘要，按需带正文）。"
                    + "需要获取实时信息、新闻、资料入口或不确定具体网址时先用它，"
                    + "再按结果里的 url 用 webFetch 读取网页正文。"
                    + "返回 JSON：{results:[{title,url,snippet,summary,content,...}]}，无结果时 results 为空数组。"
                    + "query 支持 site:、lang:、after:、before:、\"精确短语\"、-排除词 等搜索运算符。")
    public String webSearch(
            @ToolParam(name = "query", description = "搜索关键词，1-128 个字符；"
                    + "支持 site:example.com、lang:zh,en、after:2025-01-01、\"精确短语\"、-排除词 等运算符")
            String query,
            @ToolParam(name = "count", description = "返回条数，1-50，默认 10", required = false)
            Integer count,
            @ToolParam(name = "need_content", description = "是否在每条结果里返回网页正文 content，默认 false；"
                    + "通常先不取正文，挑中链接后再用 webFetch", required = false)
            Boolean need_content,
            @ToolParam(name = "ori_question", description = "触发本次搜索的用户原问题（1-128 个字符），"
                    + "用于改善查询理解；不确定时可省略", required = false)
            String ori_question,
            @ToolParam(name = "localization", description = "用于本地化搜索结果的 IP 地址，通常留空", required = false)
            String localization,
            @ToolParam(name = "model", description = "网页搜索模型别名，留空用服务配置值（默认 deepseek-search）",
                    required = false)
            String model) {
        String session = toolSessionSupport.requireSession();
        String text = text(query);
        checkQuery(text, "query");
        int limit = null == count ? DEFAULT_COUNT : count;
        if (limit < 1 || limit > MAX_COUNT) {
            throw new IllegalStateException("count 必须在 1-" + MAX_COUNT + " 之间：" + limit);
        }
        String current = model(model, searchModel, "网页搜索");
        ObjectNode body = DPUtil.objectNode();
        body.put("model", current);
        body.put("query", text);
        body.put("count", limit);
        body.put("need_content", Boolean.TRUE.equals(need_content));
        String question = text(ori_question);
        if (!question.isEmpty()) {
            checkQuery(question, "ori_question");
            body.put("ori_question", question);
        }
        String location = text(localization);
        if (!location.isEmpty()) body.put("localization", location);
        JsonNode payload = invoke(aiService.webSearch(body, session), "网页搜索");
        logger.info("Web search: model={} queryLength={} count={} need_content={}",
                current, text.length(), limit, Boolean.TRUE.equals(need_content));
        return searchResult(payload);
    }

    @Tool(name = "webFetch",
            description = "抓取单个网页并以 Markdown 返回正文，适合在 webSearch 找到链接后读取详情、"
                    + "核对原文或提取页面中的具体内容。"
                    + "返回内容依次为网页标题、来源地址与 Markdown 正文；"
                    + "正文过长时按服务配置截断，并在末尾说明原文总长度。"
                    + "url 必须是带主机名的 http/https 地址，不接受本机路径、file:// 或带用户名密码的地址。")
    public String webFetch(
            @ToolParam(name = "url", description = "要抓取的网页地址，必须是带主机名的 http/https URL")
            String url,
            @ToolParam(name = "model", description = "网页抓取模型别名，留空用服务配置值（默认 deepseek-fetch）",
                    required = false)
            String model) {
        String session = toolSessionSupport.requireSession();
        String target = normalizeUrl(url);
        String current = model(model, fetchModel, "网页抓取");
        ObjectNode body = DPUtil.objectNode();
        body.put("model", current);
        body.put("url", target);
        JsonNode payload = invoke(aiService.webFetch(body, session), "网页抓取");
        String title = payload.at("/title").asText("");
        String content = payload.at("/content").asText("");
        if (DPUtil.empty(content)) throw new IllegalStateException("网页抓取未返回正文：" + target);
        int total = content.length();
        boolean truncated = maxContent > 0 && total > maxContent;
        StringBuilder result = new StringBuilder();
        if (!DPUtil.empty(title)) result.append("# ").append(title).append("\n\n");
        result.append("来源：").append(target).append("\n\n");
        result.append(truncated ? clip(content, maxContent, "网页正文") : content);
        logger.info("Web fetch: model={} host={} length={} truncated={}",
                current, URI.create(target).getHost(), total, truncated);
        return result.toString();
    }

    /** 搜索响应 → 给模型的 JSON：去掉图标等无关字段，限制单条 content 的长度 */
    protected String searchResult(JsonNode payload) {
        JsonNode results = payload.at("/results");
        if (!results.isArray()) return DPUtil.stringify(payload);
        ArrayNode rows = DPUtil.arrayNode();
        for (JsonNode item : results) {
            if (!item.isObject()) continue;
            ObjectNode row = item.deepCopy();
            row.remove("site_icon");
            JsonNode content = row.get("content");
            if (null != content && content.isTextual()
                    && maxResultContent > 0 && content.asText().length() > maxResultContent) {
                row.put("content", clip(content.asText(), maxResultContent, "搜索结果的网页正文"));
                row.put("contentTruncated", true);
            }
            rows.add(row);
        }
        ObjectNode result = DPUtil.objectNode();
        result.set("results", rows);
        if (rows.isEmpty()) {
            result.put("message", "未找到相关网页，可换一组关键词或放宽 site: / after: 等限定后重试");
        }
        return DPUtil.stringify(result);
    }

    /**
     * 结果信封 → 业务报文：网关非 200 时 AIService 把上游原始错误报文放在 data 里，
     * 这里把状态、原因与上游错误一起交给模型，让它知道是重试、换关键词还是改配置。
     */
    protected JsonNode invoke(Map<String, Object> result, String action) {
        if (ApiUtil.failed(result)) {
            Object data = result.get("data");
            String detail = data instanceof JsonNode
                    ? DPUtil.stringify((JsonNode) data) : DPUtil.parseString(data);
            String reason = DPUtil.parseString(result.get("message"));
            throw new IllegalStateException(action + "失败：" + reason
                    + (DPUtil.empty(detail) ? "" : " " + clip(detail, 2000, "错误详情")));
        }
        JsonNode payload = ApiUtil.data(result, JsonNode.class);
        if (null == payload) throw new IllegalStateException(action + "未返回内容");
        if (payload.has("error")) {
            throw new IllegalStateException(action + "失败："
                    + payload.at("/error/message").asText(DPUtil.stringify(payload.at("/error"))));
        }
        return payload;
    }

    /** 模型别名：参数优先，其次服务配置；两者都没有时给出明确指路 */
    protected String model(String model, String configured, String action) {
        String current = text(DPUtil.empty(model) ? configured : model);
        if (current.isEmpty()) {
            throw new IllegalStateException("未指定" + action + "模型：请在方法参数 model 里给出，"
                    + "或配置 fs.agent.web." + ("网页搜索".equals(action) ? "searchModel" : "fetchModel"));
        }
        return current;
    }

    protected void checkQuery(String query, String name) {
        if (null == query || query.isEmpty()) throw new IllegalStateException(name + " 不能为空");
        int length = query.codePointCount(0, query.length());
        if (length > MAX_QUERY_LENGTH) {
            throw new IllegalStateException(name + " 不能超过 " + MAX_QUERY_LENGTH + " 个字符：" + length);
        }
    }

    /**
     * 抓取地址校验：只接受带主机名的 http/https 地址，且不能携带用户名 / 密码。
     * 访问策略（内网地址、robots 等）由网关与上游抓取服务判断。
     */
    protected String normalizeUrl(String value) {
        String url = text(value);
        if (url.isEmpty()) throw new IllegalStateException("url 不能为空");
        if (url.length() > MAX_URL_LENGTH) {
            throw new IllegalStateException("url 不能超过 " + MAX_URL_LENGTH + " 个字符");
        }
        URI uri;
        try {
            uri = new URI(url);
        } catch (Exception e) {
            throw new IllegalStateException("url 格式不正确：" + cut(url) + "（" + e.getMessage() + "）");
        }
        String scheme = uri.getScheme();
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            throw new IllegalStateException("url 只支持 http/https：" + cut(url)
                    + "（本机路径、file:// 等地址无法抓取）");
        }
        if (DPUtil.empty(uri.getHost())) {
            throw new IllegalStateException("url 必须包含主机名：" + cut(url));
        }
        if (null != uri.getUserInfo()) {
            throw new IllegalStateException("url 不能携带用户名或密码：" + cut(url));
        }
        return uri.toString();
    }

    /** 按字符上限截断并附说明：保留长度信息，模型可据此判断是否需要换页面或缩小范围 */
    protected String clip(String value, int limit, String label) {
        String text = DPUtil.parseString(value);
        if (limit <= 0 || text.length() <= limit) return text;
        int end = limit;
        if (end > 0 && Character.isHighSurrogate(text.charAt(end - 1))) end -= 1;
        return text.substring(0, end) + "\n\n...[" + label + "已截断，原文共 " + text.length() + " 字符]";
    }

    /** 错误文案里的值截断：地址可能很长，只展示开头便于定位 */
    protected String cut(String value) {
        String text = DPUtil.parseString(value);
        return text.length() <= 120 ? text : text.substring(0, 120) + "...";
    }

    /** 去掉首尾所有空白（含换行与制表符）；null 按空串处理 */
    protected String text(String value) {
        return null == value ? "" : value.strip();
    }

}
