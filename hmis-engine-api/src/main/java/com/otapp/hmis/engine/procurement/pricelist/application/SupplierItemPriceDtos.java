package com.otapp.hmis.engine.procurement.pricelist.application;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public final class SupplierItemPriceDtos {

    private SupplierItemPriceDtos() {}

    public record SupplierItemPriceDto(
            String uid,
            String supplierUid,
            String supplierName,
            String medicineUid,
            String medicineCode,
            String medicineName,
            String medicineStrength,
            BigDecimal unitPrice,
            String currency,
            LocalDate validFrom,
            LocalDate validTo,
            boolean active,
            boolean currentlyValid,
            String notes,
            Instant createdAt,
            Instant updatedAt) {}

    public record CreateSupplierItemPriceRequest(
            @NotBlank @Size(min = 26, max = 26) String medicineUid,
            @NotNull @DecimalMin(value = "0.01") BigDecimal unitPrice,
            @Size(min = 3, max = 3) String currency,
            @NotNull LocalDate validFrom,
            LocalDate validTo,
            @Size(max = 500) String notes) {}

    public record UpdateSupplierItemPriceRequest(
            @NotNull @DecimalMin(value = "0.01") BigDecimal unitPrice,
            @Size(min = 3, max = 3) String currency,
            LocalDate validTo,
            @Size(max = 500) String notes) {}
}
