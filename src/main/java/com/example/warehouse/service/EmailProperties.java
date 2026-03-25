package com.example.warehouse.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "wms.communication.email")
public class EmailProperties {

    private boolean enabled = false;
    private String from = "no-reply@warehouse.local";
    private List<String> blockedRecipientSuffixes = new ArrayList<>(List.of(".test", ".local"));

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public List<String> getBlockedRecipientSuffixes() {
        return blockedRecipientSuffixes;
    }

    public void setBlockedRecipientSuffixes(List<String> blockedRecipientSuffixes) {
        this.blockedRecipientSuffixes = blockedRecipientSuffixes == null
                ? new ArrayList<>()
                : new ArrayList<>(blockedRecipientSuffixes);
    }
}
