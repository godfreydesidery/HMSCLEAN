package com.otapp.hmis.engine.encounter.prescription.api;

import com.otapp.hmis.engine.common.api.PageResponse;
import com.otapp.hmis.engine.encounter.prescription.application.PrescribingAlertDtos.PrescribingAlertsDto;
import com.otapp.hmis.engine.encounter.prescription.application.PrescribingAlertService;
import com.otapp.hmis.engine.encounter.prescription.application.PrescriptionDtos.CancelPrescriptionRequest;
import com.otapp.hmis.engine.encounter.prescription.application.PrescriptionDtos.CreatePrescriptionRequest;
import com.otapp.hmis.engine.encounter.prescription.application.PrescriptionDtos.PrescriptionDto;
import com.otapp.hmis.engine.encounter.prescription.application.PrescriptionDtos.PrescriptionWorklistRow;
import com.otapp.hmis.engine.encounter.prescription.application.PrescriptionService;
import com.otapp.hmis.engine.encounter.prescription.domain.PrescriptionStatus;
import com.otapp.hmis.engine.patient.domain.PatientClassScope;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
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
    private final PrescribingAlertService prescribingAlertService;

    @GetMapping("/encounters/consultations/uid/{consultationUid}/prescriptions")
    public ResponseEntity<List<PrescriptionDto>> list(@PathVariable String consultationUid) {
        return ResponseEntity.ok(prescriptionService.listForConsultation(consultationUid));
    }

    /**
     * Non-blocking prescribing advisories (same-medicine-this-month +
     * unfinished-course) for a patient/medicine pair. 200 with a possibly-empty
     * list; 404 only if the patient or medicine uid is unknown. Never 4xx/5xx
     * for missing dispense history.
     */
    @GetMapping("/encounters/patients/uid/{patientUid}/medicines/uid/{medicineUid}/prescribing-alerts")
    public ResponseEntity<PrescribingAlertsDto> prescribingAlerts(@PathVariable String patientUid,
                                                                  @PathVariable String medicineUid) {
        return ResponseEntity.ok(prescribingAlertService.alertsFor(patientUid, medicineUid));
    }

    /** Pharmacy dispensing queue — prescriptions awaiting pharmacy action, scoped by patient class. */
    @GetMapping("/encounters/prescriptions/worklist")
    @PreAuthorize("hasAuthority('PHARMACY_ACCESS')")
    public ResponseEntity<PageResponse<PrescriptionWorklistRow>> dispenseWorklist(
            @RequestParam(required = false) PrescriptionStatus status,
            @RequestParam(required = false) PatientClassScope patientClass,
            @RequestParam(defaultValue = "true") boolean hideUnpaid,
            Pageable pageable) {
        return ResponseEntity.ok(prescriptionService.searchDispenseWorklist(status, patientClass, hideUnpaid, pageable));
    }

    @PostMapping("/encounters/consultations/uid/{consultationUid}/prescriptions")
    public ResponseEntity<PrescriptionDto> prescribe(@PathVariable String consultationUid,
                                                     @Valid @RequestBody CreatePrescriptionRequest request) {
        return ResponseEntity.ok(prescriptionService.prescribe(consultationUid, request));
    }

    @GetMapping("/encounters/patients/uid/{patientUid}/outsider-prescriptions")
    public ResponseEntity<List<PrescriptionDto>> listOutsiderForPatient(@PathVariable String patientUid) {
        return ResponseEntity.ok(prescriptionService.listOutsiderForPatient(patientUid));
    }

    @PostMapping("/encounters/patients/uid/{patientUid}/outsider-prescriptions")
    public ResponseEntity<PrescriptionDto> prescribeForOutsider(@PathVariable String patientUid,
                                                                @Valid @RequestBody CreatePrescriptionRequest request) {
        return ResponseEntity.ok(prescriptionService.prescribeForOutsider(patientUid, request));
    }

    @PostMapping("/encounters/prescriptions/uid/{prescriptionUid}/accept")
    @PreAuthorize("hasAuthority('PHARMACY_ACCESS')")
    public ResponseEntity<PrescriptionDto> accept(@PathVariable String prescriptionUid) {
        return ResponseEntity.ok(prescriptionService.accept(prescriptionUid));
    }

    @PostMapping("/encounters/prescriptions/uid/{prescriptionUid}/hold")
    @PreAuthorize("hasAuthority('PHARMACY_ACCESS')")
    public ResponseEntity<PrescriptionDto> hold(@PathVariable String prescriptionUid) {
        return ResponseEntity.ok(prescriptionService.hold(prescriptionUid));
    }

    @PostMapping("/encounters/prescriptions/uid/{prescriptionUid}/verify")
    @PreAuthorize("hasAuthority('PHARMACY_ACCESS')")
    public ResponseEntity<PrescriptionDto> verify(@PathVariable String prescriptionUid) {
        return ResponseEntity.ok(prescriptionService.verify(prescriptionUid));
    }

    @PostMapping("/encounters/prescriptions/uid/{prescriptionUid}/approve")
    @PreAuthorize("hasAuthority('PHARMACY_ACCESS')")
    public ResponseEntity<PrescriptionDto> approve(@PathVariable String prescriptionUid) {
        return ResponseEntity.ok(prescriptionService.approve(prescriptionUid));
    }

    @PostMapping("/encounters/prescriptions/uid/{prescriptionUid}/reject")
    @PreAuthorize("hasAuthority('PHARMACY_ACCESS')")
    public ResponseEntity<PrescriptionDto> reject(@PathVariable String prescriptionUid,
                                                  @Valid @RequestBody(required = false) CancelPrescriptionRequest request) {
        return ResponseEntity.ok(prescriptionService.reject(prescriptionUid, request));
    }

    @PostMapping("/encounters/prescriptions/uid/{prescriptionUid}/cancel")
    public ResponseEntity<PrescriptionDto> cancel(@PathVariable String prescriptionUid,
                                                  @Valid @RequestBody(required = false) CancelPrescriptionRequest request) {
        return ResponseEntity.ok(prescriptionService.cancel(prescriptionUid, request));
    }
}
