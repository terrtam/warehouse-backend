package com.example.warehouse.integration;

import com.example.warehouse.dto.CreateProductRequest;
import com.example.warehouse.event.ProductChangedEvent;
import com.example.warehouse.repository.ProductRepository;
import com.example.warehouse.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
class ProductRealtimePublisherIntegrationTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockBean
    private SimpMessagingTemplate messagingTemplate;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
        Mockito.reset(messagingTemplate);
    }

    @Test
    void websocketPublisherTriggeredAfterCommit() {
        CreateProductRequest request = new CreateProductRequest();
        request.setName("Widget");
        request.setSku("SKU-200");
        request.setStatus("ACTIVE");

        productService.createProduct(request);

        ArgumentCaptor<ProductChangedEvent> captor = ArgumentCaptor.forClass(ProductChangedEvent.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/products"), captor.capture());

        ProductChangedEvent event = captor.getValue();
        assertEquals("product.created", event.getType());
        assertNotNull(event.getId());
        assertNotNull(event.getVersion());
        assertNotNull(event.getAt());
    }

    @Test
    void websocketPublisherNotTriggeredOnRollback() {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        CreateProductRequest request = new CreateProductRequest();
        request.setName("Widget");
        request.setSku("SKU-201");
        request.setStatus("ACTIVE");

        template.execute(status -> {
            productService.createProduct(request);
            status.setRollbackOnly();
            return null;
        });

        verifyNoInteractions(messagingTemplate);
    }
}
