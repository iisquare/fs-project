package com.iisquare.fs.web.member.service;

import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.FileUtil;
import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import jakarta.activation.FileDataSource;
import jakarta.mail.*;
import jakarta.mail.internet.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;

@Service
public class EmailService {

    @Value("${fs.member.email.host}")
    String emailHost;
    @Value("${fs.member.email.port}")
    String emailPort;
    @Value("${fs.member.email.username}")
    String emailUsername;
    @Value("${fs.member.email.password}")
    String emailPassword;
    private static volatile Session session = null;
    public static final String HTML_SIGNUP;
    public static final String HTML_FORGOT;

    static {
        try {
            HTML_SIGNUP = FileUtil.getContent(
                    new ClassPathResource("/email/signup.html").getFile(), false, StandardCharsets.UTF_8);
            HTML_FORGOT = FileUtil.getContent(
                    new ClassPathResource("/email/forgot.html").getFile(), false, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Session session() {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", emailHost);
        props.put("mail.smtp.port", emailPort);
        return Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(emailUsername, emailPassword);
            }
        });
    }

    public void send(String email, String subject, String html, File... files) throws Exception {
        if (null == session) {
            synchronized (this) {
                if (null == session) {
                    session = session();
                }
            }
        }
        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(emailUsername));
        message.setRecipient(Message.RecipientType.TO, new InternetAddress(email));
        message.setSubject(subject, "UTF-8");
        // 创建消息部分（HTML正文）
        MimeBodyPart messageBodyPart = new MimeBodyPart();
        messageBodyPart.setContent(html, "text/html;charset=UTF-8");
        // 创建多重消息
        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(messageBodyPart);
        // 附件部分
        for (File file : files) {
            MimeBodyPart attachmentBodyPart = new MimeBodyPart();
            DataSource source = new FileDataSource(file);
            attachmentBodyPart.setDataHandler(new DataHandler(source));
            attachmentBodyPart.setFileName(file.getName());
            multipart.addBodyPart(attachmentBodyPart);
        }
        // 设置完整消息
        message.setContent(multipart);
        Transport.send(message);
    }

    public Map<String, Object> signup(String email, String code) {
        String subject = "平方域用户注册验证码";
        String html = HTML_SIGNUP.replace("{code}", code);
        try {
            send(email, subject, html);
            return ApiUtil.result(0, "邮箱验证码发送成功，请查收", email);
        } catch (Exception e) {
            return ApiUtil.result(17501, "邮箱验证码发送失败", e.getMessage());
        }
    }

    public Map<String, Object> forgot(String email, String code) {
        String subject = "平方域重置密码验证码";
        String html = HTML_FORGOT.replace("{code}", code);
        try {
            send(email, subject, html);
            return ApiUtil.result(0, "邮箱验证码发送成功，请查收", email);
        } catch (Exception e) {
            return ApiUtil.result(17501, "邮箱验证码发送失败", e.getMessage());
        }
    }

}
