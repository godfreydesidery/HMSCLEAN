package com.otapp.hmis.engine.transfer.pharmacystorereturn.api;

import com.otapp.hmis.engine.common.api.PageResponse;
import com.otapp.hmis.engine.transfer.pharmacystorereturn.application.PharmacyStoreReturnDtos.CreateReturnRequest;
import com.otapp.hmis.engine.transfer.pharmacystorereturn.application.PharmacyStoreReturnDtos.ReasonRequest;
import com.otapp.hmis.engine.transfer.pharmacystorereturn.application.PharmacyStoreReturnDtos.ReturnDto;
import com.otapp.hmis.engine.transfer.pharmacystorereturn.application.PharmacyStoreReturnDtos.ReturnSummary;
import com.otapp.hmis.engine.transfer.pharmacystorereturn.application.PharmacyStoreReturnService;
import com.otapp.hmis.engine.transfer.pharmacystorereturn.domain.PharmacyStoreReturnStatus;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

@Tag(name = "Pharmacy→Store returns")
@RestController
@RequestMapping("/transfers/pharmacy-store/returns")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('PHARMACY_ACCESS','STORE_ACCESS')")
public class PharmacyStoreReturnController {

    private final PharmacyStoreReturnService service;

    @PostMapping
    public ResponseEntity<ReturnDto> create(@Valid @RequestBody CreateReturnRequest request) {
        ReturnDto created = service.create(request);
        URI loc = UriComponentsBuilder.fromPath("/transfers/pharmacy-store/returns/uid/{returnUid}")
                .buildAndExpand(created.uid()).toUri();
        return ResponseEntity.created(loc).body(created);
    }

    @GetMapping
    public ResponseEntity<PageResponse<ReturnSummary>> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) PharmacyStoreReturnStatus status,
            @RequestParam(required = false) String pharmacyUid,
            @RequestParam(required = false) String storeUid,
            Pageable pageable) {
        return ResponseEntity.ok(service.search(query, status, pharmacyUid, storeUid, pageable));
    }

    @GetMapping("/uid/{returnUid}")
    public ResponseEntity<ReturnDto> findByUid(@PathVariable String returnUid) {
        return ResponseEntity.ok(service.findByUid(returnUid));
    }

    @PostMapping("/uid/{returnUid}/submit")
    public ResponseEntity<ReturnDto> submit(@PathVariable String returnUid) {
        return ResponseEntity.ok(service.submit(returnUid));
    }

    @PostMapping("/uid/{returnUid}/complete")
    public ResponseEntity<ReturnDto> complete(@PathVariable String returnUid) {
        return ResponseEntity.ok(service.complete(returnUid));
    }

    @PostMapping("/uid/{returnUid}/reject")
    public ResponseEntity<ReturnDto> reject(@PathVariable String returnUid,
                                            @Valid @RequestBody(required = false) ReasonRequest request) {
        return ResponseEntity.ok(service.reject(returnUid, request));
    }

    @PostMapping("/uid/{returnUid}/cancel")
    public ResponseEntity<ReturnDto> cancel(@PathVariable String returnUid) {
        return ResponseEntity.ok(service.cancel(returnUid));
    }
}
