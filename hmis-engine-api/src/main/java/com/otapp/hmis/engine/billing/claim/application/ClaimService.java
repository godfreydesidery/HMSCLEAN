package com.otapp.hmis.engine.billing.claim.application;

import com.otapp.hmis.engine.billing.claim.application.ClaimDtos.AssembleClaimRequest;
import com.otapp.hmis.engine.billing.claim.application.ClaimDtos.ClaimDto;
import com.otapp.hmis.engine.billing.claim.application.ClaimDtos.ClaimLineDto;
import com.otapp.hmis.engine.billing.claim.application.ClaimDtos.ClaimSummary;
import com.otapp.hmis.engine.billing.claim.domain.Claim;
import com.otapp.hmis.engine.billing.claim.domain.ClaimLine;
import com.otapp.hmis.engine.billing.claim.domain.ClaimLineRepository;
import com.otapp.hmis.engine.billing.claim.domain.ClaimRepository;
import com.otapp.hmis.engine.billing.claim.domain.ClaimStatus;
import com.otapp.hmis.engine.billing.claim.infrastructure.ClaimNumberGenerator;
import com.otapp.hmis.engine.billing.invoice.domain.Invoice;
import com.otapp.hmis.engine.billing.invoice.domain.InvoiceLine;
import com.otapp.hmis.engine.billing.invoice.domain.InvoiceLineRepository;
import com.otapp.hmis.engine.billing.invoice.domain.InvoiceRepository;
import com.otapp.hmis.engine.common.api.PageResponse;
import com.otapp.hmis.engine.common.error.BusinessRuleException;
import com.otapp.hmis.engine.common.error.NotFoundException;
import com.otapp.hmis.engine.masterdata.insurance.domain.InsurancePlan;
import com.otapp.hmis.engine.masterdata.insurance.domain.InsurancePlanRepository;
import com.otapp.hmis.engine.masterdata.insurance.domain.InsuranceProvider;
import com.otapp.hmis.engine.masterdata.insurance.domain.InsuranceProviderRepository;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Insurance claim ledger — aggregates the COVERED invoice lines routed to one
 * {@code (payer plan, member)} into a per-payer claim and drives its
 * submit → settle/reject lifecycle.
 *
 * <p>The covered portion was already settled at charge time
 * ({@code Invoice.totalCovered} nets it out of the patient balance), so this
 * service is a READ/AGGREGATE + lifecycle layer: it reads {@code invoice_line}
 * amounts and stamps {@code invoice_line.claim_id}, but NEVER mutates an
 * invoice's money fields. Lives entirely inside billing; only masterdata
 * (insurance plan/provider) is read across the boundary, which is allowed.
 */
@Service
@RequiredArgsConstructor
public class ClaimService {

    private final ClaimRepository claimRepository;
    private final ClaimLineRepository claimLineRepository;
    private final InvoiceLineRepository invoiceLineRepository;
    private final InvoiceRepository invoiceRepository;
    private final InsurancePlanRepository insurancePlanRepository;
    private final InsuranceProviderRepository insuranceProviderRepository;
    private final ClaimNumberGenerator claimNumberGenerator;

    /**
     * Build a DRAFT claim from every unclaimed COVERED line routed to the given
     * plan + member. Stamps each source line with the new claim id so it can
     * never be claimed twice.
     */
    @Transactional
    public ClaimDto assembleForPayer(AssembleClaimRequest req) {
        List<InvoiceLine> lines = invoiceLineRepository
                .findClaimableCoveredLines(req.payerPlanUid(), req.membershipNo());
        if (lines.isEmpty()) {
            throw new BusinessRuleException(
                    "No unclaimed covered lines for this plan and member");
        }

        InsurancePlan plan = insurancePlanRepository.findByUid(req.payerPlanUid())
                .orElseThrow(() -> new NotFoundException("Insurance plan not found: " + req.payerPlanUid()));

        // Patient + currency come from the lines' invoices (consistent per member).
        Map<String, Invoice> invoiceCache = new HashMap<>();
        String currency = null;
        String patientUid = null;
        BigDecimal claimed = BigDecimal.ZERO;
        for (InvoiceLine line : lines) {
            Invoice inv = invoiceCache.computeIfAbsent(line.getInvoiceUid(),
                    uid -> invoiceRepository.findByUid(uid).orElse(null));
            if (inv == null) {
                throw new BusinessRuleException("Covered line " + line.getUid() + " has no invoice");
            }
            if (currency == null) {
                currency = inv.getCurrency();
                patientUid = inv.getPatientUid();
            } else if (!currency.equals(inv.getCurrency())) {
                throw new BusinessRuleException("A claim cannot mix currencies");
            }
            claimed = claimed.add(line.getAmount());
        }

        Claim claim = claimRepository.save(new Claim(
                claimNumberGenerator.next(), req.payerPlanUid(), plan.getProviderUid(),
                req.membershipNo(), patientUid, currency, claimed, lines.size()));

        for (InvoiceLine line : lines) {
            claimLineRepository.save(new ClaimLine(
                    claim.getId(), line.getId(), line.getServiceUid(), line.getKind(),
                    line.getDescription(), line.getQuantity(), line.getUnitPrice(), line.getAmount()));
            line.markClaimed(claim.getId());
            invoiceLineRepository.save(line);
        }
        return toDto(claim);
    }

    @Transactional
    public ClaimDto submit(String claimUid) {
        Claim claim = loadOrThrow(claimUid);
        claim.submit(currentUsername());
        return toDto(claim);
    }

    @Transactional
    public ClaimDto recordSettlement(String claimUid, BigDecimal amount) {
        Claim claim = loadOrThrow(claimUid);
        claim.recordSettlement(amount, currentUsername());
        return toDto(claim);
    }

    @Transactional
    public ClaimDto reject(String claimUid, String reason) {
        Claim claim = loadOrThrow(claimUid);
        claim.reject(reason, currentUsername());
        return toDto(claim);
    }

    /** Discard a DRAFT claim, releasing its lines back to claimable. */
    @Transactional
    public void discardDraft(String claimUid) {
        Claim claim = loadOrThrow(claimUid);
        if (!claim.isDiscardable()) {
            throw new BusinessRuleException(
                    "Only a DRAFT claim can be discarded (current: " + claim.getStatus() + ")");
        }
        for (ClaimLine line : claimLineRepository.findAllByClaimIdOrderByCreatedAtAsc(claim.getId())) {
            invoiceLineRepository.findById(line.getInvoiceLineId()).ifPresent(src -> {
                src.releaseClaim();
                invoiceLineRepository.save(src);
            });
        }
        claimLineRepository.deleteAllByClaimId(claim.getId());
        claimRepository.delete(claim);
    }

    @Transactional(readOnly = true)
    public ClaimDto findByUid(String claimUid) {
        return toDto(loadOrThrow(claimUid));
    }

    @Transactional(readOnly = true)
    public PageResponse<ClaimSummary> search(ClaimStatus status, String payerPlanUid,
                                             String providerUid, String membershipNo, Pageable pageable) {
        Map<String, String> planNames = new HashMap<>();
        Map<String, String> providerNames = new HashMap<>();
        return PageResponse.from(claimRepository
                .search(status, emptyToNull(payerPlanUid), emptyToNull(providerUid), emptyToNull(membershipNo), pageable)
                .map(c -> toSummary(c, planNames, providerNames)));
    }

    // ----- mapping -----------------------------------------------------------

    private Claim loadOrThrow(String claimUid) {
        return claimRepository.findByUid(claimUid)
                .orElseThrow(() -> new NotFoundException("Claim not found: " + claimUid));
    }

    private ClaimDto toDto(Claim c) {
        List<ClaimLineDto> lines = claimLineRepository.findAllByClaimIdOrderByCreatedAtAsc(c.getId()).stream()
                .map(l -> new ClaimLineDto(l.getId(), l.getUid(), l.getInvoiceLineId(), l.getServiceUid(),
                        l.getKind(), l.getDescription(), l.getQuantity(), l.getUnitPrice(), l.getAmount()))
                .toList();
        return new ClaimDto(c.getId(), c.getUid(), c.getClaimNo(),
                c.getPayerPlanUid(), planName(c.getPayerPlanUid(), new HashMap<>()),
                c.getProviderUid(), providerName(c.getProviderUid(), new HashMap<>()),
                c.getMembershipNo(), c.getPatientUid(), c.getCurrency(),
                c.getClaimedAmount(), c.getSettledAmount(), c.outstanding(), c.getStatus(), c.getLineCount(),
                c.getSubmittedAt(), c.getSettledAt(), c.getRejectedAt(), c.getRejectionReason(),
                c.getSubmittedByUsername(), c.getSettledByUsername(), c.getRejectedByUsername(),
                c.getCreatedAt(), lines);
    }

    private ClaimSummary toSummary(Claim c, Map<String, String> planNames, Map<String, String> providerNames) {
        return new ClaimSummary(c.getId(), c.getUid(), c.getClaimNo(),
                c.getPayerPlanUid(), planName(c.getPayerPlanUid(), planNames),
                providerName(c.getProviderUid(), providerNames),
                c.getMembershipNo(), c.getPatientUid(), c.getCurrency(),
                c.getClaimedAmount(), c.getSettledAmount(), c.getStatus(), c.getLineCount(), c.getCreatedAt());
    }

    private String planName(String planUid, Map<String, String> cache) {
        return cache.computeIfAbsent(planUid, uid ->
                insurancePlanRepository.findByUid(uid).map(InsurancePlan::getName).orElse(null));
    }

    private String providerName(String providerUid, Map<String, String> cache) {
        return cache.computeIfAbsent(providerUid, uid ->
                insuranceProviderRepository.findByUid(uid).map(InsuranceProvider::getName).orElse(null));
    }

    private static String currentUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new BusinessRuleException("Authenticated user required");
        }
        return auth.getName();
    }

    private static String emptyToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
