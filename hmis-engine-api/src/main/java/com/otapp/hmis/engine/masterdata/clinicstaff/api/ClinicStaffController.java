package com.otapp.hmis.engine.masterdata.clinicstaff.api;

import com.otapp.hmis.engine.masterdata.clinicstaff.application.ClinicStaffService;
import com.otapp.hmis.engine.masterdata.clinicstaff.application.dto.AssignClinicianRequest;
import com.otapp.hmis.engine.masterdata.clinicstaff.application.dto.ClinicClinicianDto;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@Tag(name = "Clinic clinicians", description = "Master data: which clinicians work at a clinic")
@RestController
@RequestMapping("/masterdata/clinics/uid/{clinicUid}/clinicians")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('MASTERDATA_MANAGE')")
public class ClinicStaffController {

    private final ClinicStaffService clinicStaffService;

    @GetMapping
    public ResponseEntity<List<ClinicClinicianDto>> list(@PathVariable String clinicUid) {
        return ResponseEntity.ok(clinicStaffService.listClinicians(clinicUid));
    }

    @PostMapping
    public ResponseEntity<ClinicClinicianDto> assign(@PathVariable String clinicUid,
                                                     @Valid @RequestBody AssignClinicianRequest request) {
        ClinicClinicianDto assigned = clinicStaffService.assignClinician(clinicUid, request.userUid());
        URI location = UriComponentsBuilder
                .fromPath("/masterdata/clinics/uid/{clinicUid}/clinicians/uid/{userUid}")
                .buildAndExpand(clinicUid, assigned.userUid())
                .toUri();
        return ResponseEntity.created(location).body(assigned);
    }

    @DeleteMapping("/uid/{userUid}")
    public ResponseEntity<Void> remove(@PathVariable String clinicUid, @PathVariable String userUid) {
        clinicStaffService.removeClinician(clinicUid, userUid);
        return ResponseEntity.noContent().build();
    }
}
