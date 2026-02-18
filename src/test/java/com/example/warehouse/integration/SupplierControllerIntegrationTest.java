package com.example.warehouse.integration;

import com.example.warehouse.entity.SupplierEntity;
import com.example.warehouse.repository.SupplierRepository;
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
class SupplierControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SupplierRepository supplierRepository;

    @BeforeEach
    void setUp() {
        supplierRepository.deleteAll();
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void createReturns201() throws Exception {
        String request = """
                {
                  "name": "Acme Supply",
                  "email": "ops@acmesupply.com",
                  "address": "123 Industrial Ave",
                  "status": "ACTIVE"
                }
                """;

        mockMvc.perform(post("/api/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("Acme Supply"))
                .andExpect(jsonPath("$.email").value("ops@acmesupply.com"));

        assertEquals(1, supplierRepository.count());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void getByIdReturnsRow() throws Exception {
        SupplierEntity supplier = new SupplierEntity();
        supplier.setName("Globex Supplier");
        supplier.setEmail("sales@globexsuppliers.com");
        supplier.setAddress("West Dock");
        supplier.setStatus("ACTIVE");
        SupplierEntity saved = supplierRepository.saveAndFlush(supplier);

        mockMvc.perform(get("/api/suppliers/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId().toString()))
                .andExpect(jsonPath("$.address").value("West Dock"));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void updateIncrementsVersion() throws Exception {
        SupplierEntity supplier = new SupplierEntity();
        supplier.setName("Globex Supplier");
        supplier.setEmail("ops@globexsuppliers.com");
        supplier.setStatus("ACTIVE");
        SupplierEntity saved = supplierRepository.saveAndFlush(supplier);

        mockMvc.perform(put("/api/suppliers/{id}", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "version": %d,
                                  "name": "Globex Supplier Updated",
                                  "email": "ops@globexsuppliers.com",
                                  "phone": "555-4400",
                                  "address": "New Dock 4",
                                  "status": "ACTIVE"
                                }
                                """.formatted(saved.getVersion())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(saved.getVersion() + 1))
                .andExpect(jsonPath("$.phone").value("555-4400"))
                .andExpect(jsonPath("$.address").value("New Dock 4"));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void listReturnsPagedPayload() throws Exception {
        SupplierEntity supplier = new SupplierEntity();
        supplier.setName("Paged Supplier");
        supplier.setPhone("555-1234");
        supplier.setStatus("ACTIVE");
        SupplierEntity saved = supplierRepository.saveAndFlush(supplier);

        mockMvc.perform(get("/api/suppliers")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(saved.getId().toString()));
    }

    @Test
    @WithMockUser(roles = "STAFF")
    void staffCanGetButCannotPostOrPut() throws Exception {
        SupplierEntity supplier = new SupplierEntity();
        supplier.setName("Read Only Supplier");
        supplier.setPhone("555-7777");
        supplier.setStatus("ACTIVE");
        SupplierEntity saved = supplierRepository.saveAndFlush(supplier);

        mockMvc.perform(get("/api/suppliers/{id}", saved.getId()))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Blocked Create",
                                  "phone": "555-8800",
                                  "status": "ACTIVE"
                                }
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/suppliers/{id}", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "version": %d,
                                  "name": "Blocked Update",
                                  "phone": "555-9900",
                                  "status": "ACTIVE"
                                }
                                """.formatted(saved.getVersion())))
                .andExpect(status().isForbidden());
    }
}
