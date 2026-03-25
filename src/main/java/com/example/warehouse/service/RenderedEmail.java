package com.example.warehouse.service;

public record RenderedEmail(String subject, String textBody, String htmlBody) {

    public RenderedEmail {
        subject = subject == null ? "" : subject;
        textBody = textBody == null ? "" : textBody;
        htmlBody = htmlBody == null ? "" : htmlBody;
    }
}
