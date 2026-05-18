package com.otapp.hmis.engine.masterdata.company.api;

import com.otapp.hmis.engine.masterdata.company.application.CompanyProfileDtos.CompanyProfileDto;
import com.otapp.hmis.engine.masterdata.company.application.CompanyProfileDtos.UpdateCompanyProfileRequest;
import com.otapp.hmis.engine.masterdata.company.application.CompanyProfileService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Company profile")
@RestController
@RequestMapping("/masterdata/company-profile")
@RequiredArgsConstructor
public class CompanyProfileController {

    private final CompanyProfileService service;

    /** Anyone authenticated may read the profile (used for header / footer / branding). */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CompanyProfileDto> current() {
        return ResponseEntity.ok(service.current());
    }

    @PutMapping
    @PreAuthorize("hasAuthority('MASTERDATA_MANAGE')")
    public ResponseEntity<CompanyProfileDto> update(@Valid @RequestBody UpdateCompanyProfileRequest request) {
        return ResponseEntity.ok(service.update(request));
    }
}
