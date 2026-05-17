package com.otapp.hmis.engine.masterdata.clinic.api;

import com.otapp.hmis.engine.common.api.PageResponse;
import com.otapp.hmis.engine.masterdata.clinic.application.ClinicService;
import com.otapp.hmis.engine.masterdata.clinic.application.dto.ClinicDto;
import com.otapp.hmis.engine.masterdata.clinic.application.dto.CreateClinicRequest;
import com.otapp.hmis.engine.masterdata.clinic.application.dto.UpdateClinicRequest;
import com.otapp.hmis.engine.masterdata.clinic.domain.ClinicType;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@Tag(name = "Clinics", description = "Master data: clinics / specialty units")
@RestController
@RequestMapping("/masterdata/clinics")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('MASTERDATA_MANAGE')")
public class ClinicController {

    private final ClinicService clinicService;

    @PostMapping
    public ResponseEntity<ClinicDto> create(@Valid @RequestBody CreateClinicRequest request) {
        ClinicDto created = clinicService.create(request);
        URI location = UriComponentsBuilder.fromPath("/masterdata/clinics/{uid}")
                .buildAndExpand(created.uid())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    public ResponseEntity<PageResponse<ClinicDto>> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) ClinicType type,
            Pageable pageable) {
        return ResponseEntity.ok(clinicService.search(query, active, type, pageable));
    }

    @GetMapping("/{uid}")
    public ResponseEntity<ClinicDto> findByUid(@PathVariable String uid) {
        return ResponseEntity.ok(clinicService.findByUid(uid));
    }

    @PutMapping("/{uid}")
    public ResponseEntity<ClinicDto> update(@PathVariable String uid, @Valid @RequestBody UpdateClinicRequest request) {
        return ResponseEntity.ok(clinicService.update(uid, request));
    }

    @PutMapping("/{uid}/active")
    public ResponseEntity<ClinicDto> setActive(@PathVariable String uid, @RequestBody ActiveRequest request) {
        return ResponseEntity.ok(clinicService.setActive(uid, request.active()));
    }

    @DeleteMapping("/{uid}")
    public ResponseEntity<Void> delete(@PathVariable String uid) {
        clinicService.delete(uid);
        return ResponseEntity.noContent().build();
    }

    public record ActiveRequest(boolean active) {}
}
