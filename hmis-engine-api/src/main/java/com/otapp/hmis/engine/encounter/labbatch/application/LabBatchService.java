package com.otapp.hmis.engine.encounter.labbatch.application;

import com.otapp.hmis.engine.common.api.PageResponse;
import com.otapp.hmis.engine.common.error.BusinessRuleException;
import com.otapp.hmis.engine.common.error.NotFoundException;
import com.otapp.hmis.engine.encounter.labbatch.application.LabBatchDtos.CancelLabBatchRequest;
import com.otapp.hmis.engine.encounter.labbatch.application.LabBatchDtos.CreateLabBatchRequest;
import com.otapp.hmis.engine.encounter.labbatch.application.LabBatchDtos.LabBatchDto;
import com.otapp.hmis.engine.encounter.labbatch.domain.LabBatch;
import com.otapp.hmis.engine.encounter.labbatch.domain.LabBatchMember;
import com.otapp.hmis.engine.encounter.labbatch.domain.LabBatchMemberRepository;
import com.otapp.hmis.engine.encounter.labbatch.domain.LabBatchRepository;
import com.otapp.hmis.engine.encounter.labbatch.domain.LabBatchStatus;
import com.otapp.hmis.engine.encounter.labbatch.infrastructure.LabBatchNumberGenerator;
import com.otapp.hmis.engine.encounter.order.domain.ClinicalOrder;
import com.otapp.hmis.engine.encounter.order.domain.ClinicalOrderKind;
import com.otapp.hmis.engine.encounter.order.domain.ClinicalOrderRepository;
import com.otapp.hmis.engine.masterdata.labtest.domain.LabTestType;
import com.otapp.hmis.engine.masterdata.labtest.domain.LabTestTypeRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LabBatchService {

    private final LabBatchRepository batchRepository;
    private final LabBatchMemberRepository memberRepository;
    private final ClinicalOrderRepository orderRepository;
    private final LabTestTypeRepository labTestTypeRepository;
    private final LabBatchNumberGenerator numberGenerator;

    @Transactional
    public LabBatchDto create(CreateLabBatchRequest request) {
        LabTestType type = labTestTypeRepository.findByUid(request.labTestTypeUid())
                .orElseThrow(() -> new NotFoundException("Lab test type not found: " + request.labTestTypeUid()));
        if (!type.isActive()) {
            throw new BusinessRuleException("Lab test type is not active: " + type.getName());
        }
        if (request.orderUids() == null || request.orderUids().isEmpty()) {
            throw new BusinessRuleException("A batch must contain at least one order");
        }

        LabBatch batch = new LabBatch(numberGenerator.next(), type.getUid(),
                currentUsername(), emptyToNull(request.note()));
        batchRepository.save(batch);

        for (String orderUid : request.orderUids()) {
            attach(batch, type.getUid(), orderUid);
        }
        return toDto(batch);
    }

    @Transactional
    public LabBatchDto addOrder(String batchUid, String orderUid) {
        LabBatch batch = loadOrThrow(batchUid);
        if (!batch.isOpenForMembership()) {
            throw new BusinessRuleException("Cannot add members to a " + batch.getStatus() + " batch");
        }
        attach(batch, batch.getLabTestTypeUid(), orderUid);
        return toDto(batch);
    }

    @Transactional
    public LabBatchDto removeOrder(String batchUid, String orderUid) {
        LabBatch batch = loadOrThrow(batchUid);
        if (!batch.isOpenForMembership()) {
            throw new BusinessRuleException("Cannot remove members from a " + batch.getStatus() + " batch");
        }
        memberRepository.deleteByBatchUidAndOrderUid(batch.getUid(), orderUid);
        return toDto(batch);
    }

    @Transactional
    public LabBatchDto markProcessing(String batchUid) {
        LabBatch batch = loadOrThrow(batchUid);
        if (memberRepository.countByBatchUid(batch.getUid()) == 0) {
            throw new BusinessRuleException("Cannot process an empty batch");
        }
        batch.markProcessing();
        return toDto(batch);
    }

    @Transactional
    public LabBatchDto markCompleted(String batchUid) {
        LabBatch batch = loadOrThrow(batchUid);
        if (memberRepository.countByBatchUid(batch.getUid()) == 0) {
            throw new BusinessRuleException("Cannot complete an empty batch");
        }
        batch.markCompleted();
        return toDto(batch);
    }

    @Transactional
    public LabBatchDto cancel(String batchUid, CancelLabBatchRequest request) {
        LabBatch batch = loadOrThrow(batchUid);
        batch.cancel(emptyToNull(request == null ? null : request.reason()));
        return toDto(batch);
    }

    @Transactional(readOnly = true)
    public LabBatchDto findByUid(String batchUid) {
        return toDto(loadOrThrow(batchUid));
    }

    @Transactional(readOnly = true)
    public PageResponse<LabBatchDto> search(LabBatchStatus status, String labTestTypeUid, Pageable pageable) {
        return PageResponse.from(
                batchRepository.search(status, emptyToNull(labTestTypeUid), pageable)
                        .map(this::toDto));
    }

    // ----- helpers -----------------------------------------------------------

    private void attach(LabBatch batch, String requiredLabTestUid, String orderUid) {
        ClinicalOrder order = orderRepository.findByUid(orderUid)
                .orElseThrow(() -> new NotFoundException("Order not found: " + orderUid));
        if (order.getKind() != ClinicalOrderKind.LAB_TEST) {
            throw new BusinessRuleException("Order " + orderUid + " is not a LAB_TEST (kind: " + order.getKind() + ")");
        }
        if (!order.getServiceUid().equals(requiredLabTestUid)) {
            throw new BusinessRuleException(
                    "Order " + orderUid + " is for a different lab test type than the batch");
        }
        memberRepository.findByOrderUid(orderUid).ifPresent(existing -> {
            throw new BusinessRuleException(
                    "Order " + orderUid + " is already in lab batch " + existing.getBatchUid());
        });
        memberRepository.save(new LabBatchMember(batch.getUid(), order.getUid()));
    }

    private LabBatch loadOrThrow(String uid) {
        return batchRepository.findByUid(uid)
                .orElseThrow(() -> new NotFoundException("Lab batch not found: " + uid));
    }

    private LabBatchDto toDto(LabBatch b) {
        LabTestType t = labTestTypeRepository.findByUid(b.getLabTestTypeUid()).orElse(null);
        List<LabBatchMember> members = memberRepository.findAllByBatchUidOrderByCreatedAtAsc(b.getUid());
        return new LabBatchDto(
                b.getUid(),
                b.getBatchNo(),
                b.getLabTestTypeUid(),
                t == null ? null : t.getCode(),
                t == null ? null : t.getName(),
                b.getNote(),
                b.getStatus(),
                b.getOpenedByUsername(),
                b.getOpenedAt(),
                b.getProcessedAt(),
                b.getCompletedAt(),
                b.getCancelledAt(),
                b.getCancelReason(),
                members.size(),
                members.stream().map(LabBatchMember::getOrderUid).toList(),
                b.getCreatedAt(),
                b.getUpdatedAt());
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
