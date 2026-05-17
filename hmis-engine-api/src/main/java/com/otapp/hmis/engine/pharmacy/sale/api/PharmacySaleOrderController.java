package com.otapp.hmis.engine.pharmacy.sale.api;

import com.otapp.hmis.engine.common.api.PageResponse;
import com.otapp.hmis.engine.pharmacy.sale.application.PharmacySaleOrderDtos.CancelSaleRequest;
import com.otapp.hmis.engine.pharmacy.sale.application.PharmacySaleOrderDtos.CreatePharmacySaleOrderRequest;
import com.otapp.hmis.engine.pharmacy.sale.application.PharmacySaleOrderDtos.PharmacySaleOrderDto;
import com.otapp.hmis.engine.pharmacy.sale.application.PharmacySaleOrderDtos.PharmacySaleOrderSummary;
import com.otapp.hmis.engine.pharmacy.sale.application.PharmacySaleOrderDtos.RejectLineRequest;
import com.otapp.hmis.engine.pharmacy.sale.application.PharmacySaleOrderService;
import com.otapp.hmis.engine.pharmacy.sale.domain.PharmacySaleOrderStatus;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

@Tag(name = "Pharmacy sales")
@RestController
@RequestMapping("/pharmacy/sales")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('PHARMACY_ACCESS')")
public class PharmacySaleOrderController {

    private final PharmacySaleOrderService saleService;

    @PostMapping
    public ResponseEntity<PharmacySaleOrderDto> create(@Valid @RequestBody CreatePharmacySaleOrderRequest request) {
        PharmacySaleOrderDto created = saleService.create(request);
        URI loc = UriComponentsBuilder.fromPath("/pharmacy/sales/uid/{saleUid}")
                .buildAndExpand(created.uid()).toUri();
        return ResponseEntity.created(loc).body(created);
    }

    @GetMapping
    public ResponseEntity<PageResponse<PharmacySaleOrderSummary>> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) PharmacySaleOrderStatus status,
            @RequestParam(required = false) String pharmacyUid,
            @RequestParam(required = false) String patientUid,
            Pageable pageable) {
        return ResponseEntity.ok(saleService.search(query, status, pharmacyUid, patientUid, pageable));
    }

    @GetMapping("/uid/{saleUid}")
    public ResponseEntity<PharmacySaleOrderDto> findByUid(@PathVariable String saleUid) {
        return ResponseEntity.ok(saleService.findByUid(saleUid));
    }

    @PostMapping("/uid/{saleUid}/cancel")
    public ResponseEntity<PharmacySaleOrderDto> cancel(@PathVariable String saleUid,
                                                       @Valid @RequestBody(required = false) CancelSaleRequest request) {
        return ResponseEntity.ok(saleService.cancelSale(saleUid, request));
    }

    @PostMapping("/uid/{saleUid}/lines/uid/{lineUid}/accept")
    public ResponseEntity<PharmacySaleOrderDto> acceptLine(@PathVariable String saleUid,
                                                           @PathVariable String lineUid) {
        return ResponseEntity.ok(saleService.accept(saleUid, lineUid));
    }

    @PostMapping("/uid/{saleUid}/lines/uid/{lineUid}/hold")
    public ResponseEntity<PharmacySaleOrderDto> holdLine(@PathVariable String saleUid,
                                                         @PathVariable String lineUid) {
        return ResponseEntity.ok(saleService.hold(saleUid, lineUid));
    }

    @PostMapping("/uid/{saleUid}/lines/uid/{lineUid}/verify")
    public ResponseEntity<PharmacySaleOrderDto> verifyLine(@PathVariable String saleUid,
                                                           @PathVariable String lineUid) {
        return ResponseEntity.ok(saleService.verify(saleUid, lineUid));
    }

    @PostMapping("/uid/{saleUid}/lines/uid/{lineUid}/approve")
    public ResponseEntity<PharmacySaleOrderDto> approveLine(@PathVariable String saleUid,
                                                            @PathVariable String lineUid) {
        return ResponseEntity.ok(saleService.approve(saleUid, lineUid));
    }

    @PostMapping("/uid/{saleUid}/lines/uid/{lineUid}/reject")
    public ResponseEntity<PharmacySaleOrderDto> rejectLine(@PathVariable String saleUid,
                                                           @PathVariable String lineUid,
                                                           @Valid @RequestBody(required = false) RejectLineRequest request) {
        return ResponseEntity.ok(saleService.rejectLine(saleUid, lineUid, request));
    }

    @PostMapping("/uid/{saleUid}/lines/uid/{lineUid}/cancel")
    public ResponseEntity<PharmacySaleOrderDto> cancelLine(@PathVariable String saleUid,
                                                           @PathVariable String lineUid,
                                                           @Valid @RequestBody(required = false) RejectLineRequest request) {
        return ResponseEntity.ok(saleService.cancelLine(saleUid, lineUid, request));
    }
}
