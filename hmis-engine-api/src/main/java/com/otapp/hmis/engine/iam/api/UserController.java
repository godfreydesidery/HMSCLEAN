package com.otapp.hmis.engine.iam.api;

import com.otapp.hmis.engine.common.api.PageResponse;
import com.otapp.hmis.engine.iam.application.UserService;
import com.otapp.hmis.engine.iam.application.dto.CreateUserRequest;
import com.otapp.hmis.engine.iam.application.dto.UserSummary;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.security.Principal;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Users")
@RestController
@RequestMapping("/iam/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    @PreAuthorize("hasAuthority('USER_CREATE')")
    public ResponseEntity<UserSummary> create(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.ok(userService.create(request));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('USER_READ')")
    public ResponseEntity<PageResponse<UserSummary>> search(
            @org.springframework.web.bind.annotation.RequestParam(required = false) String query,
            @org.springframework.web.bind.annotation.RequestParam(required = false) Boolean enabled,
            Pageable pageable) {
        return ResponseEntity.ok(userService.search(query, enabled, pageable));
    }

    @GetMapping("/uid/{userUid}")
    @PreAuthorize("hasAuthority('USER_READ')")
    public ResponseEntity<UserSummary> findByUid(@PathVariable String userUid) {
        return ResponseEntity.ok(userService.findByUid(userUid));
    }

    @PutMapping("/uid/{userUid}/enabled")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    public ResponseEntity<UserSummary> setEnabled(@PathVariable String userUid,
                                                  @RequestBody EnabledRequest request,
                                                  Principal principal) {
        String currentUsername = principal == null ? null : principal.getName();
        return ResponseEntity.ok(userService.setEnabled(userUid, request.enabled(), currentUsername));
    }

    @PutMapping("/uid/{userUid}/roles")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    public ResponseEntity<UserSummary> replaceRoles(@PathVariable String userUid, @RequestBody Set<String> roleNames) {
        return ResponseEntity.ok(userService.replaceRoles(userUid, roleNames));
    }

    @PostMapping("/uid/{userUid}/reset-password")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    public ResponseEntity<UserSummary> resetPassword(@PathVariable String userUid,
                                                     @Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(userService.resetPassword(userUid, request.newPassword()));
    }

    @PostMapping("/uid/{userUid}/unlock")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    public ResponseEntity<UserSummary> unlock(@PathVariable String userUid) {
        return ResponseEntity.ok(userService.unlock(userUid));
    }

    public record EnabledRequest(boolean enabled) {}

    public record ResetPasswordRequest(@NotBlank @Size(min = 8, max = 128) String newPassword) {}
}
