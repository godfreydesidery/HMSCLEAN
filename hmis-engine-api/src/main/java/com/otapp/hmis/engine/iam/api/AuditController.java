package com.otapp.hmis.engine.iam.api;

import com.otapp.hmis.engine.common.api.PageResponse;
import com.otapp.hmis.engine.iam.application.AuditService;
import com.otapp.hmis.engine.iam.application.dto.LoginAttemptDto;
import com.otapp.hmis.engine.iam.domain.LoginAttempt;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "IAM audit")
@RestController
@RequestMapping("/iam/audit")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('USER_READ')")
public class AuditController {

    private final AuditService auditService;

    @GetMapping("/login-attempts")
    public ResponseEntity<PageResponse<LoginAttemptDto>> loginAttempts(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) LoginAttempt.Outcome outcome,
            Pageable pageable) {
        return ResponseEntity.ok(auditService.searchLoginAttempts(username, outcome, pageable));
    }
}
