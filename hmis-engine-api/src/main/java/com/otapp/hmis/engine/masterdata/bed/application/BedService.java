package com.otapp.hmis.engine.masterdata.bed.application;

import com.otapp.hmis.engine.common.error.BusinessRuleException;
import com.otapp.hmis.engine.common.error.ConflictException;
import com.otapp.hmis.engine.common.error.NotFoundException;
import com.otapp.hmis.engine.masterdata.bed.application.BedDtos.BedDto;
import com.otapp.hmis.engine.masterdata.bed.application.BedDtos.CreateBedRequest;
import com.otapp.hmis.engine.masterdata.bed.application.BedDtos.OutOfServiceRequest;
import com.otapp.hmis.engine.masterdata.bed.application.BedDtos.UpdateBedRequest;
import com.otapp.hmis.engine.masterdata.bed.domain.Bed;
import com.otapp.hmis.engine.masterdata.bed.domain.BedRepository;
import com.otapp.hmis.engine.masterdata.bed.domain.BedStatus;
import com.otapp.hmis.engine.masterdata.ward.domain.Ward;
import com.otapp.hmis.engine.masterdata.ward.domain.WardRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BedService {

    private final BedRepository repo;
    private final WardRepository wardRepository;

    @Transactional
    public BedDto create(String wardUid, CreateBedRequest request) {
        Ward ward = activeWard(wardUid);
        String label = request.label().trim();
        if (repo.existsByWardUidAndLabel(ward.getUid(), label)) {
            throw new ConflictException(
                    "Bed label already exists in this ward: " + label);
        }
        Bed b = repo.save(new Bed(ward.getUid(), label, emptyToNull(request.notes())));
        return toDto(b, ward);
    }

    @Transactional
    public BedDto update(String bedUid, UpdateBedRequest request) {
        Bed b = loadOrThrow(bedUid);
        String label = request.label().trim();
        if (!b.getLabel().equals(label)
                && repo.existsByWardUidAndLabel(b.getWardUid(), label)) {
            throw new ConflictException("Bed label already exists in this ward: " + label);
        }
        b.setLabel(label);
        b.setNotes(emptyToNull(request.notes()));
        return toDto(b);
    }

    @Transactional
    public BedDto setActive(String bedUid, boolean active) {
        Bed b = loadOrThrow(bedUid);
        if (!active && inUse(b)) {
            throw new BusinessRuleException(
                    "Cannot deactivate a bed that is in use (" + b.getStatus()
                            + ") — discharge, transfer or cancel the admission first");
        }
        b.setActive(active);
        return toDto(b);
    }

    @Transactional
    public BedDto markOutOfService(String bedUid, OutOfServiceRequest request) {
        Bed b = loadOrThrow(bedUid);
        // A bed RESERVED for a deposit-pending admission is committed to that patient —
        // taking it offline would orphan the reservation (and the deposit payment would
        // then have no bed to occupy). Cancel the admission first. (An OCCUPIED bed may
        // still be taken offline mid-stay, e.g. equipment failure — unchanged.)
        if (b.getStatus() == BedStatus.RESERVED) {
            throw new BusinessRuleException(
                    "Bed is RESERVED for a deposit-pending admission — cancel the admission "
                            + "before taking the bed out of service");
        }
        b.markOutOfService(request == null ? null : emptyToNull(request.reason()));
        return toDto(b);
    }

    @Transactional
    public BedDto markFree(String bedUid) {
        Bed b = loadOrThrow(bedUid);
        if (inUse(b)) {
            throw new BusinessRuleException(
                    "Bed is in use (" + b.getStatus() + ") — discharge or cancel the admission "
                            + "instead of manually freeing the bed");
        }
        b.markFree();
        return toDto(b);
    }

    @Transactional
    public void delete(String bedUid) {
        Bed b = loadOrThrow(bedUid);
        if (inUse(b)) {
            throw new BusinessRuleException("Cannot delete a bed that is in use (" + b.getStatus() + ")");
        }
        repo.delete(b);
    }

    /** A bed is "in use" — and so cannot be deactivated, freed or deleted — while it is
     *  physically OCCUPIED or held (RESERVED) for a deposit-pending admission. */
    private static boolean inUse(Bed b) {
        return b.getStatus() == BedStatus.OCCUPIED || b.getStatus() == BedStatus.RESERVED;
    }

    @Transactional(readOnly = true)
    public BedDto findByUid(String bedUid) { return toDto(loadOrThrow(bedUid)); }

    @Transactional(readOnly = true)
    public List<BedDto> listForWard(String wardUid) {
        activeWard(wardUid);
        return repo.findByWardUidOrderByLabelAsc(wardUid).stream()
                .map(this::toDto)
                .toList();
    }

    private Bed loadOrThrow(String uid) {
        return repo.findByUid(uid)
                .orElseThrow(() -> new NotFoundException("Bed not found: " + uid));
    }

    private Ward activeWard(String uid) {
        Ward w = wardRepository.findByUid(uid)
                .orElseThrow(() -> new NotFoundException("Ward not found: " + uid));
        if (!w.isActive()) {
            throw new BusinessRuleException("Ward is not active: " + w.getName());
        }
        return w;
    }

    private BedDto toDto(Bed b) {
        Ward w = wardRepository.findByUid(b.getWardUid()).orElse(null);
        return toDto(b, w);
    }

    private static BedDto toDto(Bed b, Ward w) {
        return new BedDto(
                b.getUid(), b.getWardUid(),
                w == null ? null : w.getName(),
                b.getLabel(), b.getNotes(),
                b.getStatus(), b.getOccupiedByAdmissionUid(),
                b.isActive(), b.getCreatedAt(), b.getUpdatedAt());
    }

    private static String emptyToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
