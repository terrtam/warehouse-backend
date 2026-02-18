package com.example.warehouse.integration;

import com.example.warehouse.dto.CreateSupplierRequest;
import com.example.warehouse.dto.UpdateSupplierRequest;
import com.example.warehouse.entity.SupplierEntity;
import com.example.warehouse.event.SupplierChangedEvent;
import com.example.warehouse.exception.ConflictException;
import com.example.warehouse.repository.SupplierRepository;
import com.example.warehouse.service.SupplierService;
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
class SupplierRealtimePublisherIntegrationTest {

    @Autowired
    private SupplierService supplierService;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockitoBean
    private SimpMessagingTemplate messagingTemplate;

    @BeforeEach
    void setUp() {
        supplierRepository.deleteAll();
        Mockito.reset(messagingTemplate);
    }

    @Test
    void publishOnCreateAfterCommit() {
        CreateSupplierRequest request = new CreateSupplierRequest();
        request.setName("Create Supplier");
        request.setEmail("create@supplier.com");
        request.setStatus("ACTIVE");

        supplierService.createSupplier(request);

        ArgumentCaptor<SupplierChangedEvent> captor = ArgumentCaptor.forClass(SupplierChangedEvent.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/suppliers"), captor.capture());
        SupplierChangedEvent event = captor.getValue();
        assertEquals("supplier.created", event.getType());
        assertNotNull(event.getId());
        assertNotNull(event.getVersion());
        assertNotNull(event.getAt());
    }

    @Test
    void publishOnUpdateAfterCommit() {
        SupplierEntity supplier = new SupplierEntity();
        supplier.setName("Update Supplier");
        supplier.setPhone("555-1001");
        supplier.setStatus("ACTIVE");
        SupplierEntity saved = supplierRepository.saveAndFlush(supplier);
        Mockito.reset(messagingTemplate);

        UpdateSupplierRequest request = new UpdateSupplierRequest();
        request.setVersion(saved.getVersion());
        request.setName("Update Supplier v2");
        request.setPhone("555-1002");
        request.setAddress("Receiving Gate");
        request.setStatus("ACTIVE");

        supplierService.updateSupplier(saved.getId(), request);

        ArgumentCaptor<SupplierChangedEvent> captor = ArgumentCaptor.forClass(SupplierChangedEvent.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/suppliers"), captor.capture());
        SupplierChangedEvent event = captor.getValue();
        assertEquals("supplier.updated", event.getType());
        assertEquals(saved.getId(), event.getId());
        assertEquals(saved.getVersion() + 1, event.getVersion());
        assertNotNull(event.getAt());
    }

    @Test
    void noPublishOnRollback() {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        CreateSupplierRequest request = new CreateSupplierRequest();
        request.setName("Rollback Supplier");
        request.setPhone("555-2001");
        request.setStatus("ACTIVE");

        template.execute(status -> {
            supplierService.createSupplier(request);
            status.setRollbackOnly();
            return null;
        });

        verifyNoInteractions(messagingTemplate);
    }

    @Test
    void noPublishOnFailure() {
        SupplierEntity supplier = new SupplierEntity();
        supplier.setName("Failure Supplier");
        supplier.setPhone("555-3001");
        supplier.setStatus("ACTIVE");
        SupplierEntity saved = supplierRepository.saveAndFlush(supplier);
        Mockito.reset(messagingTemplate);

        UpdateSupplierRequest request = new UpdateSupplierRequest();
        request.setVersion(saved.getVersion() + 1);
        request.setName("Failure Supplier Updated");
        request.setPhone("555-3002");
        request.setStatus("ACTIVE");

        assertThrows(ConflictException.class, () -> supplierService.updateSupplier(saved.getId(), request));
        verifyNoInteractions(messagingTemplate);
    }
}
