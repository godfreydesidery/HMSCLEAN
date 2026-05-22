package com.otapp.hmis.engine.transfer.pharmacypharmacy.api;

import com.otapp.hmis.engine.common.api.PageResponse;
import com.otapp.hmis.engine.transfer.common.domain.ReceiveNoteStatus;
import com.otapp.hmis.engine.transfer.pharmacypharmacy.application.PharmacyPharmacyTransferDtos.CreateRNRequest;
import com.otapp.hmis.engine.transfer.pharmacypharmacy.application.PharmacyPharmacyTransferDtos.RNDto;
import com.otapp.hmis.engine.transfer.pharmacypharmacy.application.PharmacyPharmacyTransferDtos.RNSummary;
import com.otapp.hmis.engine.transfer.pharmacypharmacy.application.PharmacyPharmacyTransferService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

@Tag(name = "Pharmacy↔Pharmacy transfers — RN (Receive Note)")
@RestController
@RequestMapping("/transfers/pharmacy-pharmacy/rn")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('PHARMACY_ACCESS')")
public class PharmacyToPharmacyRNController {

    private final PharmacyPharmacyTransferService transferService;

    @PostMapping
    public ResponseEntity<RNDto> create(@Valid @RequestBody CreateRNRequest request) {
        RNDto created = transferService.createRN(request);
        URI loc = UriComponentsBuilder.fromPath("/transfers/pharmacy-pharmacy/rn/uid/{rnUid}")
                .buildAndExpand(created.uid()).toUri();
        return ResponseEntity.created(loc).body(created);
    }

    @GetMapping
    public ResponseEntity<PageResponse<RNSummary>> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) ReceiveNoteStatus status,
            @RequestParam(required = false) String requestingPharmacyUid,
            @RequestParam(required = false) String deliveringPharmacyUid,
            @RequestParam(required = false) String toUid,
            Pageable pageable) {
        return ResponseEntity.ok(transferService.searchRNs(
                query, status, requestingPharmacyUid, deliveringPharmacyUid, toUid, pageable));
    }

    @GetMapping("/uid/{rnUid}")
    public ResponseEntity<RNDto> findByUid(@PathVariable String rnUid) {
        return ResponseEntity.ok(transferService.findRN(rnUid));
    }
}
