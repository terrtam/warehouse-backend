package com.example.warehouse.service;

import com.example.warehouse.dto.CreateProductRequest;
import com.example.warehouse.dto.UpdateProductRequest;
import com.example.warehouse.entity.Product;
import com.example.warehouse.event.ProductChangedEvent;
import com.example.warehouse.exception.ConflictException;
import com.example.warehouse.exception.ValidationException;
import com.example.warehouse.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ProductService productService;

    @Test
    void createProductRejectsDuplicateSku() {
        CreateProductRequest request = new CreateProductRequest();
        request.setName("Widget");
        request.setSku("SKU-100");

        when(productRepository.existsBySku("SKU-100")).thenReturn(true);

        assertThrows(ValidationException.class, () -> productService.createProduct(request));

        verify(productRepository, never()).save(any(Product.class));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void updateProductDetectsVersionConflict() {
        UUID id = UUID.randomUUID();
        Product existing = new Product();
        existing.setId(id);
        existing.setVersion(2L);

        UpdateProductRequest request = new UpdateProductRequest();
        request.setVersion(1L);
        request.setName("Widget");
        request.setSku("SKU-100");

        when(productRepository.findById(id)).thenReturn(Optional.of(existing));

        assertThrows(ConflictException.class, () -> productService.updateProduct(id, request));

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void createProductPublishesEvent() {
        CreateProductRequest request = new CreateProductRequest();
        request.setName("Widget");
        request.setSku("SKU-100");

        Product saved = new Product();
        saved.setId(UUID.randomUUID());
        saved.setVersion(0L);

        when(productRepository.existsBySku("SKU-100")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenReturn(saved);

        productService.createProduct(request);

        ArgumentCaptor<ProductChangedEvent> captor = ArgumentCaptor.forClass(ProductChangedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());

        ProductChangedEvent event = captor.getValue();
        assertEquals("product.created", event.getType());
        assertEquals(saved.getId(), event.getId());
        assertEquals(saved.getVersion(), event.getVersion());
        assertNotNull(event.getAt());
    }

    @Test
    void updateProductPublishesEvent() {
        UUID id = UUID.randomUUID();
        Product existing = new Product();
        existing.setId(id);
        existing.setVersion(1L);

        UpdateProductRequest request = new UpdateProductRequest();
        request.setVersion(1L);
        request.setName("Widget");
        request.setSku("SKU-100");

        Product saved = new Product();
        saved.setId(id);
        saved.setVersion(2L);

        when(productRepository.findById(id)).thenReturn(Optional.of(existing));
        when(productRepository.existsBySkuAndIdNot("SKU-100", id)).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenReturn(saved);

        productService.updateProduct(id, request);

        ArgumentCaptor<ProductChangedEvent> captor = ArgumentCaptor.forClass(ProductChangedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());

        ProductChangedEvent event = captor.getValue();
        assertEquals("product.updated", event.getType());
        assertEquals(saved.getId(), event.getId());
        assertEquals(saved.getVersion(), event.getVersion());
        assertNotNull(event.getAt());
    }
}
