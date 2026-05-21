package com.otapp.hmis.engine.encounter.order.application;

import com.otapp.hmis.engine.common.api.PageResponse;
import com.otapp.hmis.engine.common.error.NotFoundException;
import com.otapp.hmis.engine.common.error.BusinessRuleException;
import com.otapp.hmis.engine.encounter.admission.domain.AdmissionRepository;
import com.otapp.hmis.engine.encounter.admission.domain.AdmissionStatus;
import com.otapp.hmis.engine.encounter.consultation.domain.Consultation;
import com.otapp.hmis.engine.encounter.consultation.domain.ConsultationRepository;
import com.otapp.hmis.engine.patient.domain.Patient;
import com.otapp.hmis.engine.patient.domain.PatientClassScope;
import com.otapp.hmis.engine.patient.domain.PatientRepository;
import com.otapp.hmis.engine.patient.domain.PatientType;
import com.otapp.hmis.engine.encounter.order.application.ClinicalOrderDtos.CancelOrderRequest;
import com.otapp.hmis.engine.encounter.order.application.ClinicalOrderDtos.ClinicalOrderDto;
import com.otapp.hmis.engine.encounter.order.application.ClinicalOrderDtos.CompleteOrderRequest;
import com.otapp.hmis.engine.encounter.order.application.ClinicalOrderDtos.CreateOrderRequest;
import com.otapp.hmis.engine.encounter.order.application.ClinicalOrderDtos.OrderWorklistDto;
import com.otapp.hmis.engine.encounter.order.application.ClinicalOrderDtos.ScheduleOrderRequest;
import com.otapp.hmis.engine.encounter.order.domain.ClinicalOrder;
import com.otapp.hmis.engine.encounter.order.domain.ClinicalOrderKind;
import com.otapp.hmis.engine.encounter.order.domain.ClinicalOrderRepository;
import com.otapp.hmis.engine.encounter.order.domain.ClinicalOrderStatus;
import com.otapp.hmis.engine.encounter.order.infrastructure.OrderNumberGenerator;
import com.otapp.hmis.engine.masterdata.labtest.domain.LabTestType;
import com.otapp.hmis.engine.masterdata.labtest.domain.LabTestTypeRepository;
import com.otapp.hmis.engine.masterdata.procedure.domain.ProcedureType;
import com.otapp.hmis.engine.masterdata.procedure.domain.ProcedureTypeRepository;
import com.otapp.hmis.engine.masterdata.radiology.domain.RadiologyType;
import com.otapp.hmis.engine.masterdata.radiology.domain.RadiologyTypeRepository;
import com.otapp.hmis.engine.masterdata.theatre.domain.Theatre;
import com.otapp.hmis.engine.masterdata.theatre.domain.TheatreRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClinicalOrderService {

    private final ClinicalOrderRepository orderRepository;
    private final ConsultationRepository consultationRepository;
    private final AdmissionRepository admissionRepository;
    private final PatientRepository patientRepository;
    private final LabTestTypeRepository labTestTypeRepository;
    private final RadiologyTypeRepository radiologyTypeRepository;
    private final ProcedureTypeRepository procedureTypeRepository;
    private final TheatreRepository theatreRepository;
    private final OrderNumberGenerator orderNumberGenerator;

    @Transactional
    public ClinicalOrderDto request(String consultationUid, CreateOrderRequest request) {
        Consultation consultation = consultationRepository.findByUid(consultationUid)
                .orElseThrow(() -> new NotFoundException("Consultation not found: " + consultationUid));

        // Validate that the target service exists in the right catalogue.
        ServiceDescriptor descriptor = resolveService(request.kind(), request.serviceUid());

        ClinicalOrder order = new ClinicalOrder(
                orderNumberGenerator.next(),
                consultation.getUid(),
                consultation.getPatientUid(),
                request.kind(),
                descriptor.uid(),
                request.urgency(),
                emptyToNull(request.instructions()));
        orderRepository.save(order);

        return toDto(order, descriptor);
    }

    /**
     * Raise a clinical order directly against an OUTSIDER (walk-in) patient,
     * bypassing consultation. The patient must be on the registry and have
     * been flagged OUTSIDER — outpatients must use the consultation path.
     */
    @Transactional
    public ClinicalOrderDto requestForOutsider(String patientUid, CreateOrderRequest request) {
        Patient patient = patientRepository.findByUid(patientUid)
                .orElseThrow(() -> new NotFoundException("Patient not found: " + patientUid));
        if (!patient.isActive()) {
            throw new BusinessRuleException("Cannot raise orders for an inactive patient");
        }
        if (patient.getType() != PatientType.OUTSIDER) {
            throw new BusinessRuleException(
                    "Direct orders are for OUTSIDER patients only; OUTPATIENT raises orders inside a consultation");
        }

        ServiceDescriptor descriptor = resolveService(request.kind(), request.serviceUid());

        ClinicalOrder order = new ClinicalOrder(
                orderNumberGenerator.next(),
                null,
                patient.getUid(),
                request.kind(),
                descriptor.uid(),
                request.urgency(),
                emptyToNull(request.instructions()));
        orderRepository.save(order);

        return toDto(order, descriptor);
    }

    @Transactional
    public ClinicalOrderDto markInProgress(String uid) {
        ClinicalOrder order = loadOrThrow(uid);
        order.markInProgress();
        return toDto(order);
    }

    @Transactional
    public ClinicalOrderDto complete(String uid, CompleteOrderRequest request) {
        ClinicalOrder order = loadOrThrow(uid);
        order.complete(emptyToNull(request == null ? null : request.result()));
        return toDto(order);
    }

    @Transactional
    public ClinicalOrderDto cancel(String uid, CancelOrderRequest request) {
        ClinicalOrder order = loadOrThrow(uid);
        order.cancel(emptyToNull(request == null ? null : request.reason()));
        return toDto(order);
    }

    /**
     * Books a theatre + start time for a PROCEDURE order. Idempotent —
     * re-scheduling a still-open procedure overwrites the booking.
     */
    @Transactional
    public ClinicalOrderDto schedule(String uid, ScheduleOrderRequest request) {
        ClinicalOrder order = loadOrThrow(uid);
        Theatre theatre = theatreRepository.findByUid(request.theatreUid())
                .orElseThrow(() -> new NotFoundException("Theatre not found: " + request.theatreUid()));
        if (!theatre.isActive()) {
            throw new BusinessRuleException("Theatre is not active: " + theatre.getName());
        }
        order.schedule(theatre.getUid(), request.scheduledAt(), currentUsername());
        return toDto(order);
    }

    @Transactional(readOnly = true)
    public List<ClinicalOrderDto> listForConsultation(String consultationUid) {
        return orderRepository.findAllByConsultationUidOrderByRequestedAtDesc(consultationUid).stream()
                .map(this::toDto)
                .toList();
    }

    /** All outsider-direct orders for a patient (consultation_uid IS NULL). */
    @Transactional(readOnly = true)
    public List<ClinicalOrderDto> listOutsiderForPatient(String patientUid) {
        return orderRepository.findAllByPatientUidAndConsultationUidIsNullOrderByRequestedAtDesc(patientUid).stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * Cross-patient worklist for the Orders &amp; Results module — kind (the
     * per-role lens), status, patient-class, and settled filters. OUTSIDER =
     * raised directly on the patient; INPATIENT = consultation-bound for a
     * patient with an active admission; OUTPATIENT = consultation-bound, no
     * active admission.
     */
    @Transactional(readOnly = true)
    public PageResponse<OrderWorklistDto> searchWorklist(ClinicalOrderKind kind, ClinicalOrderStatus status,
                                                         PatientClassScope scope, boolean settledOnly, Pageable pageable) {
        Pageable effective = pageable.getSort().isSorted()
                ? pageable
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "requestedAt"));
        return PageResponse.from(orderRepository
                .searchWorklist(kind, status, scope == null ? null : scope.name(), settledOnly, effective)
                .map(this::toWorklistDto));
    }

    /** Idempotent — flips this order's settled flag. Called by the billing settlement dispatcher. */
    @Transactional
    public void markSettled(String orderUid) {
        orderRepository.findByUid(orderUid).ifPresent(ClinicalOrder::markSettled);
    }

    private OrderWorklistDto toWorklistDto(ClinicalOrder o) {
        ServiceDescriptor s = resolveServiceQuietly(o.getKind(), o.getServiceUid());
        Patient p = patientRepository.findByUid(o.getPatientUid()).orElse(null);
        return new OrderWorklistDto(
                o.getUid(),
                o.getOrderNo(),
                o.getKind(),
                s.code(),
                s.name(),
                o.getStatus(),
                o.getUrgency(),
                o.getRequestedAt(),
                o.getCompletedAt(),
                o.getPatientUid(),
                p == null ? null : p.getPatientNo(),
                p == null ? null : p.fullName(),
                resolvePatientClass(o),
                o.isSettled(),
                o.getConsultationUid());
    }

    private PatientClassScope resolvePatientClass(ClinicalOrder o) {
        if (o.getConsultationUid() == null) {
            return PatientClassScope.OUTSIDER;
        }
        return admissionRepository.existsByPatientUidAndStatus(o.getPatientUid(), AdmissionStatus.ADMITTED)
                ? PatientClassScope.INPATIENT
                : PatientClassScope.OUTPATIENT;
    }

    private ClinicalOrder loadOrThrow(String uid) {
        return orderRepository.findByUid(uid)
                .orElseThrow(() -> new NotFoundException("Order not found: " + uid));
    }

    private ServiceDescriptor resolveService(ClinicalOrderKind kind, String uid) {
        return switch (kind) {
            case LAB_TEST -> {
                LabTestType t = labTestTypeRepository.findByUid(uid)
                        .orElseThrow(() -> new NotFoundException("Lab test not found: " + uid));
                yield new ServiceDescriptor(t.getUid(), t.getCode(), t.getName());
            }
            case RADIOLOGY -> {
                RadiologyType t = radiologyTypeRepository.findByUid(uid)
                        .orElseThrow(() -> new NotFoundException("Radiology type not found: " + uid));
                yield new ServiceDescriptor(t.getUid(), t.getCode(), t.getName());
            }
            case PROCEDURE -> {
                ProcedureType t = procedureTypeRepository.findByUid(uid)
                        .orElseThrow(() -> new NotFoundException("Procedure not found: " + uid));
                yield new ServiceDescriptor(t.getUid(), t.getCode(), t.getName());
            }
        };
    }

    private ClinicalOrderDto toDto(ClinicalOrder o) {
        return toDto(o, resolveServiceQuietly(o.getKind(), o.getServiceUid()));
    }

    private ServiceDescriptor resolveServiceQuietly(ClinicalOrderKind kind, String uid) {
        return switch (kind) {
            case LAB_TEST   -> labTestTypeRepository.findByUid(uid).map(t -> new ServiceDescriptor(t.getUid(), t.getCode(), t.getName())).orElse(new ServiceDescriptor(uid, null, null));
            case RADIOLOGY  -> radiologyTypeRepository.findByUid(uid).map(t -> new ServiceDescriptor(t.getUid(), t.getCode(), t.getName())).orElse(new ServiceDescriptor(uid, null, null));
            case PROCEDURE  -> procedureTypeRepository.findByUid(uid).map(t -> new ServiceDescriptor(t.getUid(), t.getCode(), t.getName())).orElse(new ServiceDescriptor(uid, null, null));
        };
    }

    private ClinicalOrderDto toDto(ClinicalOrder o, ServiceDescriptor s) {
        String theatreName = o.getTheatreUid() == null
                ? null
                : theatreRepository.findByUid(o.getTheatreUid()).map(Theatre::getName).orElse(null);
        return new ClinicalOrderDto(
                o.getUid(),
                o.getOrderNo(),
                o.getConsultationUid(),
                o.getPatientUid(),
                o.getKind(),
                s.uid(),
                s.code(),
                s.name(),
                o.getStatus(),
                o.getUrgency(),
                o.getRequestedAt(),
                o.getCompletedAt(),
                o.getInstructions(),
                o.getResult(),
                o.getCancelReason(),
                o.getTheatreUid(),
                theatreName,
                o.getScheduledAt(),
                o.getScheduledByUsername(),
                o.getCreatedAt(),
                o.getUpdatedAt());
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

    private record ServiceDescriptor(String uid, String code, String name) {}
}
