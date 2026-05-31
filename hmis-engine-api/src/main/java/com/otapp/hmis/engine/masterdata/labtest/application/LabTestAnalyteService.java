package com.otapp.hmis.engine.masterdata.labtest.application;

import com.otapp.hmis.engine.common.error.BusinessRuleException;
import com.otapp.hmis.engine.common.error.ConflictException;
import com.otapp.hmis.engine.common.error.NotFoundException;
import com.otapp.hmis.engine.masterdata.labtest.application.LabTestAnalyteDtos.AnalyteResolutionDto;
import com.otapp.hmis.engine.masterdata.labtest.application.LabTestAnalyteDtos.AnalyteTemplateDto;
import com.otapp.hmis.engine.masterdata.labtest.application.LabTestAnalyteDtos.CreateAnalyteRequest;
import com.otapp.hmis.engine.masterdata.labtest.application.LabTestAnalyteDtos.CreateRangeRequest;
import com.otapp.hmis.engine.masterdata.labtest.application.LabTestAnalyteDtos.LabReferenceRangeDto;
import com.otapp.hmis.engine.masterdata.labtest.application.LabTestAnalyteDtos.LabTestAnalyteDto;
import com.otapp.hmis.engine.masterdata.labtest.application.LabTestAnalyteDtos.UpdateAnalyteRequest;
import com.otapp.hmis.engine.masterdata.labtest.application.LabTestAnalyteDtos.UpdateRangeRequest;
import com.otapp.hmis.engine.masterdata.labtest.domain.LabReferenceRange;
import com.otapp.hmis.engine.masterdata.labtest.domain.LabReferenceRangeRepository;
import com.otapp.hmis.engine.masterdata.labtest.domain.LabTestAnalyte;
import com.otapp.hmis.engine.masterdata.labtest.domain.LabTestAnalyteRepository;
import com.otapp.hmis.engine.masterdata.labtest.domain.LabTestTypeRepository;
import com.otapp.hmis.engine.masterdata.labtest.domain.RangeSex;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Manages the analyte catalogue + reference ranges of a lab test type, and
 * resolves the applicable range for a patient at result-entry time. The
 * encounter result module consumes {@link #templateFor} and
 * {@link #resolveForEntry} (read-only) — masterdata never depends on encounter.
 */
@Service
@RequiredArgsConstructor
public class LabTestAnalyteService {

    private final LabTestTypeRepository labTestTypeRepository;
    private final LabTestAnalyteRepository analyteRepository;
    private final LabReferenceRangeRepository rangeRepository;

    // ===== analytes ==========================================================

    @Transactional(readOnly = true)
    public List<LabTestAnalyteDto> listForType(String labTestTypeUid) {
        requireLabTestType(labTestTypeUid);
        return analyteRepository.findByLabTestTypeUidOrderByDisplayOrderAscCodeAsc(labTestTypeUid)
                .stream().map(this::toDto).toList();
    }

    @Transactional
    public LabTestAnalyteDto createAnalyte(String labTestTypeUid, CreateAnalyteRequest request) {
        requireLabTestType(labTestTypeUid);
        String code = request.code().trim().toUpperCase();
        if (analyteRepository.existsByLabTestTypeUidAndCode(labTestTypeUid, code)) {
            throw new ConflictException("Analyte code already exists on this test: " + code);
        }
        LabTestAnalyte a = new LabTestAnalyte(labTestTypeUid, code, request.name().trim(),
                emptyToNull(request.unit()), request.valueKind(), request.displayOrder());
        analyteRepository.save(a);
        return toDto(a);
    }

    @Transactional(readOnly = true)
    public LabTestAnalyteDto findAnalyte(String analyteUid) {
        return toDto(loadAnalyte(analyteUid));
    }

    @Transactional
    public LabTestAnalyteDto updateAnalyte(String analyteUid, UpdateAnalyteRequest request) {
        LabTestAnalyte a = loadAnalyte(analyteUid);
        a.setName(request.name().trim());
        a.setUnit(emptyToNull(request.unit()));
        a.setValueKind(request.valueKind());
        a.setDisplayOrder(request.displayOrder());
        if (request.active()) a.activate(); else a.deactivate();
        return toDto(a);
    }

    @Transactional
    public void deleteAnalyte(String analyteUid) {
        LabTestAnalyte a = loadAnalyte(analyteUid);
        rangeRepository.deleteByAnalyteUid(a.getUid());
        analyteRepository.delete(a);
    }

    // ===== reference ranges ==================================================

    @Transactional(readOnly = true)
    public List<LabReferenceRangeDto> listRanges(String analyteUid) {
        loadAnalyte(analyteUid);
        return rangeRepository.findByAnalyteUidOrderBySexAscCreatedAtAsc(analyteUid)
                .stream().map(LabTestAnalyteService::toDto).toList();
    }

    @Transactional
    public LabReferenceRangeDto addRange(String analyteUid, CreateRangeRequest request) {
        loadAnalyte(analyteUid);
        validateBands(request.ageMinDays(), request.ageMaxDays());
        validateBounds(request.refLow(), request.refHigh(), request.criticalLow(), request.criticalHigh());
        LabReferenceRange r = new LabReferenceRange(analyteUid, request.sex(),
                request.ageMinDays(), request.ageMaxDays(),
                request.refLow(), request.refHigh(), request.criticalLow(), request.criticalHigh(),
                emptyToNull(request.normalText()), emptyToNull(request.rangeDisplay()));
        rangeRepository.save(r);
        return toDto(r);
    }

    @Transactional
    public LabReferenceRangeDto updateRange(String rangeUid, UpdateRangeRequest request) {
        LabReferenceRange r = rangeRepository.findByUid(rangeUid)
                .orElseThrow(() -> new NotFoundException("Reference range not found: " + rangeUid));
        validateBands(request.ageMinDays(), request.ageMaxDays());
        validateBounds(request.refLow(), request.refHigh(), request.criticalLow(), request.criticalHigh());
        r.setSex(request.sex());
        r.setAgeMinDays(request.ageMinDays());
        r.setAgeMaxDays(request.ageMaxDays());
        r.setRefLow(request.refLow());
        r.setRefHigh(request.refHigh());
        r.setCriticalLow(request.criticalLow());
        r.setCriticalHigh(request.criticalHigh());
        r.setNormalText(emptyToNull(request.normalText()));
        r.setRangeDisplay(emptyToNull(request.rangeDisplay()));
        r.setActive(request.active());
        return toDto(r);
    }

    @Transactional
    public void deleteRange(String rangeUid) {
        LabReferenceRange r = rangeRepository.findByUid(rangeUid)
                .orElseThrow(() -> new NotFoundException("Reference range not found: " + rangeUid));
        rangeRepository.delete(r);
    }

    // ===== cross-module read views (encounter result entry) ==================

    @Transactional(readOnly = true)
    public List<AnalyteTemplateDto> templateFor(String labTestTypeUid) {
        return analyteRepository.findByLabTestTypeUidAndActiveTrueOrderByDisplayOrderAscCodeAsc(labTestTypeUid)
                .stream()
                .map(a -> new AnalyteTemplateDto(a.getUid(), a.getCode(), a.getName(), a.getUnit(),
                        a.getValueKind(), a.getDisplayOrder()))
                .toList();
    }

    /**
     * Resolves the analyte definition + the most specific active reference range
     * applying to a patient of the given sex and age-in-days. Used by the
     * encounter result service to snapshot bounds onto a result line.
     */
    @Transactional(readOnly = true)
    public AnalyteResolutionDto resolveForEntry(String analyteUid, RangeSex patientSex, Integer ageDays) {
        LabTestAnalyte a = loadAnalyte(analyteUid);
        LabReferenceRange best = rangeRepository.findByAnalyteUidAndActiveTrue(analyteUid).stream()
                .filter(r -> r.appliesTo(patientSex == null ? RangeSex.ANY : patientSex, ageDays))
                // Most specific wins; then the narrowest age band; then uid as a
                // stable final tie-break so two equally-ranked ranges (a config
                // overlap) always resolve to the same snapshot — determinism.
                .max(Comparator.comparingInt(LabReferenceRange::specificity)
                        .thenComparing(Comparator.comparingLong(LabTestAnalyteService::ageSpan).reversed())
                        .thenComparing(LabReferenceRange::getUid))
                .orElse(null);
        return new AnalyteResolutionDto(
                a.getUid(), a.getLabTestTypeUid(), a.getCode(), a.getName(), a.getUnit(), a.getValueKind(),
                a.getDisplayOrder(),
                best == null ? null : best.getRefLow(),
                best == null ? null : best.getRefHigh(),
                best == null ? null : best.getCriticalLow(),
                best == null ? null : best.getCriticalHigh(),
                best == null ? null : best.getNormalText(),
                best == null ? null : best.getRangeDisplay(),
                best == null ? null : best.getUid());
    }

    // ===== helpers ===========================================================

    private void requireLabTestType(String labTestTypeUid) {
        if (labTestTypeRepository.findByUid(labTestTypeUid).isEmpty()) {
            throw new NotFoundException("Lab test not found: " + labTestTypeUid);
        }
    }

    private LabTestAnalyte loadAnalyte(String analyteUid) {
        return analyteRepository.findByUid(analyteUid)
                .orElseThrow(() -> new NotFoundException("Analyte not found: " + analyteUid));
    }

    private static void validateBands(Integer ageMinDays, Integer ageMaxDays) {
        if (ageMinDays != null && ageMaxDays != null && ageMinDays > ageMaxDays) {
            throw new BusinessRuleException("age_min_days must be <= age_max_days");
        }
    }

    /**
     * criticalLow <= refLow <= refHigh <= criticalHigh, for whichever bounds are
     * present. The direct low-vs-high cross-checks close the gaps the adjacent
     * chain would miss when an intermediate bound is null (e.g. refLow set,
     * refHigh null, criticalHigh set).
     */
    private static void validateBounds(BigDecimal refLow, BigDecimal refHigh,
                                       BigDecimal criticalLow, BigDecimal criticalHigh) {
        requireOrder(criticalLow, refLow, "critical_low must be <= ref_low");
        requireOrder(refLow, refHigh, "ref_low must be <= ref_high");
        requireOrder(refHigh, criticalHigh, "ref_high must be <= critical_high");
        requireOrder(criticalLow, criticalHigh, "critical_low must be <= critical_high");
        requireOrder(criticalLow, refHigh, "critical_low must be <= ref_high");
        requireOrder(refLow, criticalHigh, "ref_low must be <= critical_high");
    }

    private static void requireOrder(BigDecimal lower, BigDecimal upper, String message) {
        if (lower != null && upper != null && lower.compareTo(upper) > 0) {
            throw new BusinessRuleException(message);
        }
    }

    /** Width of the age band in days; open-ended bounds count as very wide (least specific). */
    private static long ageSpan(LabReferenceRange r) {
        long min = r.getAgeMinDays() == null ? 0 : r.getAgeMinDays();
        long max = r.getAgeMaxDays() == null ? Integer.MAX_VALUE : r.getAgeMaxDays();
        return max - min;
    }

    private LabTestAnalyteDto toDto(LabTestAnalyte a) {
        List<LabReferenceRangeDto> ranges = rangeRepository
                .findByAnalyteUidOrderBySexAscCreatedAtAsc(a.getUid())
                .stream().map(LabTestAnalyteService::toDto).toList();
        return new LabTestAnalyteDto(a.getId(), a.getUid(), a.getLabTestTypeUid(), a.getCode(), a.getName(),
                a.getUnit(), a.getValueKind(), a.getDisplayOrder(), a.isActive(), ranges,
                a.getCreatedAt(), a.getUpdatedAt());
    }

    private static LabReferenceRangeDto toDto(LabReferenceRange r) {
        return new LabReferenceRangeDto(r.getId(), r.getUid(), r.getAnalyteUid(), r.getSex(),
                r.getAgeMinDays(), r.getAgeMaxDays(), r.getRefLow(), r.getRefHigh(),
                r.getCriticalLow(), r.getCriticalHigh(), r.getNormalText(), r.getRangeDisplay(), r.isActive());
    }

    private static String emptyToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
