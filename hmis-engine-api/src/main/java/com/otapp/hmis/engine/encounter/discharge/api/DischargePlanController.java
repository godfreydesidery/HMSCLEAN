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

@Tag(name = "Discharge plan")
@RestController
@RequestMapping("/encounters/admissions/uid/{admissionUid}/discharge-plan")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ENCOUNTER_ACCESS')")
public class DischargePlanController {

    private final DischargePlanService planService;

    @PostMapping
    public ResponseEntity<DischargePlanDto> create(@PathVariable String admissionUid,
                                                   @Valid @RequestBody CreatePlanRequest request) {
        DischargePlanDto created = planService.create(admissionUid, request);
        URI loc = UriComponentsBuilder.fromPath("/encounters/admissions/uid/{admissionUid}/discharge-plan")
                .buildAndExpand(admissionUid).toUri();
        return ResponseEntity.created(loc).body(created);
    }

    @GetMapping
    public ResponseEntity<DischargePlanDto> findByAdmission(@PathVariable String admissionUid) {
        return ResponseEntity.ok(planService.findByAdmission(admissionUid));
    }

    @PutMapping
    public ResponseEntity<DischargePlanDto> update(@PathVariable String admissionUid,
                                                   @Valid @RequestBody UpdatePlanRequest request) {
        return ResponseEntity.ok(planService.update(admissionUid, request));
    }

    @PostMapping("/approve")
    public ResponseEntity<DischargePlanDto> approve(@PathVariable String admissionUid) {
        return ResponseEntity.ok(planService.approve(admissionUid));
    }

    @PostMapping("/cancel")
    public ResponseEntity<DischargePlanDto> cancel(@PathVariable String admissionUid,
                                                   @Valid @RequestBody(required = false) CancelPlanRequest request) {
        return ResponseEntity.ok(planService.cancel(admissionUid, request));
    }
}
