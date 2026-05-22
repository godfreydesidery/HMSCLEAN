package com.otapp.hmis.engine.encounter.consumable.application;

import com.otapp.hmis.engine.common.error.BusinessRuleException;
import com.otapp.hmis.engine.common.error.NotFoundException;
import com.otapp.hmis.engine.encounter.consumable.application.ConsumableIssueDtos.AdjustConsumableRequest;
import com.otapp.hmis.engine.encounter.consumable.application.ConsumableIssueDtos.ConsumableStockBalanceDto;
import com.otapp.hmis.engine.encounter.consumable.application.ConsumableIssueDtos.ReceiveConsumableRequest;
import com.otapp.hmis.engine.encounter.consumable.domain.ConsumableMovementKind;
import com.otapp.hmis.engine.encounter.consumable.domain.ConsumableSourceKind;
import com.otapp.hmis.engine.encounter.consumable.domain.ConsumableStockBalance;
import com.otapp.hmis.engine.encounter.consumable.domain.ConsumableStockBalanceRepository;
import com.otapp.hmis.engine.encounter.consumable.domain.ConsumableStockMovement;
import com.otapp.hmis.engine.encounter.consumable.domain.ConsumableStockMovementRepository;
import com.otapp.hmis.engine.masterdata.consumable.domain.Consumable;
import com.otapp.hmis.engine.masterdata.consumable.domain.ConsumableRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tracks consumable stock at store / pharmacy locations (no batches —
 * see {@code ConsumableStockBalance} class doc for the rationale).
 * Drives both the manual receive / adjust endpoints and the
 * decrement-on-issue path called by {@link ConsumableIssueService}.
 */
@Service
@RequiredArgsConstructor
public class ConsumableStockService {

    private final ConsumableStockBalanceRepository balanceRepository;
    private final ConsumableStockMovementRepository movementRepository;
    private final ConsumableRepository consumableRepository;

    @Transactional
    public ConsumableStockBalanceDto receive(ReceiveConsumableRequest request) {
        if (request.quantity() <= 0) {
            throw new BusinessRuleException("Receipt quantity must be positive");
        }
        Consumable consumable = activeConsumable(request.consumableUid());
        ConsumableStockBalance balance = lockOrCreate(
                request.sourceKind(), request.sourceLocationUid(), consumable.getUid());
        balance.applyDelta(request.quantity());
        recordMovement(balance, ConsumableMovementKind.RECEIPT, request.quantity(),
                null, emptyToNull(request.note()));
        return toDto(balance, consumable);
    }

    @Transactional
    public ConsumableStockBalanceDto adjust(AdjustConsumableRequest request) {
        if (request.delta() == 0) {
            throw new BusinessRuleException("Adjustment delta must be non-zero");
        }
        Consumable consumable = activeConsumable(request.consumableUid());
        ConsumableStockBalance balance = lockOrCreate(
                request.sourceKind(), request.sourceLocationUid(), consumable.getUid());
        balance.applyDelta(request.delta());
        recordMovement(balance, ConsumableMovementKind.ADJUSTMENT, request.delta(),
                null, emptyToNull(request.note()));
        return toDto(balance, consumable);
    }

    /**
     * Decrement on issue — called from {@link ConsumableIssueService} when
     * a nurse charts a consumable used on an admission. Refuses if the
     * source location does not have enough on hand.
     */
    @Transactional
    public void decrementForIssue(ConsumableSourceKind sourceKind, String sourceLocationUid,
                                  String consumableUid, int quantity, String referenceUid) {
        if (quantity <= 0) {
            throw new BusinessRuleException("Issue quantity must be positive");
        }
        ConsumableStockBalance balance = balanceRepository
                .lockForUpdate(sourceKind, sourceLocationUid, consumableUid)
                .orElseThrow(() -> new BusinessRuleException(
                        "No consumable stock at " + sourceKind + " " + sourceLocationUid
                                + " for consumable " + consumableUid));
        balance.applyDelta(-quantity);
        recordMovement(balance, ConsumableMovementKind.ISSUE_TO_WARD, -quantity,
                referenceUid, null);
    }

    @Transactional(readOnly = true)
    public List<ConsumableStockBalanceDto> listBalances(ConsumableSourceKind sourceKind, String sourceUid) {
        return balanceRepository.findAllBySource(sourceKind, sourceUid).stream()
                .map(b -> toDto(b, consumableRepository.findByUid(b.getConsumableUid()).orElse(null)))
                .toList();
    }

    // ----- helpers -----------------------------------------------------------

    private ConsumableStockBalance lockOrCreate(ConsumableSourceKind sourceKind, String sourceUid,
                                                String consumableUid) {
        return balanceRepository.lockForUpdate(sourceKind, sourceUid, consumableUid)
                .orElseGet(() -> balanceRepository.save(
                        new ConsumableStockBalance(sourceKind, sourceUid, consumableUid)));
    }

    private Consumable activeConsumable(String uid) {
        Consumable c = consumableRepository.findByUid(uid)
                .orElseThrow(() -> new NotFoundException("Consumable not found: " + uid));
        if (!c.isActive()) {
            throw new BusinessRuleException("Consumable is not active: " + c.getName());
        }
        return c;
    }

    private void recordMovement(ConsumableStockBalance balance, ConsumableMovementKind kind,
                                int signedQuantity, String referenceUid, String note) {
        movementRepository.save(new ConsumableStockMovement(
                balance.getSourceKind(), balance.getSourceUid(), balance.getConsumableUid(),
                kind, signedQuantity, balance.getQuantity(),
                referenceUid, note, currentUsername()));
    }

    private static ConsumableStockBalanceDto toDto(ConsumableStockBalance b, Consumable c) {
        return new ConsumableStockBalanceDto(
                b.getUid(),
                b.getSourceKind(),
                b.getSourceUid(),
                b.getConsumableUid(),
                c == null ? null : c.getCode(),
                c == null ? null : c.getName(),
                b.getQuantity(),
                b.getUpdatedAt());
    }

    private static String emptyToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String currentUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? null : auth.getName();
    }
}
