package com.otapp.hmis.engine.encounter.vitals.api;

import com.otapp.hmis.engine.common.api.PageResponse;
import com.otapp.hmis.engine.encounter.vitals.application.PatientVitalsDtos.VitalsWorklistRow;
import com.otapp.hmis.engine.encounter.vitals.application.PatientVitalsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The OUTPATIENT nurse-triage worklist (OPC-3): fee-settled consultations still
 * BOOKED / IN_PROGRESS that may need vitals captured, enriched with the patient
 * identity and the current vitals status. Distinct from the inpatient
 * (admission-scoped) nurse worklist at {@code /encounters/admissions/nurse-worklist}.
 */
@Tag(name = "Patient vitals")
@RestController
@RequestMapping("/encounters/consultations/nurse-worklist")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ENCOUNTER_ACCESS')")
public class OutpatientNurseWorklistController {

    private final PatientVitalsService vitalsService;

    @Operation(summary = "Outpatient nurse-triage worklist (fee-settled BOOKED/IN_PROGRESS consultations)")
    @GetMapping
    public ResponseEntity<PageResponse<VitalsWorklistRow>> worklist(Pageable pageable) {
        return ResponseEntity.ok(vitalsService.nurseWorklist(pageable));
    }
}
