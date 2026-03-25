package com.example.warehouse.service;

public interface EmailSender {

    void send(String recipient, String subject, String textBody, String htmlBody);
}
