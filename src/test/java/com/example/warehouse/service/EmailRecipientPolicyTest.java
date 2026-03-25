package com.example.warehouse.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmailRecipientPolicyTest {

    @Test
    void shouldSuppressBlockedSuffixesCaseInsensitively() {
        EmailProperties properties = new EmailProperties();
        properties.setBlockedRecipientSuffixes(List.of(".test", ".local"));

        EmailRecipientPolicy policy = new EmailRecipientPolicy(properties);

        assertTrue(policy.shouldSuppress("buyer@acmeretail.test"));
        assertTrue(policy.shouldSuppress("buyer@legacy.LOCAL"));
    }

    @Test
    void shouldAllowUnblockedRecipients() {
        EmailProperties properties = new EmailProperties();
        properties.setBlockedRecipientSuffixes(List.of(".test", ".local"));

        EmailRecipientPolicy policy = new EmailRecipientPolicy(properties);

        assertFalse(policy.shouldSuppress("ops@warehouse.com"));
        assertFalse(policy.shouldSuppress("invalid-recipient"));
    }
}
