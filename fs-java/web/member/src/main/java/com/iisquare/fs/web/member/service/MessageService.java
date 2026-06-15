package com.iisquare.fs.web.member.service;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.FileUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.jpa.helper.SpecificationHelper;
import com.iisquare.fs.base.jpa.mvc.JPAServiceBase;
import com.iisquare.fs.web.member.dao.MessageDao;
import com.iisquare.fs.web.member.entity.Message;
import com.iisquare.fs.web.member.mvc.Configuration;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Service
public class MessageService extends JPAServiceBase implements DisposableBean {

    @Autowired
    MessageDao messageDao;
    @Autowired
    EmailService emailService;
    @Autowired
    private RbacService rbacService;
    @Autowired
    private Configuration configuration;
    public static final String HTML_SIGNUP;
    public static final String HTML_FORGOT;
    CloseableHttpClient client;
    final RequestConfig config;

    static {
        try (InputStream input = new ClassPathResource("/email/signup.html").getInputStream()) {
            HTML_SIGNUP = FileUtil.getContent(input, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try (InputStream input = new ClassPathResource("/email/forgot.html").getInputStream()) {
            HTML_FORGOT = FileUtil.getContent(input, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public MessageService() {
        PoolingHttpClientConnectionManager pooling = new PoolingHttpClientConnectionManager();
        pooling.setMaxTotal(200); // 最大连接数
        pooling.setDefaultMaxPerRoute(100); // 默认的每个路由的最大连接数
        HttpClientBuilder builder = HttpClientBuilder.create();
        builder.setConnectionManager(pooling);
        client = builder.build();
        config = RequestConfig.custom()
                .setSocketTimeout(25000)
                .setConnectTimeout(1000)
                .setConnectionRequestTimeout(3000)
                .build();
    }

    public String post(String url, String params, Map<String, String> headers) throws Exception {
        HttpPost http = new HttpPost(url);
        if (headers != null && !headers.isEmpty()) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                http.addHeader(entry.getKey(), entry.getValue());
            }
        }
        StringEntity entity = new StringEntity(params, StandardCharsets.UTF_8);
        entity.setContentType("application/json");
        http.setEntity(entity);
        return this.execute(http);
    }

    public String execute(HttpRequestBase http) throws Exception {
        CloseableHttpResponse response = null;
        try {
            http.setConfig(this.config);
            response = client.execute(http);
            HttpEntity entity = response.getEntity();
            return EntityUtils.toString(entity, StandardCharsets.UTF_8);
        } finally {
            FileUtil.close(response);
        }
    }

    @Override
    public void destroy() throws Exception {
        FileUtil.close(client);
    }

    public Message record(Message message) {
        message.setCreatedTime(System.currentTimeMillis());
        return messageDao.save(message);
    }

    public ObjectNode search(Map<String, Object> param, Map<?, ?> args) {
        ObjectNode result = search(messageDao, param, (Specification<Message>) (root, query, cb) -> {
            SpecificationHelper<Message> helper = SpecificationHelper.newInstance(root, cb, param);
            helper.dateFormat(configuration.getFormatDate()).equalWithIntGTZero("id").withoutDeleted();
            helper.equal("type").equal("recipient").like("subject").equal("status");
            helper.like("requestBody").like("responseBody").betweenWithDate("createdTime");
            return cb.and(helper.predicates());
        }, Sort.by(Sort.Order.desc("createdTime")), "id", "status", "createdTime");
        return result;
    }

    public boolean delete(List<Integer> ids, HttpServletRequest request) {
        return delete(messageDao, ids, rbacService.uid(request));
    }

    public Map<String, Object> email(Map<String, ?> param, MultipartHttpServletRequest request) {
        String email = DPUtil.parseString(param.get("email"));
        String subject =  DPUtil.parseString(param.get("subject"));
        String html =  DPUtil.parseString(param.get("html"));
        MimeBodyPart[] files;
        try {
            files = EmailService.parts(request.getFileMap().values().toArray(new MultipartFile[0]));
        } catch (Exception e) {
            return ApiUtil.result(17101, "解析附件失败", e.getMessage());
        }
        return email(email, subject, html, files);
    }

    public Map<String, Object> email(String email, String subject, String html, MimeBodyPart... files) {
        if (!ValidateUtil.isEmail(email)) {
            return ApiUtil.result(17001, "邮件地址不合法", email);
        }
        if (DPUtil.empty(subject)) {
            return ApiUtil.result(17002, "主题不能为空", subject);
        }
        if (DPUtil.empty(html)) {
            return ApiUtil.result(17003, "邮件内容不能为空", html);
        }
        Message.MessageBuilder message = Message.builder().responseBody("");
        message.type("email").recipient(email).subject(subject).requestBody(html);
        try {
            emailService.send(email, subject, html, files);
            message.status("success");
            return ApiUtil.result(0, null, email);
        } catch (Exception e) {
            message.status("error").responseBody(ApiUtil.getStackTrace(e));
            return ApiUtil.result(17500, "发送失败", e.getMessage());
        } finally {
            record(message.build());
        }
    }

    public Map<String, Object> signup(String email, String code) {
        String subject = "平方域用户注册验证码";
        String html = HTML_SIGNUP.replace("{code}", code);
        Map<String, Object> result = email(email, subject, html);
        if (ApiUtil.failed(result)) {
            result.put(ApiUtil.FIELD_MSG, "邮箱验证码发送失败");
        } else {
            result.put(ApiUtil.FIELD_MSG, "邮箱验证码发送成功，请查收");
        }
        return result;
    }

    public Map<String, Object> forgot(String email, String code) {
        String subject = "平方域重置密码验证码";
        String html = HTML_FORGOT.replace("{code}", code);
        Map<String, Object> result = email(email, subject, html);
        if (ApiUtil.failed(result)) {
            result.put(ApiUtil.FIELD_MSG, "邮箱验证码发送失败");
        } else {
            result.put(ApiUtil.FIELD_MSG, "邮箱验证码发送成功，请查收");
        }
        return result;
    }

    /**
     * 发送钉钉群消息
     * @param recipient 对应access_token
     * @param subject 时间主题，仅用于内部运维标识
     * @param content 消息内容
     */
    public Map<String, Object> dingtalk(String recipient, String subject, String content) {
        if (DPUtil.empty(recipient)) {
            return ApiUtil.result(17101, "认证密钥不能为空", recipient);
        }
        if (DPUtil.empty(content)) {
            return ApiUtil.result(17102, "消息内容不能为空", content);
        }
        String url = "https://oapi.dingtalk.com/robot/send?access_token=" + recipient;
        Message.MessageBuilder message = Message.builder().responseBody("");
        message.type("dingtalk").recipient(recipient).subject(subject).requestBody(content);
        try {
            String body = post(url, content, null);
            message.status("success").responseBody(body);
            return ApiUtil.result(0, null, body);
        } catch (Exception e) {
            message.status("error").responseBody(ApiUtil.getStackTrace(e));
            return ApiUtil.result(17500, "发送失败", e.getMessage());
        } finally {
            record(message.build());
        }
    }

    public Map<String, Object> wecom(String key, String msgtype, String content) {
        if (DPUtil.empty(key)) {
            return ApiUtil.result(17101, "密钥不能为空", key);
        }
        if (DPUtil.empty(content)) {
            return ApiUtil.result(17102, "消息内容不能为空", content);
        }
        String url = "https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=" + key;
        Message.MessageBuilder message = Message.builder().requestBody("");
        message.type("wecom").recipient(key).subject(msgtype).requestBody(content);
        try {
            String body = post(url, content, null);
            message.status("success").responseBody(body);
            return ApiUtil.result(0, null, body);
        } catch (Exception e) {
            message.status("error").responseBody(ApiUtil.getStackTrace(e));
            return ApiUtil.result(17500, "发送失败", e.getMessage());
        } finally {
            record(message.build());
        }
    }

}
