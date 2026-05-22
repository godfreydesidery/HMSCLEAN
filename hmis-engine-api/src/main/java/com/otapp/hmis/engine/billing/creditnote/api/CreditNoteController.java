package com.otapp.hmis.engine.billing.creditnote.api;

import com.otapp.hmis.engine.billing.creditnote.application.CreditNoteDtos.CreateCreditNoteRequest;
import com.otapp.hmis.engine.billing.creditnote.application.CreditNoteDtos.CreditNoteDto;
import com.otapp.hmis.engine.billing.creditnote.application.CreditNoteService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Credit notes")
@RestController
@RequestMapping("/billing/invoices/uid/{invoiceUid}/credit-notes")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('BILLING_ACCESS')")
public class CreditNoteController {

    private final CreditNoteService service;

    @GetMapping
    public ResponseEntity<List<CreditNoteDto>> list(@PathVariable String invoiceUid) {
        return ResponseEntity.ok(service.listForInvoice(invoiceUid));
    }

    @PostMapping
    public ResponseEntity<CreditNoteDto> raise(@PathVariable String invoiceUid,
                                               @Valid @RequestBody CreateCreditNoteRequest request) {
        return ResponseEntity.ok(service.raise(invoiceUid, request));
    }
}
