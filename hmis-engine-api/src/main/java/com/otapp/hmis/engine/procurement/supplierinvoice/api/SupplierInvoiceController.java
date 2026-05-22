package com.otapp.hmis.engine.procurement.supplierinvoice.api;

import com.otapp.hmis.engine.common.api.PageResponse;
import com.otapp.hmis.engine.procurement.supplierinvoice.application.SupplierInvoiceDtos.CreateSupplierInvoiceRequest;
import com.otapp.hmis.engine.procurement.supplierinvoice.application.SupplierInvoiceDtos.MarkPaidRequest;
import com.otapp.hmis.engine.procurement.supplierinvoice.application.SupplierInvoiceDtos.ReasonRequest;
import com.otapp.hmis.engine.procurement.supplierinvoice.application.SupplierInvoiceDtos.SupplierInvoiceDto;
import com.otapp.hmis.engine.procurement.supplierinvoice.application.SupplierInvoiceDtos.SupplierInvoiceSummary;
import com.otapp.hmis.engine.procurement.supplierinvoice.application.SupplierInvoiceService;
import com.otapp.hmis.engine.procurement.supplierinvoice.domain.SupplierInvoiceStatus;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

@Tag(name = "Supplier invoices (3-way match)")
@RestController
@RequestMapping("/procurement/supplier-invoices")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('PROCUREMENT_ACCESS')")
public class SupplierInvoiceController {

    private final SupplierInvoiceService service;

    @PostMapping
    public ResponseEntity<SupplierInvoiceDto> create(@Valid @RequestBody CreateSupplierInvoiceRequest request) {
        SupplierInvoiceDto created = service.create(request);
        URI loc = UriComponentsBuilder.fromPath("/procurement/supplier-invoices/uid/{invoiceUid}")
                .buildAndExpand(created.uid()).toUri();
        return ResponseEntity.created(loc).body(created);
    }

    @GetMapping
    public ResponseEntity<PageResponse<SupplierInvoiceSummary>> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) SupplierInvoiceStatus status,
            @RequestParam(required = false) String supplierUid,
            @RequestParam(required = false) String orderUid,
            Pageable pageable) {
        return ResponseEntity.ok(service.search(query, status, supplierUid, orderUid, pageable));
    }

    @GetMapping("/uid/{invoiceUid}")
    public ResponseEntity<SupplierInvoiceDto> findByUid(@PathVariable String invoiceUid) {
        return ResponseEntity.ok(service.findByUid(invoiceUid));
    }

    @PostMapping("/uid/{invoiceUid}/submit")
    public ResponseEntity<SupplierInvoiceDto> submit(@PathVariable String invoiceUid) {
        return ResponseEntity.ok(service.submit(invoiceUid));
    }

    @PostMapping("/uid/{invoiceUid}/approve")
    public ResponseEntity<SupplierInvoiceDto> approve(@PathVariable String invoiceUid) {
        return ResponseEntity.ok(service.approve(invoiceUid));
    }

    @PostMapping("/uid/{invoiceUid}/pay")
    public ResponseEntity<SupplierInvoiceDto> markPaid(@PathVariable String invoiceUid,
                                                       @Valid @RequestBody MarkPaidRequest request) {
        return ResponseEntity.ok(service.markPaid(invoiceUid, request));
    }

    @PostMapping("/uid/{invoiceUid}/reject")
    public ResponseEntity<SupplierInvoiceDto> reject(@PathVariable String invoiceUid,
                                                     @Valid @RequestBody(required = false) ReasonRequest request) {
        return ResponseEntity.ok(service.reject(invoiceUid, request));
    }

    @PostMapping("/uid/{invoiceUid}/cancel")
    public ResponseEntity<SupplierInvoiceDto> cancel(@PathVariable String invoiceUid) {
        return ResponseEntity.ok(service.cancel(invoiceUid));
    }
}
