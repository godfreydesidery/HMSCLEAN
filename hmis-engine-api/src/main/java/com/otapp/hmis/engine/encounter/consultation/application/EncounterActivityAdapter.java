package com.otapp.hmis.engine.encounter.consultation.application;

import com.otapp.hmis.engine.common.spi.EncounterActivityPort;
import com.otapp.hmis.engine.encounter.admission.domain.AdmissionRepository;
import com.otapp.hmis.engine.encounter.admission.domain.AdmissionStatus;
import com.otapp.hmis.engine.encounter.consultation.domain.ConsultationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Encounter-side implementation of the {@link EncounterActivityPort} shared
 * kernel SPI. Reports whether a patient has an ongoing encounter so the patient
 * module can guard type / payment-type changes (legacy change_type /
 * change_payment_type) without importing the encounter module.
 */
@Component
@RequiredArgsConstructor
class EncounterActivityAdapter implements EncounterActivityPort {

    private final ConsultationRepository consultationRepository;
    private final AdmissionRepository admissionRepository;

    @Override
    @Transactional(readOnly = true)
    public boolean hasActiveEncounter(String patientUid) {
        if (admissionRepository.existsByPatientUidAndStatusIn(patientUid, AdmissionStatus.ACTIVE)) {
            return true;
        }
        return consultationRepository.countByPatientUidAndStatusIn(
                patientUid, ConsultationService.ACTIVE_CONSULTATION_STATES) > 0;
    }
}
