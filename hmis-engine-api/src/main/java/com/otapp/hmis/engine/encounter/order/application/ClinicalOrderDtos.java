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
            String theatreUid,
            String theatreName,
            Instant scheduledAt,
            String scheduledByUsername,
            Instant createdAt,
            Instant updatedAt) {}

    /**
     * Row of the cross-patient Orders &amp; Results worklist. Carries the
     * resolved patient + service labels so the list renders without N extra
     * fetches on the client.
     */
    public record OrderWorklistDto(
            String uid,
            String orderNo,
            ClinicalOrderKind kind,
            String serviceCode,
            String serviceName,
            ClinicalOrderStatus status,
            OrderUrgency urgency,
            Instant requestedAt,
            Instant completedAt,
            String patientUid,
            String patientNo,
            String patientName,
            com.otapp.hmis.engine.patient.domain.PatientClassScope patientClass,
            boolean settled,
            String consultationUid) {}

    public record CreateOrderRequest(
            @NotNull ClinicalOrderKind kind,
            @NotBlank @Size(min = 26, max = 26) String serviceUid,
            @NotNull OrderUrgency urgency,
            @Size(max = 1000) String instructions) {}

    public record CompleteOrderRequest(
            @Size(max = 4000) String result) {}

    public record CancelOrderRequest(
            @Size(max = 255) String reason) {}

    /**
     * Books a theatre + time slot for a PROCEDURE order. Only valid for
     * procedure orders that are not yet COMPLETED or CANCELLED.
     */
    public record ScheduleOrderRequest(
            @NotBlank @Size(min = 26, max = 26) String theatreUid,
            @NotNull Instant scheduledAt) {}
}
