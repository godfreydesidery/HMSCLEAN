package com.otapp.hmis.engine.encounter.consumable.application;

import com.otapp.hmis.engine.common.error.BusinessRuleException;
import com.otapp.hmis.engine.common.error.NotFoundException;
import com.otapp.hmis.engine.encounter.admission.domain.Admission;
import com.otapp.hmis.engine.encounter.admission.domain.AdmissionRepository;
import com.otapp.hmis.engine.encounter.admission.domain.AdmissionStatus;
import com.otapp.hmis.engine.encounter.consumable.application.ConsumableIssueDtos.ConsumableIssueDto;
import com.otapp.hmis.engine.encounter.consumable.application.ConsumableIssueDtos.IssueConsumableRequest;
import com.otapp.hmis.engine.encounter.consumable.domain.ConsumableIssue;
import com.otapp.hmis.engine.encounter.consumable.domain.ConsumableIssueRepository;
import com.otapp.hmis.engine.masterdata.consumable.domain.Consumable;
import com.otapp.hmis.engine.masterdata.consumable.domain.ConsumableRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConsumableIssueService {

    private final ConsumableIssueRepository issueRepository;
    private final AdmissionRepository admissionRepository;
    private final ConsumableRepository consumableRepository;
    private final ConsumableStockService consumableStockService;

    @Transactional
    public ConsumableIssueDto issue(String admissionUid, IssueConsumableRequest request) {
        Admission admission = admissionRepository.findByUid(admissionUid)
                .orElseThrow(() -> new NotFoundException("Admission not found: " + admissionUid));
        if (admission.getStatus() != AdmissionStatus.ADMITTED) {
            throw new BusinessRuleException(
                    "Can only issue consumables against an ADMITTED admission (current: " + admission.getStatus() + ")");
        }
        Consumable consumable = consumableRepository.findByUid(request.consumableUid())
                .orElseThrow(() -> new NotFoundException("Consumable not found: " + request.consumableUid()));
        if (!consumable.isActive()) {
            throw new BusinessRuleException("Consumable is not active: " + consumable.getName());
        }

        ConsumableIssue issued = new ConsumableIssue(
                admission.getUid(),
                consumable.getUid(),
                request.sourceKind(),
                request.sourceLocationUid(),
                request.quantity(),
                request.unitCost(),
                currentUsername(),
                emptyToNull(request.note()));
        issueRepository.save(issued);
        // Decrement source stock after the audit row exists so the movement
        // can reference the issue uid. Throws if the source location is short.
        consumableStockService.decrementForIssue(
                request.sourceKind(), request.sourceLocationUid(),
                consumable.getUid(), request.quantity(), issued.getUid());
        return toDto(issued, consumable);
    }

    @Transactional(readOnly = true)
    public List<ConsumableIssueDto> listForAdmission(String admissionUid) {
        admissionRepository.findByUid(admissionUid)
                .orElseThrow(() -> new NotFoundException("Admission not found: " + admissionUid));
        return issueRepository.findAllByAdmissionUidOrderByIssuedAtAsc(admissionUid).stream()
                .map(this::toDto)
                .toList();
    }

    private ConsumableIssueDto toDto(ConsumableIssue i) {
        Consumable c = consumableRepository.findByUid(i.getConsumableUid()).orElse(null);
        return toDto(i, c);
    }

    private static ConsumableIssueDto toDto(ConsumableIssue i, Consumable c) {
        return new ConsumableIssueDto(
                i.getUid(),
                i.getAdmissionUid(),
                i.getConsumableUid(),
                c == null ? null : c.getCode(),
                c == null ? null : c.getName(),
                i.getSourceKind(),
                i.getSourceLocationUid(),
                i.getQuantity(),
                i.getUnitCost(),
                i.lineAmount(),
                i.getIssuedByUsername(),
                i.getIssuedAt(),
                i.getNote(),
                i.getCreatedAt());
    }

    private static String emptyToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String currentUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new BusinessRuleException("Authenticated user required");
        }
        return auth.getName();
    }
}
