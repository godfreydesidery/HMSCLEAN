package com.otapp.hmis.engine.encounter.nursingchart.application;

import com.otapp.hmis.engine.common.error.BusinessRuleException;
import com.otapp.hmis.engine.common.error.NotFoundException;
import com.otapp.hmis.engine.encounter.admission.domain.Admission;
import com.otapp.hmis.engine.encounter.admission.domain.AdmissionRepository;
import com.otapp.hmis.engine.encounter.admission.domain.AdmissionStatus;
import com.otapp.hmis.engine.encounter.nursingchart.application.NursingChartDtos.CancelCarePlanItemRequest;
import com.otapp.hmis.engine.encounter.nursingchart.application.NursingChartDtos.CareActivityEntryDto;
import com.otapp.hmis.engine.encounter.nursingchart.application.NursingChartDtos.CarePlanItemDto;
import com.otapp.hmis.engine.encounter.nursingchart.application.NursingChartDtos.CreateCareActivityEntryRequest;
import com.otapp.hmis.engine.encounter.nursingchart.application.NursingChartDtos.CreateCarePlanItemRequest;
import com.otapp.hmis.engine.encounter.nursingchart.application.NursingChartDtos.CreateDressingEntryRequest;
import com.otapp.hmis.engine.encounter.nursingchart.application.NursingChartDtos.CreateFluidBalanceEntryRequest;
import com.otapp.hmis.engine.encounter.nursingchart.application.NursingChartDtos.CreateVitalsEntryRequest;
import com.otapp.hmis.engine.encounter.nursingchart.application.NursingChartDtos.DressingEntryDto;
import com.otapp.hmis.engine.encounter.nursingchart.application.NursingChartDtos.FluidBalanceEntryDto;
import com.otapp.hmis.engine.encounter.nursingchart.application.NursingChartDtos.ResolveCarePlanItemRequest;
import com.otapp.hmis.engine.encounter.nursingchart.application.NursingChartDtos.UpdateCarePlanItemRequest;
import com.otapp.hmis.engine.encounter.nursingchart.application.NursingChartDtos.VitalsEntryDto;
import com.otapp.hmis.engine.encounter.nursingchart.domain.AdmissionVitalsEntry;
import com.otapp.hmis.engine.encounter.nursingchart.domain.AdmissionVitalsEntryRepository;
import com.otapp.hmis.engine.encounter.nursingchart.domain.CareActivityEntry;
import com.otapp.hmis.engine.encounter.nursingchart.domain.CareActivityEntryRepository;
import com.otapp.hmis.engine.encounter.nursingchart.domain.DressingChartEntry;
import com.otapp.hmis.engine.encounter.nursingchart.domain.DressingChartEntryRepository;
import com.otapp.hmis.engine.encounter.nursingchart.domain.FluidBalanceEntry;
import com.otapp.hmis.engine.encounter.nursingchart.domain.FluidBalanceEntryRepository;
import com.otapp.hmis.engine.encounter.nursingchart.domain.NursingCarePlanItem;
import com.otapp.hmis.engine.encounter.nursingchart.domain.NursingCarePlanItemRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * One service per nursing-chart kind would be three near-identical files;
 * grouping them here keeps the cross-cutting "admission must be ADMITTED"
 * guard in one place and matches how the ward actually thinks about a
 * patient's chart bundle.
 */
@Service
@RequiredArgsConstructor
public class NursingChartService {

    private final AdmissionRepository admissionRepository;
    private final AdmissionVitalsEntryRepository vitalsRepository;
    private final NursingCarePlanItemRepository carePlanRepository;
    private final DressingChartEntryRepository dressingRepository;
    private final FluidBalanceEntryRepository fluidBalanceRepository;
    private final CareActivityEntryRepository careActivityRepository;

    // ==================================================================
    // Observation chart (vitals)
    // ==================================================================

    @Transactional
    public VitalsEntryDto recordVitals(String admissionUid, CreateVitalsEntryRequest request) {
        requireOpenAdmission(admissionUid);
        AdmissionVitalsEntry entry = vitalsRepository.save(new AdmissionVitalsEntry(
                admissionUid, currentUsername(),
                request.temperatureC(),
                request.pulseBpm(), request.respirationsBpm(),
                request.systolicBp(), request.diastolicBp(),
                request.spo2Percent(),
                request.bloodGlucoseMmol(), request.painScore(),
                emptyToNull(request.notes())));
        return toDto(entry);
    }

    @Transactional(readOnly = true)
    public List<VitalsEntryDto> listVitals(String admissionUid) {
        loadAdmission(admissionUid);
        return vitalsRepository.findByAdmissionUidOrderByRecordedAtDesc(admissionUid).stream()
                .map(NursingChartService::toDto)
                .toList();
    }

    // ==================================================================
    // Nursing care plan
    // ==================================================================

    @Transactional
    public CarePlanItemDto addCarePlanItem(String admissionUid, CreateCarePlanItemRequest request) {
        requireOpenAdmission(admissionUid);
        NursingCarePlanItem item = carePlanRepository.save(new NursingCarePlanItem(
                admissionUid, currentUsername(),
                request.problem().trim(),
                request.goal().trim(),
                request.intervention().trim(),
                emptyToNull(request.evaluation())));
        return toDto(item);
    }

    @Transactional
    public CarePlanItemDto updateCarePlanItem(String itemUid, UpdateCarePlanItemRequest request) {
        NursingCarePlanItem item = loadCarePlanItem(itemUid);
        if (!item.isOpen()) {
            throw new BusinessRuleException(
                    "Only ACTIVE items can be edited (current: " + item.getStatus() + ")");
        }
        item.setProblem(request.problem().trim());
        item.setGoal(request.goal().trim());
        item.setIntervention(request.intervention().trim());
        item.setEvaluation(emptyToNull(request.evaluation()));
        return toDto(item);
    }

    @Transactional
    public CarePlanItemDto resolveCarePlanItem(String itemUid, ResolveCarePlanItemRequest request) {
        NursingCarePlanItem item = loadCarePlanItem(itemUid);
        item.resolve(currentUsername(), request == null ? null : emptyToNull(request.evaluation()));
        return toDto(item);
    }

    @Transactional
    public CarePlanItemDto cancelCarePlanItem(String itemUid, CancelCarePlanItemRequest request) {
        NursingCarePlanItem item = loadCarePlanItem(itemUid);
        item.cancel(currentUsername(), request == null ? null : emptyToNull(request.reason()));
        return toDto(item);
    }

    @Transactional(readOnly = true)
    public List<CarePlanItemDto> listCarePlan(String admissionUid) {
        loadAdmission(admissionUid);
        return carePlanRepository.findByAdmissionUidOrderByOpenedAtDesc(admissionUid).stream()
                .map(NursingChartService::toDto)
                .toList();
    }

    // ==================================================================
    // Dressing chart
    // ==================================================================

    @Transactional
    public DressingEntryDto recordDressing(String admissionUid, CreateDressingEntryRequest request) {
        requireOpenAdmission(admissionUid);
        DressingChartEntry entry = dressingRepository.save(new DressingChartEntry(
                admissionUid, currentUsername(),
                request.woundLocation().trim(),
                request.woundStatus(),
                request.dressingApplied().trim(),
                emptyToNull(request.notes())));
        return toDto(entry);
    }

    @Transactional(readOnly = true)
    public List<DressingEntryDto> listDressings(String admissionUid) {
        loadAdmission(admissionUid);
        return dressingRepository.findByAdmissionUidOrderByRecordedAtDesc(admissionUid).stream()
                .map(NursingChartService::toDto)
                .toList();
    }

    // ==================================================================
    // Fluid-balance chart (intake / output)
    // ==================================================================

    @Transactional
    public FluidBalanceEntryDto recordFluidBalance(String admissionUid, CreateFluidBalanceEntryRequest request) {
        requireOpenAdmission(admissionUid);
        FluidBalanceEntry entry = fluidBalanceRepository.save(new FluidBalanceEntry(
                admissionUid, currentUsername(),
                request.intakeMl(), request.urineOutputMl(), request.drainageOutputMl(),
                emptyToNull(request.notes())));
        return toDto(entry);
    }

    @Transactional(readOnly = true)
    public List<FluidBalanceEntryDto> listFluidBalance(String admissionUid) {
        loadAdmission(admissionUid);
        return fluidBalanceRepository.findByAdmissionUidOrderByRecordedAtDesc(admissionUid).stream()
                .map(NursingChartService::toDto)
                .toList();
    }

    // ==================================================================
    // Care-activity chart (per-shift tasks + bedside blood sugar)
    // ==================================================================

    @Transactional
    public CareActivityEntryDto recordCareActivity(String admissionUid, CreateCareActivityEntryRequest request) {
        requireOpenAdmission(admissionUid);
        CareActivityEntry entry = careActivityRepository.save(new CareActivityEntry(
                admissionUid, currentUsername(),
                request.feedingDone(), request.positionChanged(), request.bedBathDone(),
                request.randomBloodSugarMmol(), request.fastingBloodSugarMmol(),
                emptyToNull(request.notes())));
        return toDto(entry);
    }

    @Transactional(readOnly = true)
    public List<CareActivityEntryDto> listCareActivity(String admissionUid) {
        loadAdmission(admissionUid);
        return careActivityRepository.findByAdmissionUidOrderByRecordedAtDesc(admissionUid).stream()
                .map(NursingChartService::toDto)
                .toList();
    }

    // ==================================================================
    // Helpers + mapping
    // ==================================================================

    private Admission loadAdmission(String uid) {
        return admissionRepository.findByUid(uid)
                .orElseThrow(() -> new NotFoundException("Admission not found: " + uid));
    }

    private void requireOpenAdmission(String admissionUid) {
        Admission a = loadAdmission(admissionUid);
        if (a.getStatus() != AdmissionStatus.ADMITTED) {
            throw new BusinessRuleException(
                    "Chart entries can only be added while admission is ADMITTED (current: "
                            + a.getStatus() + ")");
        }
    }

    private NursingCarePlanItem loadCarePlanItem(String uid) {
        return carePlanRepository.findByUid(uid)
                .orElseThrow(() -> new NotFoundException("Care-plan item not found: " + uid));
    }

    private static VitalsEntryDto toDto(AdmissionVitalsEntry e) {
        return new VitalsEntryDto(
                e.getUid(), e.getAdmissionUid(),
                e.getRecordedAt(), e.getRecordedByUsername(),
                e.getTemperatureC(), e.getPulseBpm(), e.getRespirationsBpm(),
                e.getSystolicBp(), e.getDiastolicBp(),
                e.getSpo2Percent(),
                e.getBloodGlucoseMmol(), e.getPainScore(),
                e.getNotes(), e.getCreatedAt());
    }

    private static CarePlanItemDto toDto(NursingCarePlanItem i) {
        return new CarePlanItemDto(
                i.getUid(), i.getAdmissionUid(),
                i.getProblem(), i.getGoal(), i.getIntervention(), i.getEvaluation(),
                i.getStatus(),
                i.getOpenedByUsername(), i.getOpenedAt(),
                i.getClosedByUsername(), i.getClosedAt(), i.getCloseReason(),
                i.getCreatedAt(), i.getUpdatedAt());
    }

    private static DressingEntryDto toDto(DressingChartEntry d) {
        return new DressingEntryDto(
                d.getUid(), d.getAdmissionUid(),
                d.getRecordedAt(), d.getRecordedByUsername(),
                d.getWoundLocation(), d.getWoundStatus(), d.getDressingApplied(),
                d.getNotes(), d.getCreatedAt());
    }

    private static FluidBalanceEntryDto toDto(FluidBalanceEntry e) {
        return new FluidBalanceEntryDto(
                e.getUid(), e.getAdmissionUid(),
                e.getRecordedAt(), e.getRecordedByUsername(),
                e.getIntakeMl(), e.getUrineOutputMl(), e.getDrainageOutputMl(),
                e.outputMl(), e.netMl(),
                e.getNotes(), e.getCreatedAt());
    }

    private static CareActivityEntryDto toDto(CareActivityEntry e) {
        return new CareActivityEntryDto(
                e.getUid(), e.getAdmissionUid(),
                e.getRecordedAt(), e.getRecordedByUsername(),
                e.isFeedingDone(), e.isPositionChanged(), e.isBedBathDone(),
                e.getRandomBloodSugarMmol(), e.getFastingBloodSugarMmol(),
                e.getNotes(), e.getCreatedAt());
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
