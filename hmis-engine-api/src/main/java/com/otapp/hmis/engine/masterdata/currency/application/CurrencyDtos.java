package com.otapp.hmis.engine.masterdata.currency.application;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class CurrencyDtos {

    private CurrencyDtos() {}

    public record CurrencyDto(
            String uid,
            String code,
            String name,
            String symbol,
            boolean isDefault,
            boolean active,
            Instant createdAt,
            Instant updatedAt) {}

    public record CreateCurrencyRequest(
            @NotBlank @Pattern(regexp = "^[A-Za-z]{3}$") String code,
            @NotBlank @Size(max = 80) String name,
            @Size(max = 8) String symbol,
            boolean makeDefault) {}

    public record UpdateCurrencyRequest(
            @NotBlank @Size(max = 80) String name,
            @Size(max = 8) String symbol) {}
}
