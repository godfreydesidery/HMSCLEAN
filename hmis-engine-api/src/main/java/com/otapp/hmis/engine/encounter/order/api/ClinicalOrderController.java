package com.otapp.hmis.engine.encounter.order.api;

import com.otapp.hmis.engine.common.api.PageResponse;
import com.otapp.hmis.engine.encounter.order.application.ClinicalOrderDtos.CancelOrderRequest;
import com.otapp.hmis.engine.encounter.order.application.ClinicalOrderDtos.ClinicalOrderDto;
import com.otapp.hmis.engine.encounter.order.application.ClinicalOrderDtos.CompleteOrderRequest;
import com.otapp.hmis.engine.encounter.order.application.ClinicalOrderDtos.CreateOrderRequest;
import com.otapp.hmis.engine.encounter.order.application.ClinicalOrderDtos.OrderWorklistDto;
import com.otapp.hmis.engine.encounter.order.application.ClinicalOrderDtos.ScheduleOrderRequest;
import com.otapp.hmis.engine.encounter.order.application.ClinicalOrderService;
import com.otapp.hmis.engine.encounter.order.domain.ClinicalOrderKind;
import com.otapp.hmis.engine.encounter.order.domain.ClinicalOrderStatus;
import com.otapp.hmis.engine.patient.domain.PatientClassScope;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Clinical orders")
@RestController
@RequestMapping
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ENCOUNTER_ACCESS')")
public class ClinicalOrderController {

    private final ClinicalOrderService orderService;

    @GetMapping("/encounters/orders")
    public ResponseEntity<PageResponse<OrderWorklistDto>> worklist(
            @RequestParam(required = false) ClinicalOrderKind kind,
            @RequestParam(required = false) ClinicalOrderStatus status,
            @RequestParam(required = false) PatientClassScope patientClass,
            @RequestParam(defaultValue = "false") boolean settledOnly,
            Pageable pageable) {
        return ResponseEntity.ok(orderService.searchWorklist(kind, status, patientClass, settledOnly, pageable));
    }

    @GetMapping("/encounters/consultations/uid/{consultationUid}/orders")
    public ResponseEntity<List<ClinicalOrderDto>> list(@PathVariable String consultationUid) {
        return ResponseEntity.ok(orderService.listForConsultation(consultationUid));
    }

    @PostMapping("/encounters/consultations/uid/{consultationUid}/orders")
    public ResponseEntity<ClinicalOrderDto> request(@PathVariable String consultationUid,
                                                    @Valid @RequestBody CreateOrderRequest request) {
        return ResponseEntity.ok(orderService.request(consultationUid, request));
    }

    @GetMapping("/encounters/patients/uid/{patientUid}/outsider-orders")
    public ResponseEntity<List<ClinicalOrderDto>> listOutsiderForPatient(@PathVariable String patientUid) {
        return ResponseEntity.ok(orderService.listOutsiderForPatient(patientUid));
    }

    @PostMapping("/encounters/patients/uid/{patientUid}/outsider-orders")
    public ResponseEntity<ClinicalOrderDto> requestForOutsider(@PathVariable String patientUid,
                                                               @Valid @RequestBody CreateOrderRequest request) {
        return ResponseEntity.ok(orderService.requestForOutsider(patientUid, request));
    }

    @PostMapping("/encounters/orders/uid/{orderUid}/accept")
    public ResponseEntity<ClinicalOrderDto> accept(@PathVariable String orderUid) {
        return ResponseEntity.ok(orderService.accept(orderUid));
    }

    @PostMapping("/encounters/orders/uid/{orderUid}/approve")
    public ResponseEntity<ClinicalOrderDto> approve(@PathVariable String orderUid) {
        return ResponseEntity.ok(orderService.approve(orderUid));
    }

    @PostMapping("/encounters/orders/uid/{orderUid}/start")
    public ResponseEntity<ClinicalOrderDto> markInProgress(@PathVariable String orderUid) {
        return ResponseEntity.ok(orderService.markInProgress(orderUid));
    }

    @PostMapping("/encounters/orders/uid/{orderUid}/complete")
    public ResponseEntity<ClinicalOrderDto> complete(@PathVariable String orderUid,
                                                     @Valid @RequestBody(required = false) CompleteOrderRequest request) {
        return ResponseEntity.ok(orderService.complete(orderUid, request));
    }

    @PostMapping("/encounters/orders/uid/{orderUid}/cancel")
    public ResponseEntity<ClinicalOrderDto> cancel(@PathVariable String orderUid,
                                                   @Valid @RequestBody(required = false) CancelOrderRequest request) {
        return ResponseEntity.ok(orderService.cancel(orderUid, request));
    }

    @PostMapping("/encounters/orders/uid/{orderUid}/schedule")
    public ResponseEntity<ClinicalOrderDto> schedule(@PathVariable String orderUid,
                                                     @Valid @RequestBody ScheduleOrderRequest request) {
        return ResponseEntity.ok(orderService.schedule(orderUid, request));
    }
}
