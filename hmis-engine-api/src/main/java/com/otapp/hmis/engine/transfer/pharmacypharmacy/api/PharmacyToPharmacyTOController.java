package com.otapp.hmis.engine.transfer.pharmacypharmacy.api;

import com.otapp.hmis.engine.common.api.PageResponse;
import com.otapp.hmis.engine.transfer.common.domain.TransferDocStatus;
import com.otapp.hmis.engine.transfer.pharmacypharmacy.application.PharmacyPharmacyTransferDtos.CreateTORequest;
import com.otapp.hmis.engine.transfer.pharmacypharmacy.application.PharmacyPharmacyTransferDtos.ReasonRequest;
import com.otapp.hmis.engine.transfer.pharmacypharmacy.application.PharmacyPharmacyTransferDtos.TODto;
import com.otapp.hmis.engine.transfer.pharmacypharmacy.application.PharmacyPharmacyTransferDtos.TOSummary;
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

@Tag(name = "Pharmacy↔Pharmacy transfers — TO (Transfer Order)")
@RestController
@RequestMapping("/transfers/pharmacy-pharmacy/to")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('PHARMACY_ACCESS')")
public class PharmacyToPharmacyTOController {

    private final PharmacyPharmacyTransferService transferService;

    @PostMapping
    public ResponseEntity<TODto> create(@Valid @RequestBody CreateTORequest request) {
        TODto created = transferService.createTO(request);
        URI loc = UriComponentsBuilder.fromPath("/transfers/pharmacy-pharmacy/to/uid/{toUid}")
                .buildAndExpand(created.uid()).toUri();
        return ResponseEntity.created(loc).body(created);
    }

    @GetMapping
    public ResponseEntity<PageResponse<TOSummary>> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) TransferDocStatus status,
            @RequestParam(required = false) String requestingPharmacyUid,
            @RequestParam(required = false) String deliveringPharmacyUid,
            @RequestParam(required = false) String roUid,
            Pageable pageable) {
        return ResponseEntity.ok(transferService.searchTOs(
                query, status, requestingPharmacyUid, deliveringPharmacyUid, roUid, pageable));
    }

    @GetMapping("/uid/{toUid}")
    public ResponseEntity<TODto> findByUid(@PathVariable String toUid) {
        return ResponseEntity.ok(transferService.findTO(toUid));
    }

    @PostMapping("/uid/{toUid}/verify")
    public ResponseEntity<TODto> verify(@PathVariable String toUid) {
        return ResponseEntity.ok(transferService.verifyTO(toUid));
    }

    @PostMapping("/uid/{toUid}/approve")
    public ResponseEntity<TODto> approve(@PathVariable String toUid) {
        return ResponseEntity.ok(transferService.approveTO(toUid));
    }

    @PostMapping("/uid/{toUid}/issue")
    public ResponseEntity<TODto> issue(@PathVariable String toUid) {
        return ResponseEntity.ok(transferService.issueTO(toUid));
    }

    @PostMapping("/uid/{toUid}/reject")
    public ResponseEntity<TODto> reject(@PathVariable String toUid,
                                        @Valid @RequestBody(required = false) ReasonRequest request) {
        return ResponseEntity.ok(transferService.rejectTO(toUid, request == null ? null : request.reason()));
    }
}
