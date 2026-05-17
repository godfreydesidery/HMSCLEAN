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

    @GetMapping("/billing/invoices/uid/{invoiceUid}")
    public ResponseEntity<InvoiceDto> findByUid(@PathVariable String invoiceUid) {
        return ResponseEntity.ok(invoiceService.findByUid(invoiceUid));
    }

    @GetMapping("/billing/consultations/uid/{consultationUid}/invoice")
    public ResponseEntity<InvoiceDto> findForConsultation(@PathVariable String consultationUid) {
        InvoiceDto dto = invoiceService.findForConsultation(consultationUid);
        return dto == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(dto);
    }

    @PostMapping("/billing/consultations/uid/{consultationUid}/invoice")
    public ResponseEntity<InvoiceDto> generate(@PathVariable String consultationUid) {
        return ResponseEntity.ok(invoiceService.generateForConsultation(consultationUid));
    }

    @GetMapping("/billing/admissions/uid/{admissionUid}/invoice")
    public ResponseEntity<InvoiceDto> findForAdmission(@PathVariable String admissionUid) {
        InvoiceDto dto = invoiceService.findForAdmission(admissionUid);
        return dto == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(dto);
    }

    @PostMapping("/billing/admissions/uid/{admissionUid}/invoice")
    public ResponseEntity<InvoiceDto> generateForAdmission(@PathVariable String admissionUid) {
        return ResponseEntity.ok(invoiceService.generateForAdmission(admissionUid));
    }

    @GetMapping("/billing/patients/uid/{patientUid}/outsider-invoice")
    public ResponseEntity<InvoiceDto> findCurrentOutsiderDraft(@PathVariable String patientUid) {
        InvoiceDto dto = invoiceService.findCurrentDraftForOutsider(patientUid);
        return dto == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(dto);
    }

    @PostMapping("/billing/patients/uid/{patientUid}/outsider-invoice")
    public ResponseEntity<InvoiceDto> generateForOutsider(@PathVariable String patientUid) {
        return ResponseEntity.ok(invoiceService.generateForOutsider(patientUid));
    }

    @PostMapping("/billing/invoices/uid/{invoiceUid}/issue")
    public ResponseEntity<InvoiceDto> issue(@PathVariable String invoiceUid) {
        return ResponseEntity.ok(invoiceService.issue(invoiceUid));
    }

    @PostMapping("/billing/invoices/uid/{invoiceUid}/cancel")
    public ResponseEntity<InvoiceDto> cancel(@PathVariable String invoiceUid,
                                             @Valid @RequestBody(required = false) CancelInvoiceRequest request) {
        return ResponseEntity.ok(invoiceService.cancel(invoiceUid, request));
    }

    @PostMapping("/billing/invoices/uid/{invoiceUid}/payments")
    public ResponseEntity<InvoiceDto> recordPayment(@PathVariable String invoiceUid,
                                                    @Valid @RequestBody RecordPaymentRequest request) {
        return ResponseEntity.ok(invoiceService.recordPayment(invoiceUid, request));
    }
}
