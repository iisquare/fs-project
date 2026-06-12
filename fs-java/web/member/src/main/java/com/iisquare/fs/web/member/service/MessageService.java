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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Service
public class MessageService extends JPAServiceBase {

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

    public Message record(Message message) {
        message.setCreatedTime(System.currentTimeMillis());
        return messageDao.save(message);
    }

    public ObjectNode search(Map<String, Object> param, Map<?, ?> args) {
        ObjectNode result = search(messageDao, param, (Specification<Message>) (root, query, cb) -> {
            SpecificationHelper<Message> helper = SpecificationHelper.newInstance(root, cb, param);
            helper.dateFormat(configuration.getFormatDate()).equalWithIntGTZero("id").withoutDeleted();
            helper.equal("type").equal("recipient").like("subject").equal("status");
            helper.like("content").like("exception").betweenWithDate("createdTime");
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
        Message.MessageBuilder message = Message.builder().exception("");
        message.type("email").recipient(email).subject(subject).content(html);
        try {
            emailService.send(email, subject, html, files);
            message.status("success");
            return ApiUtil.result(0, null, email);
        } catch (Exception e) {
            message.status("error").exception(ApiUtil.getStackTrace(e));
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

}
