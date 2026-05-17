package com.otapp.hmis.engine.encounter.prescription.api;

import com.otapp.hmis.engine.encounter.prescription.application.PrescriptionDtos.CancelPrescriptionRequest;
import com.otapp.hmis.engine.encounter.prescription.application.PrescriptionDtos.CreatePrescriptionRequest;
import com.otapp.hmis.engine.encounter.prescription.application.PrescriptionDtos.PrescriptionDto;
import com.otapp.hmis.engine.encounter.prescription.application.PrescriptionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Prescriptions")
@RestController
@RequestMapping
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ENCOUNTER_ACCESS')")
public class PrescriptionController {

    private final PrescriptionService prescriptionService;

    @GetMapping("/encounters/consultations/{consultationUid}/prescriptions")
    public ResponseEntity<List<PrescriptionDto>> list(@PathVariable String consultationUid) {
        return ResponseEntity.ok(prescriptionService.listForConsultation(consultationUid));
    }

    @PostMapping("/encounters/consultations/{consultationUid}/prescriptions")
    public ResponseEntity<PrescriptionDto> prescribe(@PathVariable String consultationUid,
                                                     @Valid @RequestBody CreatePrescriptionRequest request) {
        return ResponseEntity.ok(prescriptionService.prescribe(consultationUid, request));
    }

    @PostMapping("/encounters/prescriptions/{uid}/dispense")
    @PreAuthorize("hasAuthority('PHARMACY_ACCESS')")
    public ResponseEntity<PrescriptionDto> dispense(@PathVariable String uid) {
        return ResponseEntity.ok(prescriptionService.dispense(uid));
    }

    @PostMapping("/encounters/prescriptions/{uid}/cancel")
    public ResponseEntity<PrescriptionDto> cancel(@PathVariable String uid,
                                                  @Valid @RequestBody(required = false) CancelPrescriptionRequest request) {
        return ResponseEntity.ok(prescriptionService.cancel(uid, request));
    }
}
