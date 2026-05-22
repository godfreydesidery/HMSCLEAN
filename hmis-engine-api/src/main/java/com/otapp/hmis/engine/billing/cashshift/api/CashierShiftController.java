package com.otapp.hmis.engine.billing.cashshift.api;

import com.otapp.hmis.engine.billing.cashshift.application.CashierShiftDtos.CashierShiftDto;
import com.otapp.hmis.engine.billing.cashshift.application.CashierShiftDtos.CloseShiftRequest;
import com.otapp.hmis.engine.billing.cashshift.application.CashierShiftDtos.OpenShiftRequest;
import com.otapp.hmis.engine.billing.cashshift.application.CashierShiftService;
import com.otapp.hmis.engine.billing.cashshift.domain.CashierShiftStatus;
import com.otapp.hmis.engine.common.api.PageResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Cashier shifts")
@RestController
@RequestMapping("/billing/cashier-shifts")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('BILLING_ACCESS')")
public class CashierShiftController {

    private final CashierShiftService service;

    @PostMapping("/open")
    public ResponseEntity<CashierShiftDto> open(@Valid @RequestBody OpenShiftRequest request) {
        return ResponseEntity.ok(service.open(request));
    }

    @PostMapping("/close")
    public ResponseEntity<CashierShiftDto> close(@Valid @RequestBody CloseShiftRequest request) {
        return ResponseEntity.ok(service.close(request));
    }

    @GetMapping("/me")
    public ResponseEntity<CashierShiftDto> currentOpen() {
        return ResponseEntity.ok(service.currentOpen());
    }

    @GetMapping("/uid/{shiftUid}")
    public ResponseEntity<CashierShiftDto> findByUid(@PathVariable String shiftUid) {
        return ResponseEntity.ok(service.findByUid(shiftUid));
    }

    @GetMapping
    public ResponseEntity<PageResponse<CashierShiftDto>> search(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) CashierShiftStatus status,
            Pageable pageable) {
        return ResponseEntity.ok(service.search(username, status, pageable));
    }
}
