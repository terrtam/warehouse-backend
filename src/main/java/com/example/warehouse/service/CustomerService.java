package com.example.warehouse.service;

import com.example.warehouse.dto.CreateCustomerRequest;
import com.example.warehouse.dto.CustomerDto;
import com.example.warehouse.dto.UpdateCustomerRequest;
import com.example.warehouse.entity.CustomerEntity;
import com.example.warehouse.event.CustomerChangedEvent;
import com.example.warehouse.exception.ConflictException;
import com.example.warehouse.exception.NotFoundException;
import com.example.warehouse.repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Service
public class CustomerService {

    private static final String DUPLICATE_EMAIL_MESSAGE = "Customer email already exists";

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public CustomerDto getCustomer(UUID id) {
        CustomerEntity customer = customerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Customer not found"));
        return toDto(customer);
    }

    @Transactional(readOnly = true)
    public Page<CustomerDto> listCustomers(Instant updatedAfter, Pageable pageable) {
        Page<CustomerEntity> page = updatedAfter == null
                ? customerRepository.findAll(pageable)
                : customerRepository.findAllByUpdatedAtGreaterThan(updatedAfter, pageable);
        return page.map(this::toDto);
    }

    @Transactional
    public CustomerDto createCustomer(CreateCustomerRequest request) {
        if (customerRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException(DUPLICATE_EMAIL_MESSAGE);
        }

        CustomerEntity customer = new CustomerEntity();
        applyCreateRequest(customer, request);
        CustomerEntity saved = saveHandlingDuplicateEmail(customer);
        CustomerDto customerDto = toDto(saved);
        eventPublisher.publishEvent(CustomerChangedEvent.created(customerDto));
        return customerDto;
    }

    @Transactional
    public CustomerDto updateCustomer(UUID id, UpdateCustomerRequest request) {
        CustomerEntity customer = customerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Customer not found"));

        if (!Objects.equals(customer.getVersion(), request.getVersion())) {
            throw new ConflictException("Customer version conflict");
        }

        if (customerRepository.existsByEmailAndIdNot(request.getEmail(), id)) {
            throw new ConflictException(DUPLICATE_EMAIL_MESSAGE);
        }

        applyUpdateRequest(customer, request);
        CustomerEntity saved = saveHandlingDuplicateEmail(customer);
        CustomerDto customerDto = toDto(saved);
        eventPublisher.publishEvent(CustomerChangedEvent.updated(customerDto));
        return customerDto;
    }

    private CustomerEntity saveHandlingDuplicateEmail(CustomerEntity customer) {
        try {
            return customerRepository.saveAndFlush(customer);
        } catch (DataIntegrityViolationException ex) {
            if (isDuplicateEmailViolation(ex)) {
                throw new ConflictException(DUPLICATE_EMAIL_MESSAGE);
            }
            throw ex;
        }
    }

    private boolean isDuplicateEmailViolation(DataIntegrityViolationException ex) {
        Throwable cause = ex.getMostSpecificCause();
        String text = cause == null ? ex.getMessage() : cause.getMessage();
        if (text == null) {
            return false;
        }
        String normalized = text.toLowerCase(Locale.ENGLISH);
        return normalized.contains("ux_customers_email")
                || (normalized.contains("customers")
                && normalized.contains("email")
                && (normalized.contains("duplicate") || normalized.contains("unique")));
    }

    private void applyCreateRequest(CustomerEntity customer, CreateCustomerRequest request) {
        customer.setName(request.getName());
        customer.setEmail(request.getEmail());
        customer.setPhone(request.getPhone());
        customer.setStatus(request.getStatus());
    }

    private void applyUpdateRequest(CustomerEntity customer, UpdateCustomerRequest request) {
        customer.setName(request.getName());
        customer.setEmail(request.getEmail());
        customer.setPhone(request.getPhone());
        customer.setStatus(request.getStatus());
    }

    private CustomerDto toDto(CustomerEntity customer) {
        CustomerDto dto = new CustomerDto();
        dto.setId(customer.getId());
        dto.setName(customer.getName());
        dto.setEmail(customer.getEmail());
        dto.setPhone(customer.getPhone());
        dto.setStatus(customer.getStatus());
        dto.setCreatedAt(customer.getCreatedAt());
        dto.setUpdatedAt(customer.getUpdatedAt());
        dto.setVersion(customer.getVersion());
        return dto;
    }
}
