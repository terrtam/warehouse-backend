package com.example.warehouse.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class EmailTemplateService {

    public RenderedEmail buildSalesOrderConfirmed(
            UUID orderId,
            String customerName,
            String customerAddress,
            LocalDate expectedDeliveryDate,
            List<EmailLineItem> lineItems
    ) {
        return build(
                "Sales order confirmed: " + orderId,
                "Sales Order Confirmed",
                "Your order has been confirmed and is now processing.",
                orderId,
                customerName,
                customerAddress,
                expectedDeliveryDate,
                lineItems
        );
    }

    public RenderedEmail buildSalesOrderShipped(
            UUID orderId,
            String customerName,
            String customerAddress,
            LocalDate expectedDeliveryDate,
            List<EmailLineItem> lineItems
    ) {
        return build(
                "Sales order shipped: " + orderId,
                "Sales Order Shipment Update",
                "Your order has shipment activity.",
                orderId,
                customerName,
                customerAddress,
                expectedDeliveryDate,
                lineItems
        );
    }

    public RenderedEmail buildPurchaseOrderIssued(UUID orderId, LocalDate expectedDeliveryDate, List<EmailLineItem> lineItems) {
        return build(
                "Purchase order issued: " + orderId,
                "Purchase Order Issued",
                "Please fulfill purchase order " + orderId + ".",
                orderId,
                null,
                null,
                expectedDeliveryDate,
                lineItems
        );
    }

    private RenderedEmail build(
            String subject,
            String heading,
            String message,
            UUID orderId,
            String customerName,
            String customerAddress,
            LocalDate expectedDeliveryDate,
            List<EmailLineItem> lineItems
    ) {
        String safeOrderId = orderId == null ? "N/A" : orderId.toString();
        List<EmailLineItem> safeLineItems = lineItems == null ? List.of() : lineItems;
        String textTable = buildTextTable(safeLineItems);
        String htmlTableRows = buildHtmlTableRows(safeLineItems);
        BigDecimal totalCost = computeTotalCost(safeLineItems);
        String safeCustomerName = normalizeText(customerName, "N/A");
        String safeCustomerAddress = normalizeText(customerAddress, "N/A");
        String safeExpectedDeliveryDate = expectedDeliveryDate == null ? "N/A" : expectedDeliveryDate.toString();

        String textBody = heading
                + System.lineSeparator()
                + System.lineSeparator()
                + message
                + System.lineSeparator()
                + "Order ID: " + safeOrderId
                + System.lineSeparator()
                + "Expected Delivery Date: " + safeExpectedDeliveryDate
                + System.lineSeparator()
                + "Customer: " + safeCustomerName
                + System.lineSeparator()
                + "Address: " + safeCustomerAddress
                + System.lineSeparator()
                + System.lineSeparator()
                + "Items"
                + System.lineSeparator()
                + textTable
                + System.lineSeparator()
                + "Total Cost: $" + formatMoney(totalCost)
                + System.lineSeparator()
                + System.lineSeparator()
                + "This is an automated message from Warehouse.";

        String htmlBody = """
                <!doctype html>
                <html lang="en">
                  <body style="margin:0;padding:0;background-color:#f4f6fb;font-family:Arial,Helvetica,sans-serif;color:#1f2937;">
                    <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background-color:#f4f6fb;padding:24px 12px;">
                      <tr>
                        <td align="center">
                          <table role="presentation" width="600" cellspacing="0" cellpadding="0" style="width:100%%;max-width:600px;background-color:#ffffff;border-radius:12px;overflow:hidden;border:1px solid #e5e7eb;">
                            <tr>
                              <td style="background:#0f172a;padding:18px 24px;">
                                <p style="margin:0;color:#ffffff;font-size:16px;font-weight:700;letter-spacing:0.4px;">Warehouse Operations</p>
                              </td>
                            </tr>
                            <tr>
                              <td style="padding:24px;">
                                <h1 style="margin:0 0 12px;font-size:22px;line-height:1.3;color:#111827;">%s</h1>
                                <p style="margin:0 0 20px;font-size:15px;line-height:1.6;color:#374151;">%s</p>
                                <div style="margin-bottom:20px;padding:14px 16px;border-radius:8px;background:#eef2ff;border:1px solid #c7d2fe;">
                                  <p style="margin:0;font-size:13px;text-transform:uppercase;letter-spacing:0.6px;color:#4338ca;">Order Reference</p>
                                  <p style="margin:8px 0 0;font-size:16px;font-weight:700;color:#1f2937;">%s</p>
                                  <p style="margin:8px 0 0;font-size:13px;color:#334155;">Expected Delivery: %s</p>
                                </div>
                                <div style="margin-bottom:20px;padding:14px 16px;border-radius:8px;background:#f8fafc;border:1px solid #e2e8f0;">
                                  <p style="margin:0;font-size:13px;text-transform:uppercase;letter-spacing:0.6px;color:#334155;">Customer</p>
                                  <p style="margin:8px 0 0;font-size:14px;font-weight:600;color:#0f172a;">%s</p>
                                  <p style="margin:6px 0 0;font-size:13px;line-height:1.5;color:#334155;">%s</p>
                                </div>
                                <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="border-collapse:collapse;margin-bottom:20px;border:1px solid #e5e7eb;border-radius:8px;overflow:hidden;">
                                  <tr>
                                    <th align="left" style="padding:10px 12px;background:#f8fafc;color:#111827;font-size:13px;border-bottom:1px solid #e5e7eb;">Product</th>
                                    <th align="left" style="padding:10px 12px;background:#f8fafc;color:#111827;font-size:13px;border-bottom:1px solid #e5e7eb;">SKU</th>
                                    <th align="right" style="padding:10px 12px;background:#f8fafc;color:#111827;font-size:13px;border-bottom:1px solid #e5e7eb;">Quantity</th>
                                    <th align="right" style="padding:10px 12px;background:#f8fafc;color:#111827;font-size:13px;border-bottom:1px solid #e5e7eb;">Unit Price</th>
                                    <th align="right" style="padding:10px 12px;background:#f8fafc;color:#111827;font-size:13px;border-bottom:1px solid #e5e7eb;">Subtotal</th>
                                  </tr>
                                  %s
                                  <tr>
                                    <td colspan="4" align="right" style="padding:10px 12px;border-top:2px solid #cbd5e1;color:#0f172a;font-size:13px;font-weight:700;">Total Cost</td>
                                    <td align="right" style="padding:10px 12px;border-top:2px solid #cbd5e1;color:#0f172a;font-size:13px;font-weight:700;">$%s</td>
                                  </tr>
                                </table>
                                <span style="display:inline-block;padding:10px 16px;border-radius:8px;background:#2563eb;color:#ffffff;font-size:14px;font-weight:600;">
                                  Action tracked in Warehouse
                                </span>
                                <p style="margin:24px 0 0;font-size:12px;line-height:1.5;color:#6b7280;">
                                  This is an automated message from Warehouse. Replies to this notification are not monitored.
                                </p>
                              </td>
                            </tr>
                          </table>
                        </td>
                      </tr>
                    </table>
                  </body>
                </html>
                """.formatted(
                escapeHtml(heading),
                escapeHtml(message),
                escapeHtml(safeOrderId),
                escapeHtml(safeExpectedDeliveryDate),
                escapeHtml(safeCustomerName),
                escapeHtml(safeCustomerAddress),
                htmlTableRows,
                formatMoney(totalCost)
        );

        return new RenderedEmail(subject, textBody, htmlBody);
    }

    private String buildTextTable(List<EmailLineItem> lineItems) {
        if (lineItems.isEmpty()) {
            return "- No line items";
        }

        StringBuilder builder = new StringBuilder();
        for (EmailLineItem line : lineItems) {
            builder.append("- Product: ")
                    .append(line.productName())
                    .append(" (")
                    .append(line.sku())
                    .append(")")
                    .append(" | Qty: ")
                    .append(line.quantity())
                    .append(" | Unit: $")
                    .append(formatMoney(line.unitPrice()))
                    .append(" | Subtotal: $")
                    .append(formatMoney(line.subtotal()))
                    .append(System.lineSeparator());
        }
        return builder.toString().trim();
    }

    private String buildHtmlTableRows(List<EmailLineItem> lineItems) {
        if (lineItems.isEmpty()) {
            return """
                    <tr>
                      <td colspan="5" style="padding:10px 12px;color:#6b7280;font-size:13px;">No line items</td>
                    </tr>
                    """;
        }

        StringBuilder builder = new StringBuilder();
        for (EmailLineItem line : lineItems) {
            builder.append("""
                    <tr>
                      <td style="padding:10px 12px;border-top:1px solid #e5e7eb;color:#1f2937;font-size:13px;">%s</td>
                      <td style="padding:10px 12px;border-top:1px solid #e5e7eb;color:#1f2937;font-size:13px;">%s</td>
                      <td align="right" style="padding:10px 12px;border-top:1px solid #e5e7eb;color:#1f2937;font-size:13px;">%s</td>
                      <td align="right" style="padding:10px 12px;border-top:1px solid #e5e7eb;color:#1f2937;font-size:13px;">$%s</td>
                      <td align="right" style="padding:10px 12px;border-top:1px solid #e5e7eb;color:#1f2937;font-size:13px;">$%s</td>
                    </tr>
                    """.formatted(
                    escapeHtml(line.productName()),
                    escapeHtml(line.sku()),
                    line.quantity(),
                    formatMoney(line.unitPrice()),
                    formatMoney(line.subtotal())
            ));
        }
        return builder.toString();
    }

    private String formatMoney(BigDecimal value) {
        BigDecimal safe = value == null ? BigDecimal.ZERO : value;
        return safe.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private BigDecimal computeTotalCost(List<EmailLineItem> lineItems) {
        return lineItems.stream()
                .map(EmailLineItem::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String normalizeText(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim().replaceAll("\\s+", " ");
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    public record EmailLineItem(String productName, String sku, int quantity, BigDecimal unitPrice, BigDecimal subtotal) {
        public EmailLineItem {
            productName = productName == null || productName.isBlank() ? "Unknown Product" : productName;
            sku = sku == null || sku.isBlank() ? "-" : sku;
            quantity = Math.max(0, quantity);
            unitPrice = unitPrice == null ? BigDecimal.ZERO : unitPrice;
            subtotal = subtotal == null ? unitPrice.multiply(BigDecimal.valueOf(quantity)) : subtotal;
        }
    }
}
