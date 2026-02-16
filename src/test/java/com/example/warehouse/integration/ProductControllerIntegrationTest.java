package com.example.warehouse.integration;

import com.example.warehouse.dto.CreateProductRequest;
import com.example.warehouse.dto.ProductDto;
import com.example.warehouse.dto.UpdateProductRequest;
import com.example.warehouse.entity.Product;
import com.example.warehouse.repository.ProductRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
    }

    @Test
    @WithMockUser
    void postInsertsRow() throws Exception {
        CreateProductRequest request = new CreateProductRequest();
        request.setName("Widget");
        request.setSku("SKU-100");
        request.setStatus("ACTIVE");
        request.setDescription("Steel widget");

        MvcResult result = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        ProductDto response = objectMapper.readValue(result.getResponse().getContentAsString(), ProductDto.class);
        assertNotNull(response.getId());
        assertEquals("Steel widget", response.getDescription());
        assertEquals(1, productRepository.count());
        Product saved = productRepository.findAll().get(0);
        assertEquals("Steel widget", saved.getDescription());
    }

    @Test
    @WithMockUser
    void putIncrementsVersion() throws Exception {
        Product product = new Product();
        product.setName("Widget");
        product.setSku("SKU-100");
        product.setStatus("ACTIVE");
        Product saved = productRepository.saveAndFlush(product);

        UpdateProductRequest request = new UpdateProductRequest();
        request.setVersion(saved.getVersion());
        request.setName("Widget Updated");
        request.setSku("SKU-100");
        request.setStatus("ACTIVE");
        request.setDescription("Updated description");

        MvcResult result = mockMvc.perform(put("/api/products/{id}", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        ProductDto response = objectMapper.readValue(result.getResponse().getContentAsString(), ProductDto.class);
        assertEquals(saved.getVersion() + 1, response.getVersion());
        assertEquals("Updated description", response.getDescription());
        Product reloaded = productRepository.findById(saved.getId()).orElseThrow();
        assertEquals("Updated description", reloaded.getDescription());
    }

    @Test
    @WithMockUser
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
    @WithMockUser
    void getByIdReturnsDescription() throws Exception {
        Product product = new Product();
        product.setName("Widget");
        product.setSku("SKU-102");
        product.setStatus("ACTIVE");
        product.setDescription("Detail description");
        Product saved = productRepository.saveAndFlush(product);

        MvcResult result = mockMvc.perform(get("/api/products/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andReturn();

        ProductDto response = objectMapper.readValue(result.getResponse().getContentAsString(), ProductDto.class);
        assertEquals("Detail description", response.getDescription());
        assertTrue(response.getId().equals(saved.getId()));
    }
}
