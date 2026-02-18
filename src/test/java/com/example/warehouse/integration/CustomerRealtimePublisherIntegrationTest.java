package com.example.warehouse.integration;

import com.example.warehouse.dto.CreateCustomerRequest;
import com.example.warehouse.dto.UpdateCustomerRequest;
import com.example.warehouse.entity.CustomerEntity;
import com.example.warehouse.event.CustomerChangedEvent;
import com.example.warehouse.exception.ConflictException;
import com.example.warehouse.repository.CustomerRepository;
import com.example.warehouse.service.CustomerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:warehouse;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "spring.flyway.enabled=false"
})
class CustomerRealtimePublisherIntegrationTest {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockitoBean
    private SimpMessagingTemplate messagingTemplate;

    @BeforeEach
    void setUp() {
        customerRepository.deleteAll();
        Mockito.reset(messagingTemplate);
    }

    @Test
    void websocketPublisherTriggeredAfterCommitOnCreate() {
        CreateCustomerRequest request = new CreateCustomerRequest();
        request.setName("Acme");
        request.setEmail("acme@customers.com");
        request.setStatus("ACTIVE");

        customerService.createCustomer(request);

        ArgumentCaptor<CustomerChangedEvent> captor = ArgumentCaptor.forClass(CustomerChangedEvent.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/customers"), captor.capture());
        CustomerChangedEvent event = captor.getValue();
        assertEquals("CUSTOMER_CREATED", event.getEventType());
        assertNotNull(event.getCustomer());
        assertEquals("acme@customers.com", event.getCustomer().getEmail());
        assertNotNull(event.getOccurredAt());
    }

    @Test
    void websocketPublisherTriggeredAfterCommitOnUpdate() {
        CreateCustomerRequest createRequest = new CreateCustomerRequest();
        createRequest.setName("Beta");
        createRequest.setEmail("beta@customers.com");
        createRequest.setStatus("ACTIVE");

        CustomerEntity created = customerRepository.saveAndFlush(toEntity(createRequest));
        Mockito.reset(messagingTemplate);

        UpdateCustomerRequest updateRequest = new UpdateCustomerRequest();
        updateRequest.setVersion(created.getVersion());
        updateRequest.setName("Beta Updated");
        updateRequest.setEmail("beta@customers.com");
        updateRequest.setPhone("555-3300");
        updateRequest.setStatus("ACTIVE");

        customerService.updateCustomer(created.getId(), updateRequest);

        ArgumentCaptor<CustomerChangedEvent> captor = ArgumentCaptor.forClass(CustomerChangedEvent.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/customers"), captor.capture());
        CustomerChangedEvent event = captor.getValue();
        assertEquals("CUSTOMER_UPDATED", event.getEventType());
        assertEquals(created.getId(), event.getCustomer().getId());
        assertEquals("Beta Updated", event.getCustomer().getName());
        assertNotNull(event.getOccurredAt());
    }

    @Test
    void websocketPublisherNotTriggeredOnRollback() {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        CreateCustomerRequest request = new CreateCustomerRequest();
        request.setName("Rollback");
        request.setEmail("rollback@customers.com");
        request.setStatus("ACTIVE");

        template.execute(status -> {
            customerService.createCustomer(request);
            status.setRollbackOnly();
            return null;
        });

        verifyNoInteractions(messagingTemplate);
    }

    @Test
    void websocketPublisherNotTriggeredOnFailure() {
        CreateCustomerRequest request = new CreateCustomerRequest();
        request.setName("Initial");
        request.setEmail("dupe@customers.com");
        request.setStatus("ACTIVE");
        customerService.createCustomer(request);
        Mockito.reset(messagingTemplate);

        CreateCustomerRequest duplicate = new CreateCustomerRequest();
        duplicate.setName("Duplicate");
        duplicate.setEmail("dupe@customers.com");
        duplicate.setStatus("ACTIVE");

        assertThrows(ConflictException.class, () -> customerService.createCustomer(duplicate));
        verifyNoInteractions(messagingTemplate);
    }

    private CustomerEntity toEntity(CreateCustomerRequest request) {
        CustomerEntity entity = new CustomerEntity();
        entity.setName(request.getName());
        entity.setEmail(request.getEmail());
        entity.setStatus(request.getStatus());
        return entity;
    }
}
