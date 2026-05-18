package com.otapp.hmis.engine.billing.creditnote.application;

import com.otapp.hmis.engine.billing.creditnote.domain.CreditNoteReason;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

public final class CreditNoteDtos {

    private CreditNoteDtos() {}

    public record CreditNoteDto(
            String uid,
            String noteNo,
            String invoiceUid,
            BigDecimal amount,
            String currency,
            CreditNoteReason reason,
            String description,
            String issuedByUsername,
            Instant issuedAt,
            Instant createdAt) {}

    public record CreateCreditNoteRequest(
            @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
            @NotNull CreditNoteReason reason,
            @Size(max = 500) String description) {}
}
