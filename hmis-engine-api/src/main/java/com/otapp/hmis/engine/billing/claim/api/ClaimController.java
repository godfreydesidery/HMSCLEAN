package com.otapp.hmis.engine.billing.claim.api;

import com.otapp.hmis.engine.billing.claim.application.ClaimDtos.AssembleClaimRequest;
import com.otapp.hmis.engine.billing.claim.application.ClaimDtos.ClaimDto;
import com.otapp.hmis.engine.billing.claim.application.ClaimDtos.ClaimSummary;
import com.otapp.hmis.engine.billing.claim.application.ClaimDtos.RecordSettlementRequest;
import com.otapp.hmis.engine.billing.claim.application.ClaimDtos.RejectClaimRequest;
import com.otapp.hmis.engine.billing.claim.application.ClaimService;
import com.otapp.hmis.engine.billing.claim.domain.ClaimStatus;
import com.otapp.hmis.engine.common.api.PageResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

@Tag(name = "Insurance claims")
@RestController
@RequestMapping("/billing/claims")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('BILLING_ACCESS')")
public class ClaimController {

    private final ClaimService claimService;

    @GetMapping
    public ResponseEntity<PageResponse<ClaimSummary>> search(
            @RequestParam(required = false) ClaimStatus status,
            @RequestParam(required = false) String payerPlanUid,
            @RequestParam(required = false) String providerUid,
            @RequestParam(required = false) String membershipNo,
            Pageable pageable) {
        return ResponseEntity.ok(claimService.search(status, payerPlanUid, providerUid, membershipNo, pageable));
    }

    @GetMapping("/uid/{claimUid}")
    public ResponseEntity<ClaimDto> findByUid(@PathVariable String claimUid) {
        return ResponseEntity.ok(claimService.findByUid(claimUid));
    }

    /** Build a DRAFT claim from the unclaimed COVERED lines for a (payer plan, member). */
    @PostMapping
    public ResponseEntity<ClaimDto> assemble(@Valid @RequestBody AssembleClaimRequest request) {
        ClaimDto created = claimService.assembleForPayer(request);
        URI loc = UriComponentsBuilder.fromPath("/billing/claims/uid/{uid}").buildAndExpand(created.uid()).toUri();
        return ResponseEntity.created(loc).body(created);
    }

    @PostMapping("/uid/{claimUid}/submit")
    public ResponseEntity<ClaimDto> submit(@PathVariable String claimUid) {
        return ResponseEntity.ok(claimService.submit(claimUid));
    }

    @PostMapping("/uid/{claimUid}/settlements")
    public ResponseEntity<ClaimDto> recordSettlement(@PathVariable String claimUid,
                                                     @Valid @RequestBody RecordSettlementRequest request) {
        return ResponseEntity.ok(claimService.recordSettlement(claimUid, request.amount()));
    }

    @PostMapping("/uid/{claimUid}/reject")
    public ResponseEntity<ClaimDto> reject(@PathVariable String claimUid,
                                           @Valid @RequestBody RejectClaimRequest request) {
        return ResponseEntity.ok(claimService.reject(claimUid, request.reason()));
    }

    /** Discard a DRAFT claim, releasing its lines back to claimable. */
    @DeleteMapping("/uid/{claimUid}")
    public ResponseEntity<Void> discard(@PathVariable String claimUid) {
        claimService.discardDraft(claimUid);
        return ResponseEntity.noContent().build();
    }
}
