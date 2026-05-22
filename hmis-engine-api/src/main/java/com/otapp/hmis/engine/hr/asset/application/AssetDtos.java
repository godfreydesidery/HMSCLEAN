package com.otapp.hmis.engine.hr.asset.application;

import com.otapp.hmis.engine.hr.asset.domain.AssetStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public final class AssetDtos {

    private AssetDtos() {}

    public record AssetDto(
            String uid,
            String tag,
            String name,
            String category,
            String location,
            String description,
            String serialNo,
            String manufacturer,
            String model,
            LocalDate acquisitionDate,
            BigDecimal acquisitionCost,
            String currency,
            String custodianUsername,
            AssetStatus status,
            LocalDate retiredAt,
            String retiredReason,
            Instant createdAt,
            Instant updatedAt) {}

    public record CreateAssetRequest(
            @NotBlank @Size(max = 64)  String tag,
            @NotBlank @Size(max = 160) String name,
            @Size(max = 80)  String category,
            @Size(max = 120) String location,
            @Size(max = 500) String description,
            @Size(max = 120) String serialNo,
            @Size(max = 120) String manufacturer,
            @Size(max = 120) String model,
            LocalDate acquisitionDate,
            @DecimalMin(value = "0.00") BigDecimal acquisitionCost,
            @Pattern(regexp = "^[A-Z]{3}$") String currency,
            @Size(max = 64) String custodianUsername) {}

    public record UpdateAssetRequest(
            @NotBlank @Size(max = 160) String name,
            @Size(max = 80)  String category,
            @Size(max = 120) String location,
            @Size(max = 500) String description,
            @Size(max = 120) String serialNo,
            @Size(max = 120) String manufacturer,
            @Size(max = 120) String model,
            LocalDate acquisitionDate,
            @DecimalMin(value = "0.00") BigDecimal acquisitionCost,
            @Pattern(regexp = "^[A-Z]{3}$") String currency,
            @Size(max = 64) String custodianUsername) {}

    public record RetireAssetRequest(
            @NotNull AssetStatus target,
            @NotNull LocalDate date,
            @Size(max = 500) String reason) {}
}
