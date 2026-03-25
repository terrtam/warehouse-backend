package com.example.warehouse.integration;

import com.example.warehouse.entity.CustomerEntity;
import com.example.warehouse.entity.InventoryTransactionType;
import com.example.warehouse.entity.Product;
import com.example.warehouse.entity.SalesOrderEntity;
import com.example.warehouse.entity.SupplierEntity;
import com.example.warehouse.repository.CustomerRepository;
import com.example.warehouse.repository.InventoryTransactionRepository;
import com.example.warehouse.repository.ProductRepository;
import com.example.warehouse.repository.PurchaseOrderRepository;
import com.example.warehouse.repository.SalesOrderRepository;
import com.example.warehouse.repository.SupplierRepository;
import com.example.warehouse.service.InventoryComputationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:warehouse-operational;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "spring.flyway.enabled=false",
        "spring.task.scheduling.enabled=false"
})
@AutoConfigureMockMvc
class OperationalFlowsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private SalesOrderRepository salesOrderRepository;

    @Autowired
    private PurchaseOrderRepository purchaseOrderRepository;

    @Autowired
    private InventoryTransactionRepository inventoryTransactionRepository;

    @Autowired
    private InventoryComputationService inventoryComputationService;

    private Product product;
    private CustomerEntity customer;
    private SupplierEntity supplier;

    @BeforeEach
    void setUp() {
        inventoryTransactionRepository.deleteAll();
        salesOrderRepository.deleteAll();
        purchaseOrderRepository.deleteAll();
        customerRepository.deleteAll();
        supplierRepository.deleteAll();
        productRepository.deleteAll();

        Product seededProduct = new Product();
        seededProduct.setName("Operational Product");
        seededProduct.setSku("OPS-PRD-001");
        seededProduct.setStatus("ACTIVE");
        product = productRepository.saveAndFlush(seededProduct);

        CustomerEntity seededCustomer = new CustomerEntity();
        seededCustomer.setName("Operational Customer");
        seededCustomer.setEmail("ops-customer@example.com");
        seededCustomer.setStatus("ACTIVE");
        customer = customerRepository.saveAndFlush(seededCustomer);

        SupplierEntity seededSupplier = new SupplierEntity();
        seededSupplier.setName("Operational Supplier");
        seededSupplier.setEmail("ops-supplier@example.com");
        seededSupplier.setStatus("ACTIVE");
        supplier = supplierRepository.saveAndFlush(seededSupplier);
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void reservationShipmentAndReceivingRespectTransactionSourceOfTruth() throws Exception {
        postJson("/api/inventory/adjustments", """
                {
                  "productId": "%s",
                  "quantityDelta": 10,
                  "reason": "Initial stock"
                }
                """.formatted(product.getId()));

        JsonNode firstOrder = postJson("/api/sales-orders", """
                {
                  "customerId": "%s",
                  "lines": [
                    {
                      "productId": "%s",
                      "quantity": 8,
                      "unitPrice": 12.50
                    }
                  ]
                }
                """.formatted(customer.getId(), product.getId()));
        UUID firstOrderId = UUID.fromString(firstOrder.path("id").asText());

        JsonNode firstConfirmed = postJson("/api/sales-orders/%s/confirm".formatted(firstOrderId), """
                {
                  "version": %d
                }
                """.formatted(firstOrder.path("version").asLong()));
        UUID firstLineId = UUID.fromString(firstConfirmed.path("lines").get(0).path("id").asText());

        JsonNode secondOrder = postJson("/api/sales-orders", """
                {
                  "customerId": "%s",
                  "lines": [
                    {
                      "productId": "%s",
                      "quantity": 5
                    }
                  ]
                }
                """.formatted(customer.getId(), product.getId()));
        UUID secondOrderId = UUID.fromString(secondOrder.path("id").asText());

        postJson("/api/sales-orders/%s/confirm".formatted(secondOrderId), """
                {
                  "version": %d
                }
                """.formatted(secondOrder.path("version").asLong()));

        postJson("/api/sales-orders/%s/ship".formatted(firstOrderId), """
                {
                  "version": %d,
                  "lines": [
                    {
                      "lineId": "%s",
                      "quantity": 8
                    }
                  ]
                }
                """.formatted(firstConfirmed.path("version").asLong(), firstLineId));

        assertEquals(2, inventoryComputationService.onHandForProduct(product.getId()));
        assertEquals(2, inventoryComputationService.reservedForProduct(product.getId()));
        assertEquals(0, inventoryComputationService.availableForProduct(product.getId()));

        JsonNode purchaseOrder = postJson("/api/purchase-orders", """
                {
                  "supplierId": "%s",
                  "lines": [
                    {
                      "productId": "%s",
                      "quantity": 5,
                      "unitPrice": 6.25
                    }
                  ]
                }
                """.formatted(supplier.getId(), product.getId()));
        UUID purchaseOrderId = UUID.fromString(purchaseOrder.path("id").asText());

        JsonNode ordered = postJson("/api/purchase-orders/%s/order".formatted(purchaseOrderId), """
                {
                  "version": %d
                }
                """.formatted(purchaseOrder.path("version").asLong()));
        UUID purchaseLineId = UUID.fromString(ordered.path("lines").get(0).path("id").asText());

        postJson("/api/purchase-orders/%s/receive".formatted(purchaseOrderId), """
                {
                  "version": %d,
                  "lines": [
                    {
                      "lineId": "%s",
                      "quantity": 5
                    }
                  ]
                }
                """.formatted(ordered.path("version").asLong(), purchaseLineId));

        SalesOrderEntity reloadedSecondOrder = salesOrderRepository.findByIdWithDetails(secondOrderId).orElseThrow();
        assertEquals(5, reloadedSecondOrder.getLines().get(0).getQuantityReserved());
        assertEquals("PROCESSING", reloadedSecondOrder.getStatus().name());

        assertEquals(7, inventoryComputationService.onHandForProduct(product.getId()));
        assertEquals(5, inventoryComputationService.reservedForProduct(product.getId()));
        assertEquals(2, inventoryComputationService.availableForProduct(product.getId()));

        long adjustCount = inventoryTransactionRepository.findAll().stream()
                .filter(txn -> txn.getTransactionType() == InventoryTransactionType.ADJUST)
                .count();
        long outCount = inventoryTransactionRepository.findAll().stream()
                .filter(txn -> txn.getTransactionType() == InventoryTransactionType.OUT)
                .count();
        long inCount = inventoryTransactionRepository.findAll().stream()
                .filter(txn -> txn.getTransactionType() == InventoryTransactionType.IN)
                .count();
        assertEquals(1, adjustCount);
        assertEquals(1, outCount);
        assertEquals(1, inCount);
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void staleOrderVersionReturnsConflict() throws Exception {
        JsonNode salesOrder = postJson("/api/sales-orders", """
                {
                  "customerId": "%s",
                  "lines": [
                    {
                      "productId": "%s",
                      "quantity": 1
                    }
                  ]
                }
                """.formatted(customer.getId(), product.getId()));
        long staleSalesVersion = salesOrder.path("version").asLong() + 1;

        mockMvc.perform(post("/api/sales-orders/{id}/confirm", salesOrder.path("id").asText())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "version": %d
                                }
                                """.formatted(staleSalesVersion)))
                .andExpect(status().isConflict());

        JsonNode purchaseOrder = postJson("/api/purchase-orders", """
                {
                  "supplierId": "%s",
                  "lines": [
                    {
                      "productId": "%s",
                      "quantity": 1
                    }
                  ]
                }
                """.formatted(supplier.getId(), product.getId()));
        long stalePurchaseVersion = purchaseOrder.path("version").asLong() + 1;

        mockMvc.perform(post("/api/purchase-orders/{id}/order", purchaseOrder.path("id").asText())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "version": %d
                                }
                                """.formatted(stalePurchaseVersion)))
                .andExpect(status().isConflict());
    }

    private JsonNode postJson(String path, String body) throws Exception {
        MvcResult result = mockMvc.perform(post(path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        assertTrue(response.isObject());
        return response;
    }
}
