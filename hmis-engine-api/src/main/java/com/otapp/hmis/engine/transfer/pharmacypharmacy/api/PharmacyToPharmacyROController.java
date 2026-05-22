package com.otapp.hmis.engine.transfer.pharmacypharmacy.api;

import com.otapp.hmis.engine.common.api.PageResponse;
import com.otapp.hmis.engine.transfer.common.domain.TransferDocStatus;
import com.otapp.hmis.engine.transfer.pharmacypharmacy.application.PharmacyPharmacyTransferDtos.CreateRORequest;
import com.otapp.hmis.engine.transfer.pharmacypharmacy.application.PharmacyPharmacyTransferDtos.RODto;
import com.otapp.hmis.engine.transfer.pharmacypharmacy.application.PharmacyPharmacyTransferDtos.ROSummary;
import com.otapp.hmis.engine.transfer.pharmacypharmacy.application.PharmacyPharmacyTransferDtos.ReasonRequest;
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

@Tag(name = "Pharmacy↔Pharmacy transfers — RO (Request Order)")
@RestController
@RequestMapping("/transfers/pharmacy-pharmacy/ro")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('PHARMACY_ACCESS')")
public class PharmacyToPharmacyROController {

    private final PharmacyPharmacyTransferService transferService;

    @PostMapping
    public ResponseEntity<RODto> create(@Valid @RequestBody CreateRORequest request) {
        RODto created = transferService.createRO(request);
        URI loc = UriComponentsBuilder.fromPath("/transfers/pharmacy-pharmacy/ro/uid/{roUid}")
                .buildAndExpand(created.uid()).toUri();
        return ResponseEntity.created(loc).body(created);
    }

    @GetMapping
    public ResponseEntity<PageResponse<ROSummary>> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) TransferDocStatus status,
            @RequestParam(required = false) String requestingPharmacyUid,
            @RequestParam(required = false) String deliveringPharmacyUid,
            Pageable pageable) {
        return ResponseEntity.ok(transferService.searchROs(
                query, status, requestingPharmacyUid, deliveringPharmacyUid, pageable));
    }

    @GetMapping("/uid/{roUid}")
    public ResponseEntity<RODto> findByUid(@PathVariable String roUid) {
        return ResponseEntity.ok(transferService.findRO(roUid));
    }

    @PostMapping("/uid/{roUid}/verify")
    public ResponseEntity<RODto> verify(@PathVariable String roUid) {
        return ResponseEntity.ok(transferService.verifyRO(roUid));
    }

    @PostMapping("/uid/{roUid}/approve")
    public ResponseEntity<RODto> approve(@PathVariable String roUid) {
        return ResponseEntity.ok(transferService.approveRO(roUid));
    }

    @PostMapping("/uid/{roUid}/submit")
    public ResponseEntity<RODto> submit(@PathVariable String roUid) {
        return ResponseEntity.ok(transferService.submitRO(roUid));
    }

    @PostMapping("/uid/{roUid}/reject")
    public ResponseEntity<RODto> reject(@PathVariable String roUid,
                                        @Valid @RequestBody(required = false) ReasonRequest request) {
        return ResponseEntity.ok(transferService.rejectRO(roUid, request == null ? null : request.reason()));
    }

    @PostMapping("/uid/{roUid}/return")
    public ResponseEntity<RODto> returnDoc(@PathVariable String roUid,
                                           @Valid @RequestBody(required = false) ReasonRequest request) {
        return ResponseEntity.ok(transferService.returnRO(roUid, request == null ? null : request.reason()));
    }
}
