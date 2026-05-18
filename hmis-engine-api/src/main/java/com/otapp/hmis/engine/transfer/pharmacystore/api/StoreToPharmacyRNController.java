package com.otapp.hmis.engine.transfer.pharmacystore.api;

import com.otapp.hmis.engine.common.api.PageResponse;
import com.otapp.hmis.engine.transfer.pharmacystore.application.PharmacyStoreTransferDtos.CreateRNRequest;
import com.otapp.hmis.engine.transfer.pharmacystore.application.PharmacyStoreTransferDtos.RNDto;
import com.otapp.hmis.engine.transfer.pharmacystore.application.PharmacyStoreTransferDtos.RNSummary;
import com.otapp.hmis.engine.transfer.pharmacystore.application.PharmacyStoreTransferService;
import com.otapp.hmis.engine.transfer.pharmacystore.domain.ReceiveNoteStatus;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

@Tag(name = "Pharmacy↔Store transfers — RN (Receive Note)")
@RestController
@RequestMapping("/transfers/pharmacy-store/rn")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('PHARMACY_ACCESS')")
public class StoreToPharmacyRNController {

    private final PharmacyStoreTransferService transferService;

    @PostMapping
    public ResponseEntity<RNDto> create(@Valid @RequestBody CreateRNRequest request) {
        RNDto created = transferService.createRN(request);
        URI loc = UriComponentsBuilder.fromPath("/transfers/pharmacy-store/rn/uid/{rnUid}")
                .buildAndExpand(created.uid()).toUri();
        return ResponseEntity.created(loc).body(created);
    }

    @GetMapping
    public ResponseEntity<PageResponse<RNSummary>> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) ReceiveNoteStatus status,
            @RequestParam(required = false) String pharmacyUid,
            @RequestParam(required = false) String storeUid,
            @RequestParam(required = false) String toUid,
            Pageable pageable) {
        return ResponseEntity.ok(
                transferService.searchRNs(query, status, pharmacyUid, storeUid, toUid, pageable));
    }

    @GetMapping("/uid/{rnUid}")
    public ResponseEntity<RNDto> findByUid(@PathVariable String rnUid) {
        return ResponseEntity.ok(transferService.findRN(rnUid));
    }
}
