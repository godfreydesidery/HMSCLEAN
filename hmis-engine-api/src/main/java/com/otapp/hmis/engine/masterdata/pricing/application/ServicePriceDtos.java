package com.otapp.hmis.engine.masterdata.pricing.application;

import com.otapp.hmis.engine.masterdata.pricing.domain.ServiceKind;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

public final class ServicePriceDtos {

    private ServicePriceDtos() {}

    public record ServicePriceDto(
            String uid,
            String planUid,
            String planName,
            ServiceKind kind,
            String serviceUid,
            String serviceName,
            BigDecimal amount,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            String currency,
            String note,
            Instant createdAt,
            Instant updatedAt) {}

    /**
     * Create a new price for a (plan, service, currency) cell. Fails with a
     * conflict if that exact cell already exists — callers must edit the
     * existing row instead of overwriting it. {@code minAmount}/{@code maxAmount}
     * are optional; when present they bound the amount and any negotiated
     * per-line override.
     */
    public record SetServicePriceRequest(
            @Size(min = 26, max = 26) String planUid,
            @NotNull ServiceKind kind,
            // Usually a 26-char catalogue uid; the REGISTRATION singleton uses the
            // sentinel "DEFAULT". The service validates existence per kind.
            @NotBlank @Size(max = 26) String serviceUid,
            @NotNull @DecimalMin(value = "0.00", inclusive = true) BigDecimal amount,
            @DecimalMin(value = "0.00", inclusive = true) BigDecimal minAmount,
            @DecimalMin(value = "0.00", inclusive = true) BigDecimal maxAmount,
            @NotBlank @Pattern(regexp = "^[A-Z]{3}$") String currency,
            @Size(max = 255) String note) {}

    /**
     * Update an existing price (identified by its uid). Only the amount, band
     * and note are mutable — the key (payer, service, currency) is immutable;
     * to change those, delete the row and create a new one.
     */
    public record UpdateServicePriceRequest(
            @NotNull @DecimalMin(value = "0.00", inclusive = true) BigDecimal amount,
            @DecimalMin(value = "0.00", inclusive = true) BigDecimal minAmount,
            @DecimalMin(value = "0.00", inclusive = true) BigDecimal maxAmount,
            @Size(max = 255) String note) {}
}
