package com.otapp.hmis.engine.encounter.nursingchart.api;

import com.otapp.hmis.engine.encounter.nursingchart.application.NursingChartDtos.CancelCarePlanItemRequest;
import com.otapp.hmis.engine.encounter.nursingchart.application.NursingChartDtos.CareActivityEntryDto;
import com.otapp.hmis.engine.encounter.nursingchart.application.NursingChartDtos.CarePlanItemDto;
import com.otapp.hmis.engine.encounter.nursingchart.application.NursingChartDtos.CreateCareActivityEntryRequest;
import com.otapp.hmis.engine.encounter.nursingchart.application.NursingChartDtos.CreateCarePlanItemRequest;
import com.otapp.hmis.engine.encounter.nursingchart.application.NursingChartDtos.CreateDressingEntryRequest;
import com.otapp.hmis.engine.encounter.nursingchart.application.NursingChartDtos.CreateFluidBalanceEntryRequest;
import com.otapp.hmis.engine.encounter.nursingchart.application.NursingChartDtos.CreateVitalsEntryRequest;
import com.otapp.hmis.engine.encounter.nursingchart.application.NursingChartDtos.DressingEntryDto;
import com.otapp.hmis.engine.encounter.nursingchart.application.NursingChartDtos.FluidBalanceEntryDto;
import com.otapp.hmis.engine.encounter.nursingchart.application.NursingChartDtos.ResolveCarePlanItemRequest;
import com.otapp.hmis.engine.encounter.nursingchart.application.NursingChartDtos.UpdateCarePlanItemRequest;
import com.otapp.hmis.engine.encounter.nursingchart.application.NursingChartDtos.VitalsEntryDto;
import com.otapp.hmis.engine.encounter.nursingchart.application.NursingChartService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Nursing chart")
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ENCOUNTER_ACCESS')")
public class NursingChartController {

    private final NursingChartService service;

    // ----- vitals ----------------------------------------------------------

    @GetMapping("/encounters/admissions/uid/{admissionUid}/vitals")
    public ResponseEntity<List<VitalsEntryDto>> listVitals(@PathVariable String admissionUid) {
        return ResponseEntity.ok(service.listVitals(admissionUid));
    }

    @PostMapping("/encounters/admissions/uid/{admissionUid}/vitals")
    public ResponseEntity<VitalsEntryDto> recordVitals(@PathVariable String admissionUid,
                                                       @Valid @RequestBody CreateVitalsEntryRequest request) {
        return ResponseEntity.ok(service.recordVitals(admissionUid, request));
    }

    // ----- nursing care plan -----------------------------------------------

    @GetMapping("/encounters/admissions/uid/{admissionUid}/care-plan")
    public ResponseEntity<List<CarePlanItemDto>> listCarePlan(@PathVariable String admissionUid) {
        return ResponseEntity.ok(service.listCarePlan(admissionUid));
    }

    @PostMapping("/encounters/admissions/uid/{admissionUid}/care-plan")
    public ResponseEntity<CarePlanItemDto> addCarePlanItem(@PathVariable String admissionUid,
                                                           @Valid @RequestBody CreateCarePlanItemRequest request) {
        return ResponseEntity.ok(service.addCarePlanItem(admissionUid, request));
    }

    @PutMapping("/encounters/care-plan-items/uid/{itemUid}")
    public ResponseEntity<CarePlanItemDto> updateCarePlanItem(@PathVariable String itemUid,
                                                              @Valid @RequestBody UpdateCarePlanItemRequest request) {
        return ResponseEntity.ok(service.updateCarePlanItem(itemUid, request));
    }

    @PostMapping("/encounters/care-plan-items/uid/{itemUid}/resolve")
    public ResponseEntity<CarePlanItemDto> resolveCarePlanItem(@PathVariable String itemUid,
                                                               @Valid @RequestBody(required = false) ResolveCarePlanItemRequest request) {
        return ResponseEntity.ok(service.resolveCarePlanItem(itemUid, request));
    }

    @PostMapping("/encounters/care-plan-items/uid/{itemUid}/cancel")
    public ResponseEntity<CarePlanItemDto> cancelCarePlanItem(@PathVariable String itemUid,
                                                              @Valid @RequestBody(required = false) CancelCarePlanItemRequest request) {
        return ResponseEntity.ok(service.cancelCarePlanItem(itemUid, request));
    }

    // ----- dressing chart --------------------------------------------------

    @GetMapping("/encounters/admissions/uid/{admissionUid}/dressings")
    public ResponseEntity<List<DressingEntryDto>> listDressings(@PathVariable String admissionUid) {
        return ResponseEntity.ok(service.listDressings(admissionUid));
    }

    @PostMapping("/encounters/admissions/uid/{admissionUid}/dressings")
    public ResponseEntity<DressingEntryDto> recordDressing(@PathVariable String admissionUid,
                                                           @Valid @RequestBody CreateDressingEntryRequest request) {
        return ResponseEntity.ok(service.recordDressing(admissionUid, request));
    }

    // ----- fluid-balance chart ---------------------------------------------

    @GetMapping("/encounters/admissions/uid/{admissionUid}/fluid-balance")
    public ResponseEntity<List<FluidBalanceEntryDto>> listFluidBalance(@PathVariable String admissionUid) {
        return ResponseEntity.ok(service.listFluidBalance(admissionUid));
    }

    @PostMapping("/encounters/admissions/uid/{admissionUid}/fluid-balance")
    public ResponseEntity<FluidBalanceEntryDto> recordFluidBalance(@PathVariable String admissionUid,
                                                                   @Valid @RequestBody CreateFluidBalanceEntryRequest request) {
        return ResponseEntity.ok(service.recordFluidBalance(admissionUid, request));
    }

    // ----- care-activity chart ---------------------------------------------

    @GetMapping("/encounters/admissions/uid/{admissionUid}/care-activities")
    public ResponseEntity<List<CareActivityEntryDto>> listCareActivity(@PathVariable String admissionUid) {
        return ResponseEntity.ok(service.listCareActivity(admissionUid));
    }

    @PostMapping("/encounters/admissions/uid/{admissionUid}/care-activities")
    public ResponseEntity<CareActivityEntryDto> recordCareActivity(@PathVariable String admissionUid,
                                                                   @Valid @RequestBody CreateCareActivityEntryRequest request) {
        return ResponseEntity.ok(service.recordCareActivity(admissionUid, request));
    }
}
