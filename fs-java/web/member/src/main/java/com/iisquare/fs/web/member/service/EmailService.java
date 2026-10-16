package com.iisquare.fs.web.member.service;

import com.iisquare.fs.base.core.util.DPUtil;
import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import jakarta.activation.FileDataSource;
import jakarta.mail.*;
import jakarta.mail.internet.*;
import jakarta.mail.util.ByteArrayDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.*;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    /**
     * 邮件服务参数均提供默认值：未配置时不影响项目启动
     */
    @Value("${fs.member.email.host:}")
    String emailHost;
    @Value("${fs.member.email.port:587}")
    String emailPort;
    @Value("${fs.member.email.username:}")
    String emailUsername;
    @Value("${fs.member.email.password:}")
    String emailPassword;
    private static volatile Session session = null;

    /**
     * 邮件服务是否已配置：host、username、password 任一为空即视为未启用，
     * 发送时跳过实际投递，仅在日志中记录，消息状态标记为 skipped
     */
    public boolean enabled() {
        return !DPUtil.empty(emailHost) && !DPUtil.empty(emailUsername) && !DPUtil.empty(emailPassword);
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

    public static MimeBodyPart[] parts(File... files) throws Exception {
        MimeBodyPart[] parts = new MimeBodyPart[files.length];
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            MimeBodyPart attachmentBodyPart = new MimeBodyPart();
            DataSource source = new FileDataSource(file);
            attachmentBodyPart.setDataHandler(new DataHandler(source));
            attachmentBodyPart.setFileName(file.getName());
            parts[i] = attachmentBodyPart;
        }
        return parts;
    }

    public static MimeBodyPart[] parts(MultipartFile... files) throws Exception {
        MimeBodyPart[] parts = new MimeBodyPart[files.length];
        for (int i = 0; i < files.length; i++) {
            MultipartFile file = files[i];
            MimeBodyPart attachmentBodyPart = new MimeBodyPart();
            ByteArrayDataSource source = new ByteArrayDataSource(file.getBytes(), file.getContentType());
            attachmentBodyPart.setDataHandler(new DataHandler(source));
            attachmentBodyPart.setFileName(file.getOriginalFilename());
            parts[i] = attachmentBodyPart;
        }
        return parts;
    }

    /**
     * 发送邮件
     *
     * @return true 表示已实际投递；false 表示邮件服务未配置，已跳过实际投递且仅记录日志
     */
    public boolean send(String email, String subject, String html, MimeBodyPart... files) throws Exception {
        if (!enabled()) {
            log.warn("邮件服务未配置(fs.member.email.host/username/password)，仅记录日志，不实际发送邮件；"
                            + "收件人：{}，主题：{}，附件数：{}，邮件内容：{}",
                    email, subject, null == files ? 0 : files.length, html);
            return false;
        }
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
        for (MimeBodyPart file : files) {
            multipart.addBodyPart(file);
        }
        // 设置完整消息
        message.setContent(multipart);
        Transport.send(message);
        return true;
    }

}
