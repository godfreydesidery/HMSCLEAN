package com.otapp.hmis.engine.billing.refund.api;

import com.otapp.hmis.engine.billing.refund.application.RefundDtos.CreateRefundRequest;
import com.otapp.hmis.engine.billing.refund.application.RefundDtos.RefundDto;
import com.otapp.hmis.engine.billing.refund.application.RefundService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Refunds")
@RestController
@RequestMapping("/billing/invoices/uid/{invoiceUid}/refunds")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('BILLING_ACCESS')")
public class RefundController {

    private final RefundService service;

    @GetMapping
    public ResponseEntity<List<RefundDto>> list(@PathVariable String invoiceUid) {
        return ResponseEntity.ok(service.listForInvoice(invoiceUid));
    }

    @PostMapping
    public ResponseEntity<RefundDto> raise(@PathVariable String invoiceUid,
                                           @Valid @RequestBody CreateRefundRequest request) {
        return ResponseEntity.ok(service.raise(invoiceUid, request));
    }
}
