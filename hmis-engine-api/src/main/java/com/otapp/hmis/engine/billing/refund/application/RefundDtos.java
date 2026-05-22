package com.otapp.hmis.engine.billing.refund.application;

import com.otapp.hmis.engine.billing.payment.domain.PaymentMethod;
import com.otapp.hmis.engine.billing.refund.domain.RefundReason;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

public final class RefundDtos {

    private RefundDtos() {}

    public record RefundDto(
            String uid,
            String refundNo,
            String invoiceUid,
            BigDecimal amount,
            String currency,
            PaymentMethod method,
            RefundReason reason,
            String description,
            String reference,
            String refundedByUsername,
            Instant refundedAt,
            Instant createdAt) {}

    public record CreateRefundRequest(
            @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
            @NotNull PaymentMethod method,
            @NotNull RefundReason reason,
            @Size(max = 500) String description,
            @Size(max = 80)  String reference) {}
}
