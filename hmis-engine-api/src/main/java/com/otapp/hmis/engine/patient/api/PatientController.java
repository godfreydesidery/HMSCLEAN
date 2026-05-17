package com.otapp.hmis.engine.patient.api;

import com.otapp.hmis.engine.common.api.PageResponse;
import com.otapp.hmis.engine.patient.application.PatientService;
import com.otapp.hmis.engine.patient.application.dto.CreatePatientRequest;
import com.otapp.hmis.engine.patient.application.dto.PatientDto;
import com.otapp.hmis.engine.patient.application.dto.PatientSummary;
import com.otapp.hmis.engine.patient.application.dto.UpdatePatientRequest;
import com.otapp.hmis.engine.patient.domain.Gender;
import com.otapp.hmis.engine.patient.domain.PaymentType;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

@Tag(name = "Patients")
@RestController
@RequestMapping("/patients")
@RequiredArgsConstructor
public class PatientController {

    private final PatientService patientService;

    @PostMapping
    @PreAuthorize("hasAuthority('PATIENT_ACCESS')")
    public ResponseEntity<PatientDto> register(@Valid @RequestBody CreatePatientRequest request) {
        PatientDto created = patientService.register(request);
        URI loc = UriComponentsBuilder.fromPath("/patients/{uid}").buildAndExpand(created.uid()).toUri();
        return ResponseEntity.created(loc).body(created);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PATIENT_ACCESS')")
    public ResponseEntity<PageResponse<PatientSummary>> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Gender gender,
            @RequestParam(required = false) PaymentType paymentType,
            Pageable pageable) {
        return ResponseEntity.ok(patientService.search(query, active, gender, paymentType, pageable));
    }

    @GetMapping("/{uid}")
    @PreAuthorize("hasAuthority('PATIENT_ACCESS')")
    public ResponseEntity<PatientDto> findByUid(@PathVariable String uid) {
        return ResponseEntity.ok(patientService.findByUid(uid));
    }

    @PutMapping("/{uid}")
    @PreAuthorize("hasAuthority('PATIENT_ACCESS')")
    public ResponseEntity<PatientDto> update(@PathVariable String uid, @Valid @RequestBody UpdatePatientRequest request) {
        return ResponseEntity.ok(patientService.update(uid, request));
    }

    @PutMapping("/{uid}/active")
    @PreAuthorize("hasAuthority('PATIENT_ACCESS')")
    public ResponseEntity<PatientDto> setActive(@PathVariable String uid, @RequestBody ActiveRequest request) {
        return ResponseEntity.ok(patientService.setActive(uid, request.active()));
    }

    public record ActiveRequest(boolean active) {}
}
