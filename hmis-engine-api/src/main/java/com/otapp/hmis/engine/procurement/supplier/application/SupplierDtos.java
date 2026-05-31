package com.otapp.hmis.engine.procurement.supplier.application;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class SupplierDtos {

    private SupplierDtos() {}

    public record SupplierDto(
            String uid,
            String code,
            String name,
            String contactName,
            String phone,
            String email,
            String address,
            String taxId,
            String notes,
            boolean active,
            Instant createdAt,
            Instant updatedAt,
            // --- VAT / contract terms / bank-account block (legacy parity) ---
            String vrn,
            String termsOfContract,
            String bankName,
            String bankAccountName,
            String bankAccountNo) {}

    public record CreateSupplierRequest(
            @NotBlank @Size(max = 32) String code,
            @NotBlank @Size(max = 200) String name,
            @Size(max = 120) String contactName,
            @Size(max = 32) String phone,
            @Email @Size(max = 120) String email,
            @Size(max = 255) String address,
            @Size(max = 64) String taxId,
            @Size(max = 500) String notes,
            @Size(max = 32) String vrn,
            @Size(max = 1000) String termsOfContract,
            @Size(max = 120) String bankName,
            @Size(max = 200) String bankAccountName,
            @Size(max = 64) String bankAccountNo) {}

    public record UpdateSupplierRequest(
            @NotBlank @Size(max = 200) String name,
            @Size(max = 120) String contactName,
            @Size(max = 32) String phone,
            @Email @Size(max = 120) String email,
            @Size(max = 255) String address,
            @Size(max = 64) String taxId,
            @Size(max = 500) String notes,
            @Size(max = 32) String vrn,
            @Size(max = 1000) String termsOfContract,
            @Size(max = 120) String bankName,
            @Size(max = 200) String bankAccountName,
            @Size(max = 64) String bankAccountNo) {}
}
