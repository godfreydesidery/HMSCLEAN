package com.otapp.hmis.engine.encounter.result.application;

import com.otapp.hmis.engine.common.error.BusinessRuleException;
import com.otapp.hmis.engine.common.error.NotFoundException;
import com.otapp.hmis.engine.encounter.order.domain.ClinicalOrder;
import com.otapp.hmis.engine.encounter.order.domain.ClinicalOrderRepository;
import com.otapp.hmis.engine.encounter.order.domain.ClinicalOrderStatus;
import com.otapp.hmis.engine.encounter.result.application.OrderResultDtos.OrderResultDto;
import com.otapp.hmis.engine.encounter.result.application.OrderResultDtos.SaveResultRequest;
import com.otapp.hmis.engine.encounter.result.domain.OrderResult;
import com.otapp.hmis.engine.encounter.result.domain.OrderResultRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderResultService {

    private final OrderResultRepository resultRepository;
    private final ClinicalOrderRepository orderRepository;

    @Transactional(readOnly = true)
    public Optional<OrderResultDto> findForOrder(String orderUid) {
        return resultRepository.findByOrderUid(orderUid).map(OrderResultService::toDto);
    }

    /** Create or update a preliminary result; finalized results require {@link #amend}. */
    @Transactional
    public OrderResultDto save(String orderUid, SaveResultRequest request) {
        ClinicalOrder order = loadOrder(orderUid);
        if (order.getStatus() == ClinicalOrderStatus.CANCELLED) {
            throw new BusinessRuleException("Cannot record results on a cancelled order");
        }
        String narrative = emptyToNull(request.narrative());
        String impression = emptyToNull(request.impression());
        OrderResult result = resultRepository.findByOrderUid(orderUid)
                .map(existing -> {
                    existing.editPreliminary(narrative, impression);
                    return existing;
                })
                .orElseGet(() -> resultRepository.save(new OrderResult(orderUid, order.getKind(), narrative, impression)));

        // Move REQUESTED -> IN_PROGRESS the moment a result entry begins.
        if (order.getStatus() == ClinicalOrderStatus.REQUESTED) {
            order.markInProgress();
        }
        return toDto(result);
    }

    @Transactional
    public OrderResultDto finalizeResult(String orderUid) {
        ClinicalOrder order = loadOrder(orderUid);
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
        loadOrder(orderUid);
        OrderResult result = resultRepository.findByOrderUid(orderUid)
                .orElseThrow(() -> new NotFoundException("No result recorded for order: " + orderUid));
        result.amend(emptyToNull(request.narrative()), emptyToNull(request.impression()), currentUsername());
        return toDto(result);
    }

    private ClinicalOrder loadOrder(String orderUid) {
        return orderRepository.findByUid(orderUid)
                .orElseThrow(() -> new NotFoundException("Order not found: " + orderUid));
    }

    private static String currentUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? null : auth.getName();
    }

    private static OrderResultDto toDto(OrderResult r) {
        return new OrderResultDto(
                r.getUid(),
                r.getOrderUid(),
                r.getOrderKind(),
                r.getStatus(),
                r.getNarrative(),
                r.getImpression(),
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
