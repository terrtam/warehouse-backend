package com.example.warehouse.integration;

import com.example.warehouse.entity.Product;
import com.example.warehouse.repository.InventoryTransactionRepository;
import com.example.warehouse.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:warehouse-rbac;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "spring.flyway.enabled=false"
})
@AutoConfigureMockMvc
class RbacMatrixIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventoryTransactionRepository inventoryTransactionRepository;

    private Product product;

    @BeforeEach
    void setUp() {
        inventoryTransactionRepository.deleteAll();
        productRepository.deleteAll();

        Product seeded = new Product();
        seeded.setName("Seed Product");
        seeded.setSku("RBAC-SEED-001");
        seeded.setStatus("ACTIVE");
        product = productRepository.saveAndFlush(seeded);
    }

    @Test
    @WithMockUser(roles = "STAFF")
    void staffCannotCreateMasterDataProducts() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Blocked",
                                  "sku": "RBAC-BLOCKED-001",
                                  "status": "ACTIVE"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "STAFF")
    void staffCannotUpdateMasterDataCustomers() throws Exception {
        mockMvc.perform(put("/api/customers/{id}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "version": 0,
                                  "name": "Blocked",
                                  "email": "blocked@example.com",
                                  "status": "ACTIVE"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "STAFF")
    void staffCanPerformOperationalInventoryAdjustment() throws Exception {
        mockMvc.perform(post("/api/inventory/adjustments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": "%s",
                                  "quantityDelta": 3,
                                  "reason": "Cycle count correction"
                                }
                                """.formatted(product.getId())))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void managerCanCreateProductAndAdjustInventory() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Manager Product",
                                  "sku": "RBAC-MANAGER-001",
                                  "status": "ACTIVE"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/inventory/adjustments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": "%s",
                                  "quantityDelta": 2,
                                  "reason": "Internal use"
                                }
                                """.formatted(product.getId())))
                .andExpect(status().isOk());
    }
}
