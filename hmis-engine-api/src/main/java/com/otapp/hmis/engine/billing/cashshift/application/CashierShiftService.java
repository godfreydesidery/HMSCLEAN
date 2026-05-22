package com.otapp.hmis.engine.billing.cashshift.application;

import com.otapp.hmis.engine.billing.cashshift.application.CashierShiftDtos.CashierShiftDto;
import com.otapp.hmis.engine.billing.cashshift.application.CashierShiftDtos.CloseShiftRequest;
import com.otapp.hmis.engine.billing.cashshift.application.CashierShiftDtos.OpenShiftRequest;
import com.otapp.hmis.engine.billing.cashshift.domain.CashierShift;
import com.otapp.hmis.engine.billing.cashshift.domain.CashierShiftRepository;
import com.otapp.hmis.engine.billing.cashshift.domain.CashierShiftStatus;
import com.otapp.hmis.engine.billing.payment.domain.PaymentRepository;
import com.otapp.hmis.engine.common.api.PageResponse;
import com.otapp.hmis.engine.common.error.BusinessRuleException;
import com.otapp.hmis.engine.common.error.NotFoundException;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CashierShiftService {

    private final CashierShiftRepository shiftRepository;
    private final PaymentRepository paymentRepository;

    @Transactional
    public CashierShiftDto open(OpenShiftRequest request) {
        String username = currentUsername();
        shiftRepository.findFirstByCashierUsernameAndStatus(username, CashierShiftStatus.OPEN)
                .ifPresent(open -> {
                    throw new BusinessRuleException(
                            "Cashier already has an open shift: " + open.getUid());
                });
        CashierShift shift = shiftRepository.save(new CashierShift(
                username,
                emptyToNull(request.currency()),
                request.openingFloat()));
        return toDto(shift);
    }

    /**
     * Close the current user's open shift. Recomputes the expected cash
     * from {@link PaymentRepository#sumCashByUserInRange(String, Instant, Instant)}
     * and records both the declared amount and the variance.
     */
    @Transactional
    public CashierShiftDto close(CloseShiftRequest request) {
        String username = currentUsername();
        CashierShift shift = shiftRepository
                .findFirstByCashierUsernameAndStatus(username, CashierShiftStatus.OPEN)
                .orElseThrow(() -> new BusinessRuleException("No open shift to close"));
        BigDecimal expectedTakings = paymentRepository.sumCashByUserInRange(
                username, shift.getOpenedAt(), Instant.now());
        shift.close(request.closingDeclaredAmount(),
                expectedTakings,
                emptyToNull(request.note()));
        return toDto(shift);
    }

    @Transactional(readOnly = true)
    public CashierShiftDto currentOpen() {
        String username = currentUsername();
        return shiftRepository.findFirstByCashierUsernameAndStatus(username, CashierShiftStatus.OPEN)
                .map(CashierShiftService::toDto)
                .orElseThrow(() -> new NotFoundException("No open shift for " + username));
    }

    @Transactional(readOnly = true)
    public CashierShiftDto findByUid(String uid) {
        return toDto(loadOrThrow(uid));
    }

    @Transactional(readOnly = true)
    public PageResponse<CashierShiftDto> search(String username,
                                                CashierShiftStatus status,
                                                Pageable pageable) {
        return PageResponse.from(
                shiftRepository.search(emptyToNull(username), status, pageable)
                        .map(CashierShiftService::toDto));
    }

    private CashierShift loadOrThrow(String uid) {
        return shiftRepository.findByUid(uid)
                .orElseThrow(() -> new NotFoundException("Cashier shift not found: " + uid));
    }

    private static CashierShiftDto toDto(CashierShift s) {
        return new CashierShiftDto(
                s.getUid(),
                s.getCashierUsername(),
                s.getCurrency(),
                s.getOpeningFloat(),
                s.getOpenedAt(),
                s.getStatus(),
                s.getClosedAt(),
                s.getClosingDeclaredAmount(),
                s.getClosingExpectedAmount(),
                s.getVariance(),
                s.getClosingNote(),
                s.getCreatedAt(),
                s.getUpdatedAt());
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
