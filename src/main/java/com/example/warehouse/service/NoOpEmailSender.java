package com.example.warehouse.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "wms.communication.email", name = "enabled", havingValue = "false", matchIfMissing = true)
public class NoOpEmailSender implements EmailSender {

    private static final Logger logger = LoggerFactory.getLogger(NoOpEmailSender.class);

    @Override
    public void send(String recipient, String subject, String textBody, String htmlBody) {
        logger.info("Email dispatch skipped because SMTP is disabled recipient={} subject={} textLength={} htmlLength={}",
                recipient,
                subject,
                textBody == null ? 0 : textBody.length(),
                htmlBody == null ? 0 : htmlBody.length());
    }
}
