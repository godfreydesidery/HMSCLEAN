package com.otapp.hmis.engine.encounter.medadmin.api;

import com.otapp.hmis.engine.encounter.medadmin.application.MedicationAdministrationDtos.MedicationAdministrationDto;
import com.otapp.hmis.engine.encounter.medadmin.application.MedicationAdministrationDtos.RecordAdministrationRequest;
import com.otapp.hmis.engine.encounter.medadmin.application.MedicationAdministrationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Medication administration (MAR)")
@RestController
@RequestMapping("/encounters/admissions/uid/{admissionUid}/medication-administrations")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ENCOUNTER_ACCESS')")
public class MedicationAdministrationController {

    private final MedicationAdministrationService service;

    @GetMapping
    public ResponseEntity<List<MedicationAdministrationDto>> list(@PathVariable String admissionUid) {
        return ResponseEntity.ok(service.listForAdmission(admissionUid));
    }

    @PostMapping
    public ResponseEntity<MedicationAdministrationDto> record(@PathVariable String admissionUid,
                                                             @Valid @RequestBody RecordAdministrationRequest request) {
        return ResponseEntity.ok(service.record(admissionUid, request));
    }
}
