package com.example.warehouse.integration;

import com.example.warehouse.dto.CreateSalesOrderLineRequest;
import com.example.warehouse.dto.CreateSalesOrderRequest;
import com.example.warehouse.entity.CustomerEntity;
import com.example.warehouse.entity.Product;
import com.example.warehouse.event.InventoryChangedEvent;
import com.example.warehouse.event.OrderChangedEvent;
import com.example.warehouse.repository.CustomerRepository;
import com.example.warehouse.repository.ProductRepository;
import com.example.warehouse.repository.SalesOrderRepository;
import com.example.warehouse.service.SalesOrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:warehouse-realtime;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "spring.flyway.enabled=false",
        "spring.task.scheduling.enabled=false"
})
class OrderInventoryRealtimePublisherIntegrationTest {

    @Autowired
    private SalesOrderService salesOrderService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private SalesOrderRepository salesOrderRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockitoBean
    private SimpMessagingTemplate messagingTemplate;

    private Product product;
    private CustomerEntity customer;

    @BeforeEach
    void setUp() {
        salesOrderRepository.deleteAll();
        customerRepository.deleteAll();
        productRepository.deleteAll();
        Mockito.reset(messagingTemplate);

        Product seededProduct = new Product();
        seededProduct.setName("Realtime Product");
        seededProduct.setSku("RT-PRODUCT-001");
        seededProduct.setStatus("ACTIVE");
        product = productRepository.saveAndFlush(seededProduct);

        CustomerEntity seededCustomer = new CustomerEntity();
        seededCustomer.setName("Realtime Customer");
        seededCustomer.setEmail("realtime-customer@example.com");
        seededCustomer.setStatus("ACTIVE");
        customer = customerRepository.saveAndFlush(seededCustomer);
    }

    @Test
    void websocketPublishersTriggerAfterCommitForOrderAndInventoryTopics() {
        CreateSalesOrderRequest request = buildCreateOrderRequest();
        var created = salesOrderService.createOrder(request);
        salesOrderService.confirmOrder(created.getId(), created.getVersion());

        verify(messagingTemplate, atLeastOnce()).convertAndSend(eq("/topic/orders"), any(OrderChangedEvent.class));
        verify(messagingTemplate, atLeastOnce()).convertAndSend(eq("/topic/inventory"), any(InventoryChangedEvent.class));
    }

    @Test
    void websocketPublishersDoNotTriggerOnRollback() {
        TransactionTemplate template = new TransactionTemplate(transactionManager);

        template.execute(status -> {
            CreateSalesOrderRequest request = buildCreateOrderRequest();
            salesOrderService.createOrder(request);
            status.setRollbackOnly();
            return null;
        });

        verifyNoInteractions(messagingTemplate);
    }

    private CreateSalesOrderRequest buildCreateOrderRequest() {
        CreateSalesOrderLineRequest line = new CreateSalesOrderLineRequest();
        line.setProductId(product.getId());
        line.setQuantity(1);

        CreateSalesOrderRequest request = new CreateSalesOrderRequest();
        request.setCustomerId(customer.getId());
        request.setLines(List.of(line));
        return request;
    }
}
