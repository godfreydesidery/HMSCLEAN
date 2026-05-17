package com.otapp.hmis.engine.procurement.supplier.application;

import com.otapp.hmis.engine.common.api.PageResponse;
import com.otapp.hmis.engine.common.error.BusinessRuleException;
import com.otapp.hmis.engine.common.error.NotFoundException;
import com.otapp.hmis.engine.procurement.supplier.application.SupplierDtos.CreateSupplierRequest;
import com.otapp.hmis.engine.procurement.supplier.application.SupplierDtos.SupplierDto;
import com.otapp.hmis.engine.procurement.supplier.application.SupplierDtos.UpdateSupplierRequest;
import com.otapp.hmis.engine.procurement.supplier.domain.Supplier;
import com.otapp.hmis.engine.procurement.supplier.domain.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SupplierService {

    private final SupplierRepository supplierRepository;

    @Transactional
    public SupplierDto create(CreateSupplierRequest request) {
        if (supplierRepository.existsByCode(request.code().trim())) {
            throw new BusinessRuleException("Supplier code already in use: " + request.code());
        }
        Supplier supplier = new Supplier(
                request.code().trim(),
                request.name().trim(),
                emptyToNull(request.contactName()),
                emptyToNull(request.phone()),
                emptyToNull(request.email()),
                emptyToNull(request.address()),
                emptyToNull(request.taxId()),
                emptyToNull(request.notes()));
        supplierRepository.save(supplier);
        return toDto(supplier);
    }

    @Transactional
    public SupplierDto update(String uid, UpdateSupplierRequest request) {
        Supplier supplier = loadOrThrow(uid);
        supplier.setName(request.name().trim());
        supplier.setContactName(emptyToNull(request.contactName()));
        supplier.setPhone(emptyToNull(request.phone()));
        supplier.setEmail(emptyToNull(request.email()));
        supplier.setAddress(emptyToNull(request.address()));
        supplier.setTaxId(emptyToNull(request.taxId()));
        supplier.setNotes(emptyToNull(request.notes()));
        return toDto(supplier);
    }

    @Transactional
    public SupplierDto setActive(String uid, boolean active) {
        Supplier supplier = loadOrThrow(uid);
        if (active) supplier.activate(); else supplier.deactivate();
        return toDto(supplier);
    }

    @Transactional(readOnly = true)
    public SupplierDto findByUid(String uid) {
        return toDto(loadOrThrow(uid));
    }

    @Transactional(readOnly = true)
    public PageResponse<SupplierDto> search(String query, Boolean active, Pageable pageable) {
        return PageResponse.from(
                supplierRepository.search(query == null ? null : query.trim(), active, pageable)
                        .map(SupplierService::toDto));
    }

    private Supplier loadOrThrow(String uid) {
        return supplierRepository.findByUid(uid)
                .orElseThrow(() -> new NotFoundException("Supplier not found: " + uid));
    }

    private static SupplierDto toDto(Supplier s) {
        return new SupplierDto(
                s.getUid(),
                s.getCode(),
                s.getName(),
                s.getContactName(),
                s.getPhone(),
                s.getEmail(),
                s.getAddress(),
                s.getTaxId(),
                s.getNotes(),
                s.isActive(),
                s.getCreatedAt(),
                s.getUpdatedAt());
    }

    private static String emptyToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
