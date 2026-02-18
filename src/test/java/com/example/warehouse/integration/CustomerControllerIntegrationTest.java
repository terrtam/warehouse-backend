package com.example.warehouse.integration;

import com.example.warehouse.entity.CustomerEntity;
import com.example.warehouse.repository.CustomerRepository;
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
class CustomerControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CustomerRepository customerRepository;

    @BeforeEach
    void setUp() {
        customerRepository.deleteAll();
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void postInsertsRow() throws Exception {
        String request = """
                {
                  "name": "Acme Inc",
                  "email": "ops@acme.com",
                  "phone": "555-1200"
                }
                """;

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("Acme Inc"))
                .andExpect(jsonPath("$.email").value("ops@acme.com"));
        assertEquals(1, customerRepository.count());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void getByIdReturnsCustomer() throws Exception {
        CustomerEntity customer = new CustomerEntity();
        customer.setName("Globex");
        customer.setEmail("sales@globex.com");
        customer.setStatus("ACTIVE");
        CustomerEntity saved = customerRepository.saveAndFlush(customer);

        mockMvc.perform(get("/api/customers/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId().toString()))
                .andExpect(jsonPath("$.email").value("sales@globex.com"));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void putIncrementsVersion() throws Exception {
        CustomerEntity customer = new CustomerEntity();
        customer.setName("Globex");
        customer.setEmail("ops@globex.com");
        customer.setStatus("ACTIVE");
        CustomerEntity saved = customerRepository.saveAndFlush(customer);

        mockMvc.perform(put("/api/customers/{id}", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "version": %d,
                                  "name": "Globex Logistics",
                                  "email": "ops@globex.com",
                                  "phone": "555-2000",
                                  "status": "ACTIVE"
                                }
                                """.formatted(saved.getVersion())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(saved.getVersion() + 1))
                .andExpect(jsonPath("$.name").value("Globex Logistics"))
                .andExpect(jsonPath("$.phone").value("555-2000"));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void listFiltersByUpdatedAfter() throws Exception {
        CustomerEntity first = new CustomerEntity();
        first.setName("First");
        first.setEmail("first@customers.com");
        first.setStatus("ACTIVE");
        first = customerRepository.saveAndFlush(first);

        Thread.sleep(30);

        CustomerEntity second = new CustomerEntity();
        second.setName("Second");
        second.setEmail("second@customers.com");
        second.setStatus("ACTIVE");
        second = customerRepository.saveAndFlush(second);

        mockMvc.perform(get("/api/customers")
                        .param("updatedAfter", first.getUpdatedAt().toString())
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(second.getId().toString()));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void duplicateEmailReturnsConflict() throws Exception {
        CustomerEntity existing = new CustomerEntity();
        existing.setName("First");
        existing.setEmail("dupe@customers.com");
        existing.setStatus("ACTIVE");
        customerRepository.saveAndFlush(existing);

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Second",
                                  "email": "dupe@customers.com",
                                  "status": "ACTIVE"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Customer email already exists"));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void optimisticLockConflictReturns409() throws Exception {
        CustomerEntity customer = new CustomerEntity();
        customer.setName("Conflict");
        customer.setEmail("conflict@customers.com");
        customer.setStatus("ACTIVE");
        CustomerEntity saved = customerRepository.saveAndFlush(customer);

        mockMvc.perform(put("/api/customers/{id}", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "version": %d,
                                  "name": "Conflict Updated",
                                  "email": "conflict@customers.com",
                                  "status": "ACTIVE"
                                }
                                """.formatted(saved.getVersion() + 1)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Customer version conflict"));
    }
}
