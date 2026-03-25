package com.example.warehouse.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

class EmailTemplateServiceTest {

    private final EmailTemplateService service = new EmailTemplateService();

    @Test
    void buildSalesOrderConfirmedIncludesStyledHtmlAndOrderId() {
        UUID orderId = UUID.randomUUID();
        List<EmailTemplateService.EmailLineItem> lineItems = List.of(
                new EmailTemplateService.EmailLineItem("Product A", "SKU-A", 3, new BigDecimal("10.50"), new BigDecimal("31.50")),
                new EmailTemplateService.EmailLineItem("Product B", "SKU-B", 7, new BigDecimal("5.00"), new BigDecimal("35.00"))
        );

        RenderedEmail email = service.buildSalesOrderConfirmed(
                orderId,
                "Acme Retail Group",
                "10 Market St, San Francisco, CA",
                LocalDate.of(2026, 3, 2),
                lineItems
        );

        assertTrue(email.subject().contains(orderId.toString()));
        assertTrue(email.textBody().contains(orderId.toString()));
        assertTrue(email.htmlBody().contains(orderId.toString()));
        assertTrue(email.htmlBody().contains("Warehouse Operations"));
        assertTrue(email.htmlBody().contains("Quantity"));
        assertTrue(email.htmlBody().contains("Product A"));
        assertTrue(email.htmlBody().contains("SKU-A"));
        assertTrue(email.htmlBody().contains("$10.50"));
        assertTrue(email.htmlBody().contains("$31.50"));
        assertTrue(email.htmlBody().contains("Total Cost"));
        assertTrue(email.htmlBody().contains("$66.50"));
        assertTrue(email.htmlBody().contains("Acme Retail Group"));
        assertTrue(email.htmlBody().contains("10 Market St"));
        assertTrue(email.textBody().contains("Product B (SKU-B)"));
        assertTrue(email.textBody().contains("Subtotal: $35.00"));
        assertTrue(email.textBody().contains("Total Cost: $66.50"));
        assertTrue(email.textBody().contains("Customer: Acme Retail Group"));
        assertTrue(email.textBody().contains("Expected Delivery Date: 2026-03-02"));
    }

    @Test
    void buildSalesOrderShippedIncludesStyledHtmlAndOrderId() {
        UUID orderId = UUID.randomUUID();
        List<EmailTemplateService.EmailLineItem> lineItems = List.of(
                new EmailTemplateService.EmailLineItem("Product C", "SKU-C", 2, new BigDecimal("8.25"), new BigDecimal("16.50"))
        );

        RenderedEmail email = service.buildSalesOrderShipped(
                orderId,
                "Bluebird Stores",
                "501 Pine Ave, Seattle, WA",
                LocalDate.of(2026, 3, 4),
                lineItems
        );

        assertTrue(email.subject().contains(orderId.toString()));
        assertTrue(email.textBody().contains(orderId.toString()));
        assertTrue(email.htmlBody().contains(orderId.toString()));
        assertTrue(email.htmlBody().contains("Action tracked in Warehouse"));
        assertTrue(email.htmlBody().contains("Product C"));
        assertTrue(email.htmlBody().contains("SKU-C"));
        assertTrue(email.htmlBody().contains("$16.50"));
        assertTrue(email.textBody().contains("Total Cost: $16.50"));
        assertTrue(email.textBody().contains("Address: 501 Pine Ave, Seattle, WA"));
        assertTrue(email.htmlBody().contains("Expected Delivery: 2026-03-04"));
    }

    @Test
    void buildPurchaseOrderIssuedIncludesStyledHtmlAndOrderId() {
        UUID orderId = UUID.randomUUID();
        List<EmailTemplateService.EmailLineItem> lineItems = List.of(
                new EmailTemplateService.EmailLineItem("Product D", "SKU-D", 20, new BigDecimal("2.30"), new BigDecimal("46.00"))
        );

        RenderedEmail email = service.buildPurchaseOrderIssued(orderId, LocalDate.of(2026, 3, 6), lineItems);

        assertTrue(email.subject().contains(orderId.toString()));
        assertTrue(email.textBody().contains(orderId.toString()));
        assertTrue(email.htmlBody().contains(orderId.toString()));
        assertTrue(email.htmlBody().contains("Order Reference"));
        assertTrue(email.textBody().contains("Product D (SKU-D)"));
        assertTrue(email.textBody().contains("Unit: $2.30"));
        assertTrue(email.textBody().contains("Total Cost: $46.00"));
        assertTrue(email.textBody().contains("Expected Delivery Date: 2026-03-06"));
    }
}
