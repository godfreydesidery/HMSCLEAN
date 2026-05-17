package com.otapp.hmis.engine.encounter.order.api;

import com.otapp.hmis.engine.encounter.order.application.ClinicalOrderDtos.CancelOrderRequest;
import com.otapp.hmis.engine.encounter.order.application.ClinicalOrderDtos.ClinicalOrderDto;
import com.otapp.hmis.engine.encounter.order.application.ClinicalOrderDtos.CompleteOrderRequest;
import com.otapp.hmis.engine.encounter.order.application.ClinicalOrderDtos.CreateOrderRequest;
import com.otapp.hmis.engine.encounter.order.application.ClinicalOrderService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
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

    @GetMapping("/encounters/consultations/{consultationUid}/orders")
    public ResponseEntity<List<ClinicalOrderDto>> list(@PathVariable String consultationUid) {
        return ResponseEntity.ok(orderService.listForConsultation(consultationUid));
    }

    @PostMapping("/encounters/consultations/{consultationUid}/orders")
    public ResponseEntity<ClinicalOrderDto> request(@PathVariable String consultationUid,
                                                    @Valid @RequestBody CreateOrderRequest request) {
        return ResponseEntity.ok(orderService.request(consultationUid, request));
    }

    @PostMapping("/encounters/orders/{uid}/start")
    public ResponseEntity<ClinicalOrderDto> markInProgress(@PathVariable String uid) {
        return ResponseEntity.ok(orderService.markInProgress(uid));
    }

    @PostMapping("/encounters/orders/{uid}/complete")
    public ResponseEntity<ClinicalOrderDto> complete(@PathVariable String uid,
                                                     @Valid @RequestBody(required = false) CompleteOrderRequest request) {
        return ResponseEntity.ok(orderService.complete(uid, request));
    }

    @PostMapping("/encounters/orders/{uid}/cancel")
    public ResponseEntity<ClinicalOrderDto> cancel(@PathVariable String uid,
                                                   @Valid @RequestBody(required = false) CancelOrderRequest request) {
        return ResponseEntity.ok(orderService.cancel(uid, request));
    }
}
