package com.otapp.hmis.engine.encounter.result.application;

import com.otapp.hmis.engine.common.error.BusinessRuleException;
import com.otapp.hmis.engine.common.error.NotFoundException;
import com.otapp.hmis.engine.encounter.order.domain.ClinicalOrder;
import com.otapp.hmis.engine.encounter.order.domain.ClinicalOrderKind;
import com.otapp.hmis.engine.encounter.order.domain.ClinicalOrderRepository;
import com.otapp.hmis.engine.encounter.order.domain.ClinicalOrderStatus;
import com.otapp.hmis.engine.encounter.result.application.OrderResultDtos.LabResultLineDto;
import com.otapp.hmis.engine.encounter.result.application.OrderResultDtos.LabResultLineInput;
import com.otapp.hmis.engine.encounter.result.application.OrderResultDtos.OrderResultDto;
import com.otapp.hmis.engine.encounter.result.application.OrderResultDtos.SaveResultRequest;
import com.otapp.hmis.engine.encounter.result.domain.LabResultFlag;
import com.otapp.hmis.engine.encounter.result.domain.LabResultFlagger;
import com.otapp.hmis.engine.encounter.result.domain.LabResultLine;
import com.otapp.hmis.engine.encounter.result.domain.LabResultLineRepository;
import com.otapp.hmis.engine.encounter.result.domain.OrderResult;
import com.otapp.hmis.engine.encounter.result.domain.OrderResultRepository;
import com.otapp.hmis.engine.masterdata.labtest.application.LabTestAnalyteDtos.AnalyteResolutionDto;
import com.otapp.hmis.engine.masterdata.labtest.application.LabTestAnalyteDtos.AnalyteTemplateDto;
import com.otapp.hmis.engine.masterdata.labtest.application.LabTestAnalyteService;
import com.otapp.hmis.engine.masterdata.labtest.domain.RangeSex;
import com.otapp.hmis.engine.patient.domain.Gender;
import com.otapp.hmis.engine.patient.domain.Patient;
import com.otapp.hmis.engine.patient.domain.PatientRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderResultService {

    private final OrderResultRepository resultRepository;
    private final ClinicalOrderRepository orderRepository;
    private final LabResultLineRepository lineRepository;
    private final LabTestAnalyteService analyteService;
    private final PatientRepository patientRepository;

    @Transactional(readOnly = true)
    public Optional<OrderResultDto> findForOrder(String orderUid) {
        return resultRepository.findByOrderUid(orderUid).map(this::toDto);
    }

    /**
     * The analyte definitions to populate a result-entry grid for a LAB_TEST
     * order. Empty for non-lab orders (they use the narrative form only). Lets a
     * lab technician (ENCOUNTER_ACCESS) read the panel without MASTERDATA_MANAGE.
     */
    @Transactional(readOnly = true)
    public List<AnalyteTemplateDto> analyteTemplate(String orderUid) {
        ClinicalOrder order = loadOrder(orderUid);
        if (order.getKind() != ClinicalOrderKind.LAB_TEST) {
            return List.of();
        }
        return analyteService.templateFor(order.getServiceUid());
    }

    /** Create or update a preliminary result; finalized results require {@link #amend}. */
    @Transactional
    public OrderResultDto save(String orderUid, SaveResultRequest request) {
        ClinicalOrder order = loadOrder(orderUid);
        requireNotCancelled(order);
        String narrative = emptyToNull(request.narrative());
        String impression = emptyToNull(request.impression());
        OrderResult result = resultRepository.findByOrderUid(orderUid)
                .map(existing -> {
                    existing.editPreliminary(narrative, impression);
                    return existing;
                })
                .orElseGet(() -> resultRepository.save(new OrderResult(orderUid, order.getKind(), narrative, impression)));

        // The order must have passed its accept/approve gate before results can
        // be recorded; the first result entry moves it ACCEPTED/APPROVED -> IN_PROGRESS.
        if (order.getStatus() == ClinicalOrderStatus.REQUESTED) {
            throw new BusinessRuleException(
                    "Accept (lab / radiology) or approve (procedure) the order before recording a result");
        }
        if (order.getStatus() == ClinicalOrderStatus.ACCEPTED
                || order.getStatus() == ClinicalOrderStatus.APPROVED) {
            order.markInProgress();
        }

        rebuildLabLines(result, order, request.lines());
        return toDto(result);
    }

    @Transactional
    public OrderResultDto finalizeResult(String orderUid) {
        ClinicalOrder order = loadOrder(orderUid);
        requireNotCancelled(order);
        OrderResult result = resultRepository.findByOrderUid(orderUid)
                .orElseThrow(() -> new NotFoundException("No result recorded for order: " + orderUid));
        result.finalize(currentUsername());

        // Drive the order to COMPLETED, surfacing the impression as the order's
        // result summary so list views can show it without an extra fetch.
        if (order.getStatus() != ClinicalOrderStatus.COMPLETED) {
            order.complete(result.getImpression());
        }
        return toDto(result);
    }

    @Transactional
    public OrderResultDto amend(String orderUid, SaveResultRequest request) {
        ClinicalOrder order = loadOrder(orderUid);
        requireNotCancelled(order);
        OrderResult result = resultRepository.findByOrderUid(orderUid)
                .orElseThrow(() -> new NotFoundException("No result recorded for order: " + orderUid));
        result.amend(emptyToNull(request.narrative()), emptyToNull(request.impression()), currentUsername());
        rebuildLabLines(result, order, request.lines());
        return toDto(result);
    }

    // ===== structured lab lines =============================================

    /**
     * Rebuilds the structured analyte lines for a LAB_TEST result. Each measured
     * value is resolved against the applicable (sex/age-banded) reference range,
     * which is <strong>snapshotted</strong> onto the line and the abnormal flag
     * computed at this moment — so the masterdata can later change without
     * altering an already-recorded result. No-op for non-lab orders or when the
     * request carries no {@code lines} (narrative-only edit).
     */
    private void rebuildLabLines(OrderResult result, ClinicalOrder order, List<LabResultLineInput> inputs) {
        if (order.getKind() != ClinicalOrderKind.LAB_TEST || inputs == null) {
            return;
        }
        Set<String> seen = new HashSet<>();
        for (LabResultLineInput in : inputs) {
            if (!seen.add(in.analyteUid())) {
                throw new BusinessRuleException("Duplicate analyte in result: " + in.analyteUid());
            }
        }

        RangeSex sex = RangeSex.ANY;
        Integer ageDays = null;
        Optional<Patient> patient = patientRepository.findByUid(order.getPatientUid());
        if (patient.isPresent()) {
            sex = mapSex(patient.get().getGender());
            ageDays = ageInDays(patient.get().getDateOfBirth(), specimenDateFor(order));
        }

        lineRepository.deleteByOrderResultUid(result.getUid());
        lineRepository.flush();

        for (LabResultLineInput in : inputs) {
            BigDecimal numeric = in.valueNumeric();
            String text = emptyToNull(in.valueText());
            if (numeric == null && text == null) {
                continue; // skip blank measurements — nothing recorded for this analyte
            }
            AnalyteResolutionDto a = analyteService.resolveForEntry(in.analyteUid(), sex, ageDays);
            if (!a.labTestTypeUid().equals(order.getServiceUid())) {
                throw new BusinessRuleException(
                        "Analyte " + in.analyteUid() + " does not belong to this order's test");
            }
            LabResultFlag flag = LabResultFlagger.flag(a.valueKind(), numeric, text,
                    a.refLow(), a.refHigh(), a.criticalLow(), a.criticalHigh(), a.normalText());
            lineRepository.save(new LabResultLine(
                    result.getUid(), a.analyteUid(), a.code(), a.name(), a.valueKind(), a.unit(),
                    numeric, text, flag, a.refLow(), a.refHigh(), a.criticalLow(), a.criticalHigh(),
                    a.normalText(), a.rangeDisplay(), a.referenceRangeUid(), a.displayOrder(),
                    emptyToNull(in.note())));
        }
    }

    private static RangeSex mapSex(Gender gender) {
        if (gender == Gender.MALE) return RangeSex.MALE;
        if (gender == Gender.FEMALE) return RangeSex.FEMALE;
        return RangeSex.ANY;
    }

    /**
     * Patient age in days at the clinically relevant moment — the specimen
     * accept date, falling back to the order request date. Using a fixed event
     * date (not {@code now()}) makes age-banding reproducible: re-saving or
     * amending a result later resolves the same band it did at entry.
     */
    private static Integer ageInDays(LocalDate dateOfBirth, LocalDate asOf) {
        if (dateOfBirth == null) return null;
        long days = ChronoUnit.DAYS.between(dateOfBirth, asOf);
        return days < 0 ? 0 : (int) days;
    }

    private static LocalDate specimenDateFor(ClinicalOrder order) {
        Instant ref = order.getAcceptedAt() != null ? order.getAcceptedAt() : order.getRequestedAt();
        return ref == null ? LocalDate.now() : LocalDate.ofInstant(ref, ZoneId.systemDefault());
    }

    private static void requireNotCancelled(ClinicalOrder order) {
        if (order.getStatus() == ClinicalOrderStatus.CANCELLED) {
            throw new BusinessRuleException("Cannot record or change results on a cancelled order");
        }
    }

    // ===== helpers ==========================================================

    private ClinicalOrder loadOrder(String orderUid) {
        return orderRepository.findByUid(orderUid)
                .orElseThrow(() -> new NotFoundException("Order not found: " + orderUid));
    }

    private static String currentUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? null : auth.getName();
    }

    private OrderResultDto toDto(OrderResult r) {
        List<LabResultLineDto> lines = lineRepository
                .findByOrderResultUidOrderByDisplayOrderAscAnalyteCodeAsc(r.getUid())
                .stream()
                .map(l -> new LabResultLineDto(l.getId(), l.getUid(), l.getAnalyteUid(), l.getAnalyteCode(),
                        l.getAnalyteName(), l.getValueKind(), l.getValueNumeric(), l.getValueText(), l.getUnit(),
                        l.getFlag(), l.getRefLow(), l.getRefHigh(), l.getCriticalLow(), l.getCriticalHigh(),
                        l.getRangeDisplay(), l.getDisplayOrder(), l.getNote()))
                .toList();
        return new OrderResultDto(
                r.getId(),
                r.getUid(),
                r.getOrderUid(),
                r.getOrderKind(),
                r.getStatus(),
                r.getNarrative(),
                r.getImpression(),
                lines,
                r.getFinalizedAt(),
                r.getFinalizedBy(),
                r.getAmendedAt(),
                r.getAmendedBy(),
                r.getCreatedAt(),
                r.getUpdatedAt());
    }

    private static String emptyToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
