package com.example.warehouse.service;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class EmailRecipientPolicy {

    private final EmailProperties emailProperties;

    public EmailRecipientPolicy(EmailProperties emailProperties) {
        this.emailProperties = emailProperties;
    }

    public boolean shouldSuppress(String recipient) {
        if (recipient == null || recipient.isBlank()) {
            return false;
        }
        int atIndex = recipient.lastIndexOf('@');
        if (atIndex < 0 || atIndex == recipient.length() - 1) {
            return false;
        }

        String domain = recipient.substring(atIndex + 1).trim().toLowerCase(Locale.ROOT);
        if (domain.isEmpty()) {
            return false;
        }

        List<String> blockedSuffixes = emailProperties.getBlockedRecipientSuffixes();
        if (blockedSuffixes == null || blockedSuffixes.isEmpty()) {
            return false;
        }

        for (String suffix : blockedSuffixes) {
            if (suffix == null) {
                continue;
            }
            String normalized = suffix.trim().toLowerCase(Locale.ROOT);
            if (normalized.isEmpty()) {
                continue;
            }
            String requiredSuffix = normalized.startsWith(".") ? normalized : "." + normalized;
            if (domain.endsWith(requiredSuffix)) {
                return true;
            }
        }
        return false;
    }
}
