package com.otapp.hmis.engine.masterdata.clinic.application;

import com.otapp.hmis.engine.common.api.PageResponse;
import com.otapp.hmis.engine.common.error.ConflictException;
import com.otapp.hmis.engine.common.error.NotFoundException;
import com.otapp.hmis.engine.masterdata.clinic.application.dto.ClinicDto;
import com.otapp.hmis.engine.masterdata.clinic.application.dto.CreateClinicRequest;
import com.otapp.hmis.engine.masterdata.clinic.application.dto.UpdateClinicRequest;
import com.otapp.hmis.engine.masterdata.clinic.domain.Clinic;
import com.otapp.hmis.engine.masterdata.clinic.domain.ClinicRepository;
import com.otapp.hmis.engine.masterdata.clinic.domain.ClinicType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClinicService {

    private final ClinicRepository clinicRepository;

    @Transactional
    public ClinicDto create(CreateClinicRequest request) {
        String code = request.code().trim().toUpperCase();
        if (clinicRepository.existsByCode(code)) {
            throw new ConflictException("Clinic code already exists: " + code);
        }
        Clinic clinic = new Clinic(
                code,
                request.name().trim(),
                request.type(),
                request.description(),
                request.location());
        clinicRepository.save(clinic);
        return ClinicMapper.toDto(clinic);
    }

    @Transactional
    public ClinicDto update(String uid, UpdateClinicRequest request) {
        Clinic clinic = loadOrThrow(uid);
        clinic.setName(request.name().trim());
        clinic.setType(request.type());
        clinic.setDescription(request.description());
        clinic.setLocation(request.location());
        return ClinicMapper.toDto(clinic);
    }

    @Transactional
    public ClinicDto setActive(String uid, boolean active) {
        Clinic clinic = loadOrThrow(uid);
        if (active) {
            clinic.activate();
        } else {
            clinic.deactivate();
        }
        return ClinicMapper.toDto(clinic);
    }

    @Transactional
    public void delete(String uid) {
        clinicRepository.delete(loadOrThrow(uid));
    }

    @Transactional(readOnly = true)
    public ClinicDto findByUid(String uid) {
        return ClinicMapper.toDto(loadOrThrow(uid));
    }

    @Transactional(readOnly = true)
    public PageResponse<ClinicDto> search(String query, Boolean active, ClinicType type, Pageable pageable) {
        String trimmed = query == null ? null : query.trim();
        return PageResponse.from(
                clinicRepository.search(trimmed, active, type, pageable).map(ClinicMapper::toDto));
    }

    private Clinic loadOrThrow(String uid) {
        return clinicRepository.findByUid(uid)
                .orElseThrow(() -> new NotFoundException("Clinic not found: " + uid));
    }
}
