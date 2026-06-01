package com.otapp.hmis.engine.encounter.discharge.api;

import com.otapp.hmis.engine.encounter.discharge.application.DischargePlanDtos.CancelPlanRequest;
import com.otapp.hmis.engine.encounter.discharge.application.DischargePlanDtos.CreatePlanRequest;
import com.otapp.hmis.engine.encounter.discharge.application.DischargePlanDtos.DischargePlanDto;
import com.otapp.hmis.engine.encounter.discharge.application.DischargePlanDtos.UpdatePlanRequest;
import com.otapp.hmis.engine.encounter.discharge.application.DischargePlanService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Outpatient closure of a consultation — death (DECEASED) or external referral
 * (REFERRAL). Mirrors {@link DischargePlanController} but keyed off a
 * consultation. Authored by the clinician, approved by a second user; approval
 * closes the consultation (and, for DECEASED, flags the patient).
 */
@Tag(name = "Consultation closure")
@RestController
@RequestMapping("/encounters/consultations/uid/{consultationUid}/closure")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ENCOUNTER_ACCESS')")
public class ConsultationClosureController {

    private final DischargePlanService planService;

    @PostMapping
    public ResponseEntity<DischargePlanDto> create(@PathVariable String consultationUid,
                                                   @Valid @RequestBody CreatePlanRequest request) {
        DischargePlanDto created = planService.createForConsultation(consultationUid, request);
        URI loc = UriComponentsBuilder.fromPath("/encounters/consultations/uid/{consultationUid}/closure")
                .buildAndExpand(consultationUid).toUri();
        return ResponseEntity.created(loc).body(created);
    }

    @GetMapping
    public ResponseEntity<DischargePlanDto> findByConsultation(@PathVariable String consultationUid) {
        return ResponseEntity.ok(planService.findByConsultation(consultationUid));
    }

    @PutMapping
    public ResponseEntity<DischargePlanDto> update(@PathVariable String consultationUid,
                                                   @Valid @RequestBody UpdatePlanRequest request) {
        return ResponseEntity.ok(planService.updateForConsultation(consultationUid, request));
    }

    @PostMapping("/approve")
    public ResponseEntity<DischargePlanDto> approve(@PathVariable String consultationUid) {
        return ResponseEntity.ok(planService.approveForConsultation(consultationUid));
    }

    @PostMapping("/cancel")
    public ResponseEntity<DischargePlanDto> cancel(@PathVariable String consultationUid,
                                                   @Valid @RequestBody(required = false) CancelPlanRequest request) {
        return ResponseEntity.ok(planService.cancelForConsultation(consultationUid, request));
    }
}
