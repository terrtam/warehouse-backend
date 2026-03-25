package com.example.warehouse.service;

import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SmtpEmailSenderTest {

    @Mock
    private JavaMailSender javaMailSender;

    private SmtpEmailSender smtpEmailSender;

    @BeforeEach
    void setUp() {
        EmailProperties emailProperties = new EmailProperties();
        emailProperties.setFrom("no-reply@warehouse.local");
        smtpEmailSender = new SmtpEmailSender(javaMailSender, emailProperties);
    }

    @Test
    void sendBuildsMultipartMessageWithConfiguredFromAddress() throws Exception {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        smtpEmailSender.send(
                "ops@warehouse.com",
                "Subject",
                "Plain body",
                "<p><strong>HTML body</strong></p>"
        );

        ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(javaMailSender).send(messageCaptor.capture());

        MimeMessage sent = messageCaptor.getValue();
        assertEquals("Subject", sent.getSubject());
        assertEquals("ops@warehouse.com", ((InternetAddress) sent.getAllRecipients()[0]).getAddress());
        assertEquals("no-reply@warehouse.local", ((InternetAddress) sent.getFrom()[0]).getAddress());
        assertTrue(sent.getContent() != null);
    }
}
