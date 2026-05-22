package com.otapp.hmis.engine.masterdata.currency.application;

import com.otapp.hmis.engine.common.api.PageResponse;
import com.otapp.hmis.engine.common.error.BusinessRuleException;
import com.otapp.hmis.engine.common.error.ConflictException;
import com.otapp.hmis.engine.common.error.NotFoundException;
import com.otapp.hmis.engine.masterdata.currency.application.CurrencyDtos.CreateCurrencyRequest;
import com.otapp.hmis.engine.masterdata.currency.application.CurrencyDtos.CurrencyDto;
import com.otapp.hmis.engine.masterdata.currency.application.CurrencyDtos.UpdateCurrencyRequest;
import com.otapp.hmis.engine.masterdata.currency.domain.Currency;
import com.otapp.hmis.engine.masterdata.currency.domain.CurrencyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CurrencyService {

    /** Last-resort fallback if no currency is flagged as default (should not happen post-seed). */
    private static final String FALLBACK_CODE = "TZS";

    private final CurrencyRepository currencyRepository;

    @Transactional
    public CurrencyDto create(CreateCurrencyRequest request) {
        String code = request.code().trim().toUpperCase();
        if (currencyRepository.existsByCode(code)) {
            throw new ConflictException("Currency code already exists: " + code);
        }
        Currency currency = new Currency(code, request.name().trim(), trimToNull(request.symbol()));
        currencyRepository.save(currency);
        if (request.makeDefault()) {
            applyDefault(currency);
        }
        return toDto(currency);
    }

    @Transactional
    public CurrencyDto update(String uid, UpdateCurrencyRequest request) {
        Currency c = loadOrThrow(uid);
        c.setName(request.name().trim());
        c.setSymbol(trimToNull(request.symbol()));
        return toDto(c);
    }

    @Transactional
    public CurrencyDto setActive(String uid, boolean active) {
        Currency c = loadOrThrow(uid);
        if (!active && c.isDefaultCurrency()) {
            throw new BusinessRuleException("Cannot deactivate the default currency — set another default first");
        }
        if (active) c.activate(); else c.deactivate();
        return toDto(c);
    }

    @Transactional
    public CurrencyDto setDefault(String uid) {
        Currency target = loadOrThrow(uid);
        if (!target.isActive()) {
            throw new BusinessRuleException("Cannot make an inactive currency the default");
        }
        if (!target.isDefaultCurrency()) {
            applyDefault(target);
        }
        return toDto(target);
    }

    @Transactional
    public void delete(String uid) {
        Currency c = loadOrThrow(uid);
        if (c.isDefaultCurrency()) {
            throw new BusinessRuleException("Cannot delete the default currency — set another default first");
        }
        currencyRepository.delete(c);
    }

    @Transactional(readOnly = true)
    public CurrencyDto findByUid(String uid) {
        return toDto(loadOrThrow(uid));
    }

    @Transactional(readOnly = true)
    public CurrencyDto findDefault() {
        return currencyRepository.findByDefaultCurrencyTrue().map(CurrencyService::toDto).orElse(null);
    }

    /** The system default currency code, used as the billing fallback. */
    @Transactional(readOnly = true)
    public String defaultCode() {
        return currencyRepository.findByDefaultCurrencyTrue().map(Currency::getCode).orElse(FALLBACK_CODE);
    }

    /** Whether {@code code} is a known, active currency — used to validate price entry. */
    @Transactional(readOnly = true)
    public boolean isActiveCode(String code) {
        return code != null && currencyRepository.existsByCodeAndActiveTrue(code.toUpperCase());
    }

    @Transactional(readOnly = true)
    public PageResponse<CurrencyDto> search(String query, Boolean active, Pageable pageable) {
        String trimmed = query == null ? null : query.trim();
        return PageResponse.from(currencyRepository.search(trimmed, active, pageable).map(CurrencyService::toDto));
    }

    /** Clears any existing default (DB-level, dodging the partial unique index) and marks {@code target}. */
    private void applyDefault(Currency target) {
        currencyRepository.clearDefaultFlag();
        currencyRepository.flush();
        target.markDefault();
    }

    private Currency loadOrThrow(String uid) {
        return currencyRepository.findByUid(uid)
                .orElseThrow(() -> new NotFoundException("Currency not found: " + uid));
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static CurrencyDto toDto(Currency c) {
        return new CurrencyDto(c.getUid(), c.getCode(), c.getName(), c.getSymbol(),
                c.isDefaultCurrency(), c.isActive(), c.getCreatedAt(), c.getUpdatedAt());
    }
}
