package com.otapp.hmis.engine.iam.api;

import com.otapp.hmis.engine.iam.application.ProviderProfileService;
import com.otapp.hmis.engine.iam.application.dto.ProviderProfileDtos.ProviderProfileDto;
import com.otapp.hmis.engine.iam.application.dto.ProviderProfileDtos.UpsertProviderProfileRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Provider profiles", description = "Clinician specialty / registration / licence")
@RestController
@RequestMapping("/iam/users/uid/{userUid}/provider-profile")
@RequiredArgsConstructor
public class ProviderProfileController {

    private final ProviderProfileService providerProfileService;

    @GetMapping
    @PreAuthorize("hasAuthority('USER_READ')")
    public ResponseEntity<ProviderProfileDto> get(@PathVariable String userUid) {
        return ResponseEntity.ok(providerProfileService.findByUserUid(userUid));
    }

    @PutMapping
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    public ResponseEntity<ProviderProfileDto> upsert(@PathVariable String userUid,
                                                     @Valid @RequestBody UpsertProviderProfileRequest request) {
        return ResponseEntity.ok(providerProfileService.upsert(userUid, request));
    }
}
