package com.example.warehouse.integration;

import com.example.warehouse.entity.Product;
import com.example.warehouse.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:warehouse;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "spring.flyway.enabled=false"
})
@AutoConfigureMockMvc
class ProductControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void postInsertsRow() throws Exception {
        String request = """
                {
                  "name": "Widget",
                  "sku": "SKU-100",
                  "status": "ACTIVE",
                  "description": "Steel widget"
                }
                """;

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.description").value("Steel widget"));
        assertEquals(1, productRepository.count());
        Product saved = productRepository.findAll().get(0);
        assertEquals("Steel widget", saved.getDescription());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void putIncrementsVersion() throws Exception {
        Product product = new Product();
        product.setName("Widget");
        product.setSku("SKU-100");
        product.setStatus("ACTIVE");
        Product saved = productRepository.saveAndFlush(product);

        mockMvc.perform(put("/api/products/{id}", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                .content("""
                                {
                                  "version": %d,
                                  "name": "Widget Updated",
                                  "sku": "SKU-100",
                                  "status": "ACTIVE",
                                  "description": "Updated description"
                                }
                                """.formatted(saved.getVersion())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Updated description"));
        Product reloaded = productRepository.findById(saved.getId()).orElseThrow();
        assertEquals("Updated description", reloaded.getDescription());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void listReturnsDescription() throws Exception {
        Product product = new Product();
        product.setName("Widget");
        product.setSku("SKU-101");
        product.setStatus("ACTIVE");
        product.setDescription("List description");
        productRepository.saveAndFlush(product);

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].description").value("List description"));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void getByIdReturnsDescription() throws Exception {
        Product product = new Product();
        product.setName("Widget");
        product.setSku("SKU-102");
        product.setStatus("ACTIVE");
        product.setDescription("Detail description");
        Product saved = productRepository.saveAndFlush(product);

        mockMvc.perform(get("/api/products/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Detail description"))
                .andExpect(jsonPath("$.id").value(saved.getId().toString()));
    }
}
