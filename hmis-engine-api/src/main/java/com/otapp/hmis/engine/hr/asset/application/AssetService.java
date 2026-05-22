package com.otapp.hmis.engine.hr.asset.application;

import com.otapp.hmis.engine.common.api.PageResponse;
import com.otapp.hmis.engine.common.error.BusinessRuleException;
import com.otapp.hmis.engine.common.error.NotFoundException;
import com.otapp.hmis.engine.hr.asset.application.AssetDtos.AssetDto;
import com.otapp.hmis.engine.hr.asset.application.AssetDtos.CreateAssetRequest;
import com.otapp.hmis.engine.hr.asset.application.AssetDtos.RetireAssetRequest;
import com.otapp.hmis.engine.hr.asset.application.AssetDtos.UpdateAssetRequest;
import com.otapp.hmis.engine.hr.asset.domain.Asset;
import com.otapp.hmis.engine.hr.asset.domain.AssetRepository;
import com.otapp.hmis.engine.hr.asset.domain.AssetStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AssetService {

    private final AssetRepository assetRepository;

    @Transactional
    public AssetDto create(CreateAssetRequest request) {
        if (assetRepository.existsByTag(request.tag().trim())) {
            throw new BusinessRuleException("Asset tag already in use: " + request.tag());
        }
        Asset asset = new Asset(request.tag(), request.name());
        applyMutableFields(asset, request.category(), request.location(), request.description(),
                request.serialNo(), request.manufacturer(), request.model(),
                request.acquisitionDate(), request.acquisitionCost(), request.currency(),
                request.custodianUsername());
        assetRepository.save(asset);
        return toDto(asset);
    }

    @Transactional
    public AssetDto update(String uid, UpdateAssetRequest request) {
        Asset asset = loadOrThrow(uid);
        asset.setName(request.name().trim());
        applyMutableFields(asset, request.category(), request.location(), request.description(),
                request.serialNo(), request.manufacturer(), request.model(),
                request.acquisitionDate(), request.acquisitionCost(), request.currency(),
                request.custodianUsername());
        return toDto(asset);
    }

    @Transactional
    public AssetDto retire(String uid, RetireAssetRequest request) {
        Asset asset = loadOrThrow(uid);
        asset.retire(request.target(), request.date(), emptyToNull(request.reason()));
        return toDto(asset);
    }

    @Transactional
    public AssetDto reinstate(String uid) {
        Asset asset = loadOrThrow(uid);
        asset.reinstate();
        return toDto(asset);
    }

    @Transactional(readOnly = true)
    public AssetDto findByUid(String uid) {
        return toDto(loadOrThrow(uid));
    }

    @Transactional(readOnly = true)
    public AssetDto findByTag(String tag) {
        Asset asset = assetRepository.findByTag(tag)
                .orElseThrow(() -> new NotFoundException("Asset not found: " + tag));
        return toDto(asset);
    }

    @Transactional(readOnly = true)
    public PageResponse<AssetDto> search(String query, AssetStatus status, String category,
                                         String location, Pageable pageable) {
        return PageResponse.from(
                assetRepository.search(
                        query == null ? null : query.trim(),
                        status,
                        emptyToNull(category),
                        emptyToNull(location),
                        pageable).map(AssetService::toDto));
    }

    private Asset loadOrThrow(String uid) {
        return assetRepository.findByUid(uid)
                .orElseThrow(() -> new NotFoundException("Asset not found: " + uid));
    }

    @SuppressWarnings("java:S107") // shared setter for create + update; no benefit from grouping
    private static void applyMutableFields(Asset asset, String category, String location, String description,
                                           String serialNo, String manufacturer, String model,
                                           java.time.LocalDate acquisitionDate, java.math.BigDecimal acquisitionCost,
                                           String currency, String custodianUsername) {
        asset.setCategory(emptyToNull(category));
        asset.setLocation(emptyToNull(location));
        asset.setDescription(emptyToNull(description));
        asset.setSerialNo(emptyToNull(serialNo));
        asset.setManufacturer(emptyToNull(manufacturer));
        asset.setModel(emptyToNull(model));
        asset.setAcquisitionDate(acquisitionDate);
        asset.setAcquisitionCost(acquisitionCost);
        asset.setCurrency(emptyToNull(currency));
        asset.setCustodianUsername(emptyToNull(custodianUsername));
    }

    private static AssetDto toDto(Asset a) {
        return new AssetDto(
                a.getUid(),
                a.getTag(),
                a.getName(),
                a.getCategory(),
                a.getLocation(),
                a.getDescription(),
                a.getSerialNo(),
                a.getManufacturer(),
                a.getModel(),
                a.getAcquisitionDate(),
                a.getAcquisitionCost(),
                a.getCurrency(),
                a.getCustodianUsername(),
                a.getStatus(),
                a.getRetiredAt(),
                a.getRetiredReason(),
                a.getCreatedAt(),
                a.getUpdatedAt());
    }

    private static String emptyToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
