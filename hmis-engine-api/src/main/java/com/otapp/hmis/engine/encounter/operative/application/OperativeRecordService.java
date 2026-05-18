package com.otapp.hmis.engine.encounter.operative.application;

import com.otapp.hmis.engine.common.error.BusinessRuleException;
import com.otapp.hmis.engine.common.error.NotFoundException;
import com.otapp.hmis.engine.encounter.operative.application.OperativeRecordDtos.OperativeRecordDto;
import com.otapp.hmis.engine.encounter.operative.application.OperativeRecordDtos.UpsertOperativeRecordRequest;
import com.otapp.hmis.engine.encounter.operative.domain.OperativeRecord;
import com.otapp.hmis.engine.encounter.operative.domain.OperativeRecordRepository;
import com.otapp.hmis.engine.encounter.order.domain.ClinicalOrder;
import com.otapp.hmis.engine.encounter.order.domain.ClinicalOrderKind;
import com.otapp.hmis.engine.encounter.order.domain.ClinicalOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OperativeRecordService {

    private final OperativeRecordRepository recordRepository;
    private final ClinicalOrderRepository orderRepository;

    /**
     * Upsert the editable fields on the operative record for a procedure
     * order. Creates the record on first call; updates in place on later
     * calls while the record is still unlocked.
     */
    @Transactional
    public OperativeRecordDto upsert(String orderUid, UpsertOperativeRecordRequest request) {
        ClinicalOrder order = requireProcedureOrder(orderUid);
        OperativeRecord record = recordRepository.findByOrderUid(order.getUid())
                .orElseGet(() -> recordRepository.save(
                        new OperativeRecord(order.getUid(), currentUsername())));
        record.requireEditable();
        apply(record, request);
        return toDto(record);
    }

    @Transactional
    public OperativeRecordDto lock(String orderUid) {
        OperativeRecord record = loadByOrder(orderUid);
        record.lock(currentUsername());
        return toDto(record);
    }

    @Transactional(readOnly = true)
    public OperativeRecordDto findByOrder(String orderUid) {
        return toDto(loadByOrder(orderUid));
    }

    // ----- helpers ---------------------------------------------------------

    private ClinicalOrder requireProcedureOrder(String orderUid) {
        ClinicalOrder order = orderRepository.findByUid(orderUid)
                .orElseThrow(() -> new NotFoundException("Order not found: " + orderUid));
        if (order.getKind() != ClinicalOrderKind.PROCEDURE) {
            throw new BusinessRuleException(
                    "Operative records only attach to PROCEDURE orders (kind: " + order.getKind() + ")");
        }
        return order;
    }

    private OperativeRecord loadByOrder(String orderUid) {
        return recordRepository.findByOrderUid(orderUid)
                .orElseThrow(() -> new NotFoundException(
                        "No operative record for order: " + orderUid));
    }

    private static void apply(OperativeRecord r, UpsertOperativeRecordRequest req) {
        r.setFindings(emptyToNull(req.findings()));
        r.setTechnique(emptyToNull(req.technique()));
        r.setInstruments(emptyToNull(req.instruments()));
        r.setComplications(emptyToNull(req.complications()));
        r.setSpecimens(emptyToNull(req.specimens()));
        r.setSurgeonUsername(emptyToNull(req.surgeonUsername()));
        r.setAssistants(emptyToNull(req.assistants()));
        r.setAnaesthetistUsername(emptyToNull(req.anaesthetistUsername()));
        r.setAnaesthesiaType(emptyToNull(req.anaesthesiaType()));
        r.setScrubNurse(emptyToNull(req.scrubNurse()));
        r.setCirculatingNurse(emptyToNull(req.circulatingNurse()));
        r.setStartedAt(req.startedAt());
        r.setEndedAt(req.endedAt());
    }

    private OperativeRecordDto toDto(OperativeRecord r) {
        return new OperativeRecordDto(
                r.getUid(),
                r.getOrderUid(),
                r.getFindings(), r.getTechnique(), r.getInstruments(),
                r.getComplications(), r.getSpecimens(),
                r.getSurgeonUsername(), r.getAssistants(),
                r.getAnaesthetistUsername(), r.getAnaesthesiaType(),
                r.getScrubNurse(), r.getCirculatingNurse(),
                r.getStartedAt(), r.getEndedAt(),
                r.getAuthoredByUsername(), r.getAuthoredAt(),
                r.getLockedAt(), r.getLockedByUsername(),
                r.getCreatedAt(), r.getUpdatedAt());
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
