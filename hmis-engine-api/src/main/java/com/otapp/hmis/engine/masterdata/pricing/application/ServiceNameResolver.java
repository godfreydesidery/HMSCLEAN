package com.otapp.hmis.engine.masterdata.pricing.application;

import com.otapp.hmis.engine.masterdata.clinic.domain.Clinic;
import com.otapp.hmis.engine.masterdata.clinic.domain.ClinicRepository;
import com.otapp.hmis.engine.masterdata.labtest.domain.LabTestType;
import com.otapp.hmis.engine.masterdata.labtest.domain.LabTestTypeRepository;
import com.otapp.hmis.engine.masterdata.medicine.domain.Medicine;
import com.otapp.hmis.engine.masterdata.medicine.domain.MedicineRepository;
import com.otapp.hmis.engine.masterdata.pricing.domain.ServiceKind;
import com.otapp.hmis.engine.masterdata.procedure.domain.ProcedureType;
import com.otapp.hmis.engine.masterdata.procedure.domain.ProcedureTypeRepository;
import com.otapp.hmis.engine.masterdata.radiology.domain.RadiologyType;
import com.otapp.hmis.engine.masterdata.radiology.domain.RadiologyTypeRepository;
import com.otapp.hmis.engine.masterdata.ward.domain.Ward;
import com.otapp.hmis.engine.masterdata.ward.domain.WardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Resolves a (kind, uid) pair to a human-readable service name for inclusion
 * in pricing DTOs. Keeps the polymorphic price model from leaking into
 * downstream consumers.
 */
@Component
@RequiredArgsConstructor
class ServiceNameResolver {

    private final ClinicRepository clinicRepository;
    private final LabTestTypeRepository labTestTypeRepository;
    private final ProcedureTypeRepository procedureTypeRepository;
    private final RadiologyTypeRepository radiologyTypeRepository;
    private final MedicineRepository medicineRepository;
    private final WardRepository wardRepository;

    String resolveName(ServiceKind kind, String serviceUid) {
        return switch (kind) {
            case CONSULTATION -> clinicRepository.findByUid(serviceUid).map(Clinic::getName).orElse(null);
            case LAB_TEST     -> labTestTypeRepository.findByUid(serviceUid).map(LabTestType::getName).orElse(null);
            case PROCEDURE    -> procedureTypeRepository.findByUid(serviceUid).map(ProcedureType::getName).orElse(null);
            case RADIOLOGY    -> radiologyTypeRepository.findByUid(serviceUid).map(RadiologyType::getName).orElse(null);
            case MEDICINE     -> medicineRepository.findByUid(serviceUid).map(Medicine::getName).orElse(null);
            case WARD         -> wardRepository.findByUid(serviceUid).map(Ward::getName).orElse(null);
            // REGISTRATION uses the sentinel serviceUid "DEFAULT" — there's no
            // masterdata catalogue to resolve against, the name is the kind itself.
            case REGISTRATION -> "Patient registration fee";
        };
    }

    boolean serviceExists(ServiceKind kind, String serviceUid) {
        if (kind == ServiceKind.REGISTRATION) {
            return ServiceKind.REGISTRATION_SERVICE_UID.equals(serviceUid);
        }
        return resolveName(kind, serviceUid) != null;
    }
}
