package com.otapp.hmis.engine.encounter.order.application;

import com.otapp.hmis.engine.common.error.NotFoundException;
import com.otapp.hmis.engine.encounter.consultation.domain.Consultation;
import com.otapp.hmis.engine.encounter.consultation.domain.ConsultationRepository;
import com.otapp.hmis.engine.encounter.order.application.ClinicalOrderDtos.CancelOrderRequest;
import com.otapp.hmis.engine.encounter.order.application.ClinicalOrderDtos.ClinicalOrderDto;
import com.otapp.hmis.engine.encounter.order.application.ClinicalOrderDtos.CompleteOrderRequest;
import com.otapp.hmis.engine.encounter.order.application.ClinicalOrderDtos.CreateOrderRequest;
import com.otapp.hmis.engine.encounter.order.domain.ClinicalOrder;
import com.otapp.hmis.engine.encounter.order.domain.ClinicalOrderKind;
import com.otapp.hmis.engine.encounter.order.domain.ClinicalOrderRepository;
import com.otapp.hmis.engine.encounter.order.infrastructure.OrderNumberGenerator;
import com.otapp.hmis.engine.masterdata.labtest.domain.LabTestType;
import com.otapp.hmis.engine.masterdata.labtest.domain.LabTestTypeRepository;
import com.otapp.hmis.engine.masterdata.procedure.domain.ProcedureType;
import com.otapp.hmis.engine.masterdata.procedure.domain.ProcedureTypeRepository;
import com.otapp.hmis.engine.masterdata.radiology.domain.RadiologyType;
import com.otapp.hmis.engine.masterdata.radiology.domain.RadiologyTypeRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClinicalOrderService {

    private final ClinicalOrderRepository orderRepository;
    private final ConsultationRepository consultationRepository;
    private final LabTestTypeRepository labTestTypeRepository;
    private final RadiologyTypeRepository radiologyTypeRepository;
    private final ProcedureTypeRepository procedureTypeRepository;
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

    @Transactional(readOnly = true)
    public List<ClinicalOrderDto> listForConsultation(String consultationUid) {
        return orderRepository.findAllByConsultationUidOrderByRequestedAtDesc(consultationUid).stream()
                .map(this::toDto)
                .toList();
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

    private static ClinicalOrderDto toDto(ClinicalOrder o, ServiceDescriptor s) {
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
                o.getCreatedAt(),
                o.getUpdatedAt());
    }

    private static String emptyToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private record ServiceDescriptor(String uid, String code, String name) {}
}
