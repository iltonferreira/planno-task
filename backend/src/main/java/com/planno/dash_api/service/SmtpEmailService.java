package com.planno.dash_api.service;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
public class SmtpEmailService implements EmailService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SmtpEmailService.class);

    private final JavaMailSender mailSender;
    private final EmailTemplateService templateService;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${app.mail.from:no-reply@planno.local}")
    private String from;

    public SmtpEmailService(JavaMailSender mailSender, EmailTemplateService templateService) {
        this.mailSender = mailSender;
        this.templateService = templateService;
    }

    @Override
    @Async
    public void sendAsync(String to, String subject, String templateName, Map<String, String> parameters) {
        if (!mailEnabled || to == null || to.isBlank()) {
            LOGGER.info("Mail skipped for template {} because mail is disabled or recipient is empty.", templateName);
            return;
        }

        try {
            String html = templateService.render(templateName, parameters);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
        } catch (Exception exception) {
            LOGGER.error("Error while sending email {}", templateName, exception);
        }
    }
}
