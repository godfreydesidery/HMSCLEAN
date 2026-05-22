package com.otapp.hmis.engine.procurement.pricelist.application;

import com.otapp.hmis.engine.common.error.BusinessRuleException;
import com.otapp.hmis.engine.common.error.NotFoundException;
import com.otapp.hmis.engine.masterdata.medicine.domain.Medicine;
import com.otapp.hmis.engine.masterdata.medicine.domain.MedicineRepository;
import com.otapp.hmis.engine.procurement.pricelist.application.SupplierItemPriceDtos.CreateSupplierItemPriceRequest;
import com.otapp.hmis.engine.procurement.pricelist.application.SupplierItemPriceDtos.SupplierItemPriceDto;
import com.otapp.hmis.engine.procurement.pricelist.application.SupplierItemPriceDtos.UpdateSupplierItemPriceRequest;
import com.otapp.hmis.engine.procurement.pricelist.domain.SupplierItemPrice;
import com.otapp.hmis.engine.procurement.pricelist.domain.SupplierItemPriceRepository;
import com.otapp.hmis.engine.procurement.supplier.domain.Supplier;
import com.otapp.hmis.engine.procurement.supplier.domain.SupplierRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SupplierItemPriceService {

    private final SupplierItemPriceRepository priceRepository;
    private final SupplierRepository supplierRepository;
    private final MedicineRepository medicineRepository;

    @Transactional
    public SupplierItemPriceDto create(String supplierUid, CreateSupplierItemPriceRequest request) {
        Supplier supplier = activeSupplier(supplierUid);
        Medicine medicine = activeMedicine(request.medicineUid());
        SupplierItemPrice price = priceRepository.save(new SupplierItemPrice(
                supplier.getUid(),
                medicine.getUid(),
                request.unitPrice(),
                emptyToNull(request.currency()),
                request.validFrom(),
                request.validTo(),
                emptyToNull(request.notes())));
        return toDto(price, supplier, medicine);
    }

    @Transactional
    public SupplierItemPriceDto update(String supplierUid, String priceUid,
                                       UpdateSupplierItemPriceRequest request) {
        SupplierItemPrice price = loadOwned(supplierUid, priceUid);
        if (request.unitPrice().signum() <= 0) {
            throw new BusinessRuleException("unitPrice must be positive");
        }
        if (request.validTo() != null && request.validTo().isBefore(price.getValidFrom())) {
            throw new BusinessRuleException("validTo cannot be before validFrom");
        }
        price.setUnitPrice(request.unitPrice());
        if (request.currency() != null && !request.currency().isBlank()) {
            price.setCurrency(request.currency().toUpperCase());
        }
        price.setValidTo(request.validTo());
        price.setNotes(emptyToNull(request.notes()));
        return toDto(price);
    }

    @Transactional
    public SupplierItemPriceDto setActive(String supplierUid, String priceUid, boolean active) {
        SupplierItemPrice price = loadOwned(supplierUid, priceUid);
        price.setActive(active);
        return toDto(price);
    }

    @Transactional
    public void delete(String supplierUid, String priceUid) {
        SupplierItemPrice price = loadOwned(supplierUid, priceUid);
        priceRepository.delete(price);
    }

    @Transactional(readOnly = true)
    public List<SupplierItemPriceDto> listForSupplier(String supplierUid) {
        activeSupplier(supplierUid);
        return priceRepository.findBySupplierUidOrderByValidFromDesc(supplierUid).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SupplierItemPriceDto> listForMedicine(String medicineUid) {
        activeMedicine(medicineUid);
        return priceRepository.findByMedicineUidOrderByValidFromDesc(medicineUid).stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * Cheapest currently-valid quote across all suppliers for a medicine,
     * if any. Used by procurement when picking a supplier for an LPO line.
     */
    @Transactional(readOnly = true)
    public Optional<SupplierItemPriceDto> findCurrentBest(String medicineUid) {
        activeMedicine(medicineUid);
        return priceRepository.findActiveForMedicine(medicineUid, LocalDate.now()).stream()
                .findFirst()
                .map(this::toDto);
    }

    @Transactional(readOnly = true)
    public List<SupplierItemPriceDto> listActiveForMedicine(String medicineUid) {
        activeMedicine(medicineUid);
        return priceRepository.findActiveForMedicine(medicineUid, LocalDate.now()).stream()
                .map(this::toDto)
                .toList();
    }

    // ----- helpers ---------------------------------------------------------

    private SupplierItemPrice loadOwned(String supplierUid, String priceUid) {
        SupplierItemPrice price = priceRepository.findByUid(priceUid)
                .orElseThrow(() -> new NotFoundException("Supplier price not found: " + priceUid));
        if (!price.getSupplierUid().equals(supplierUid)) {
            throw new BusinessRuleException("Price does not belong to this supplier");
        }
        return price;
    }

    private Supplier activeSupplier(String uid) {
        Supplier s = supplierRepository.findByUid(uid)
                .orElseThrow(() -> new NotFoundException("Supplier not found: " + uid));
        if (!s.isActive()) {
            throw new BusinessRuleException("Supplier is not active: " + s.getName());
        }
        return s;
    }

    private Medicine activeMedicine(String uid) {
        Medicine m = medicineRepository.findByUid(uid)
                .orElseThrow(() -> new NotFoundException("Medicine not found: " + uid));
        if (!m.isActive()) {
            throw new BusinessRuleException("Medicine is not active: " + m.getName());
        }
        return m;
    }

    private SupplierItemPriceDto toDto(SupplierItemPrice p) {
        Supplier supplier = supplierRepository.findByUid(p.getSupplierUid()).orElse(null);
        Medicine medicine = medicineRepository.findByUid(p.getMedicineUid()).orElse(null);
        return toDto(p, supplier, medicine);
    }

    private SupplierItemPriceDto toDto(SupplierItemPrice p, Supplier supplier, Medicine medicine) {
        return new SupplierItemPriceDto(
                p.getUid(),
                p.getSupplierUid(),
                supplier == null ? null : supplier.getName(),
                p.getMedicineUid(),
                medicine == null ? null : medicine.getCode(),
                medicine == null ? null : medicine.getName(),
                medicine == null ? null : medicine.getStrength(),
                p.getUnitPrice(),
                p.getCurrency(),
                p.getValidFrom(),
                p.getValidTo(),
                p.isActive(),
                p.isCurrentlyValid(),
                p.getNotes(),
                p.getCreatedAt(),
                p.getUpdatedAt());
    }

    private static String emptyToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
