package com.otapp.hmis.engine.encounter.order.application;

import com.otapp.hmis.engine.encounter.order.domain.ClinicalOrderKind;
import com.otapp.hmis.engine.encounter.order.domain.ClinicalOrderStatus;
import com.otapp.hmis.engine.encounter.order.domain.OrderUrgency;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class ClinicalOrderDtos {

    private ClinicalOrderDtos() {}

    public record ClinicalOrderDto(
            String uid,
            String orderNo,
            String consultationUid,
            String patientUid,
            ClinicalOrderKind kind,
            String serviceUid,
            String serviceCode,
            String serviceName,
            ClinicalOrderStatus status,
            OrderUrgency urgency,
            Instant requestedAt,
            Instant completedAt,
            String instructions,
            String result,
            String cancelReason,
            Instant createdAt,
            Instant updatedAt) {}

    public record CreateOrderRequest(
            @NotNull ClinicalOrderKind kind,
            @NotBlank @Size(min = 26, max = 26) String serviceUid,
            @NotNull OrderUrgency urgency,
            @Size(max = 1000) String instructions) {}

    public record CompleteOrderRequest(
            @Size(max = 4000) String result) {}

    public record CancelOrderRequest(
            @Size(max = 255) String reason) {}
}
