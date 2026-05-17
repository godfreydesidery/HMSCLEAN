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
            String currency,
            String note,
            Instant createdAt,
            Instant updatedAt) {}

    /**
     * "Upsert" — set the price for a (plan, service) cell. Creates the row if
     * it doesn't exist, updates the amount/currency/note otherwise.
     */
    public record SetServicePriceRequest(
            @Size(min = 26, max = 26) String planUid,
            @NotNull ServiceKind kind,
            @NotBlank @Size(min = 26, max = 26) String serviceUid,
            @NotNull @DecimalMin(value = "0.00", inclusive = true) BigDecimal amount,
            @NotBlank @Pattern(regexp = "^[A-Z]{3}$") String currency,
            @Size(max = 255) String note) {}
}
