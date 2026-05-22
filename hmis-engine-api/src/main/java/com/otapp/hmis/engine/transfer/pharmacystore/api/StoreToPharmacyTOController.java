package com.otapp.hmis.engine.transfer.pharmacystore.api;

import com.otapp.hmis.engine.common.api.PageResponse;
import com.otapp.hmis.engine.transfer.pharmacystore.application.PharmacyStoreTransferDtos.CreateTORequest;
import com.otapp.hmis.engine.transfer.pharmacystore.application.PharmacyStoreTransferDtos.ReasonRequest;
import com.otapp.hmis.engine.transfer.pharmacystore.application.PharmacyStoreTransferDtos.TODto;
import com.otapp.hmis.engine.transfer.pharmacystore.application.PharmacyStoreTransferDtos.TOSummary;
import com.otapp.hmis.engine.transfer.pharmacystore.application.PharmacyStoreTransferService;
import com.otapp.hmis.engine.transfer.common.domain.TransferDocStatus;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

@Tag(name = "Pharmacy↔Store transfers — TO (Transfer Order)")
@RestController
@RequestMapping("/transfers/pharmacy-store/to")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('STORE_ACCESS')")
public class StoreToPharmacyTOController {

    private final PharmacyStoreTransferService transferService;

    @PostMapping
    public ResponseEntity<TODto> create(@Valid @RequestBody CreateTORequest request) {
        TODto created = transferService.createTO(request);
        URI loc = UriComponentsBuilder.fromPath("/transfers/pharmacy-store/to/uid/{toUid}")
                .buildAndExpand(created.uid()).toUri();
        return ResponseEntity.created(loc).body(created);
    }

    @GetMapping
    public ResponseEntity<PageResponse<TOSummary>> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) TransferDocStatus status,
            @RequestParam(required = false) String pharmacyUid,
            @RequestParam(required = false) String storeUid,
            @RequestParam(required = false) String roUid,
            Pageable pageable) {
        return ResponseEntity.ok(
                transferService.searchTOs(query, status, pharmacyUid, storeUid, roUid, pageable));
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
