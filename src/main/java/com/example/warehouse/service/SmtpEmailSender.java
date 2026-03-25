package com.example.warehouse.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
@ConditionalOnProperty(prefix = "wms.communication.email", name = "enabled", havingValue = "true")
public class SmtpEmailSender implements EmailSender {

    private final JavaMailSender javaMailSender;
    private final EmailProperties emailProperties;

    public SmtpEmailSender(JavaMailSender javaMailSender, EmailProperties emailProperties) {
        this.javaMailSender = javaMailSender;
        this.emailProperties = emailProperties;
    }

    @Override
    public void send(String recipient, String subject, String textBody, String htmlBody) {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();

        try {
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.name());
            helper.setTo(recipient);
            helper.setFrom(emailProperties.getFrom());
            helper.setSubject(subject == null ? "" : subject);

            String safeText = textBody == null ? "" : textBody;
            if (htmlBody == null || htmlBody.isBlank()) {
                helper.setText(safeText, false);
            } else {
                helper.setText(safeText, htmlBody);
            }
            javaMailSender.send(mimeMessage);
        } catch (MessagingException ex) {
            throw new IllegalStateException("Failed to compose SMTP email message", ex);
        }
    }
}
