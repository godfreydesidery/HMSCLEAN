package com.otapp.hmis.engine.billing.invoice.api;

import com.otapp.hmis.engine.billing.invoice.application.InvoiceDtos.CancelInvoiceRequest;
import com.otapp.hmis.engine.billing.invoice.application.InvoiceDtos.InvoiceDto;
import com.otapp.hmis.engine.billing.invoice.application.InvoiceDtos.InvoiceSummary;
import com.otapp.hmis.engine.billing.invoice.application.InvoiceDtos.RecordPaymentRequest;
import com.otapp.hmis.engine.billing.invoice.application.InvoiceService;
import com.otapp.hmis.engine.billing.invoice.domain.InvoiceStatus;
import com.otapp.hmis.engine.common.api.PageResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Invoices")
@RestController
@RequestMapping
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('BILLING_ACCESS')")
public class InvoiceController {

    private final InvoiceService invoiceService;

    @GetMapping("/billing/invoices")
    public ResponseEntity<PageResponse<InvoiceSummary>> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) InvoiceStatus status,
            @RequestParam(required = false) String patientUid,
            Pageable pageable) {
        return ResponseEntity.ok(invoiceService.search(query, status, patientUid, pageable));
    }

    @GetMapping("/billing/invoices/{uid}")
    public ResponseEntity<InvoiceDto> findByUid(@PathVariable String uid) {
        return ResponseEntity.ok(invoiceService.findByUid(uid));
    }

    @GetMapping("/billing/consultations/{consultationUid}/invoice")
    public ResponseEntity<InvoiceDto> findForConsultation(@PathVariable String consultationUid) {
        InvoiceDto dto = invoiceService.findForConsultation(consultationUid);
        return dto == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(dto);
    }

    @PostMapping("/billing/consultations/{consultationUid}/invoice")
    public ResponseEntity<InvoiceDto> generate(@PathVariable String consultationUid) {
        return ResponseEntity.ok(invoiceService.generateForConsultation(consultationUid));
    }

    @PostMapping("/billing/invoices/{uid}/issue")
    public ResponseEntity<InvoiceDto> issue(@PathVariable String uid) {
        return ResponseEntity.ok(invoiceService.issue(uid));
    }

    @PostMapping("/billing/invoices/{uid}/cancel")
    public ResponseEntity<InvoiceDto> cancel(@PathVariable String uid,
                                             @Valid @RequestBody(required = false) CancelInvoiceRequest request) {
        return ResponseEntity.ok(invoiceService.cancel(uid, request));
    }

    @PostMapping("/billing/invoices/{uid}/payments")
    public ResponseEntity<InvoiceDto> recordPayment(@PathVariable String uid,
                                                    @Valid @RequestBody RecordPaymentRequest request) {
        return ResponseEntity.ok(invoiceService.recordPayment(uid, request));
    }
}
