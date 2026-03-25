package com.example.warehouse.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateSupplierRequest {

    @NotBlank
    @Size(max = 200)
    private String name;

    @Email
    @Size(max = 255)
    private String email;

    @Size(max = 30)
    private String phone;

    private String address;

    @Size(max = 50)
    private String status;

    private String notes;

    @AssertTrue(message = "at least one of email or phone is required")
    public boolean isContactProvided() {
        return hasText(email) || hasText(phone);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
