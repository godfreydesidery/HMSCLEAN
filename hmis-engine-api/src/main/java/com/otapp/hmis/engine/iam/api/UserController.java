package com.otapp.hmis.engine.iam.api;

import com.otapp.hmis.engine.common.api.PageResponse;
import com.otapp.hmis.engine.iam.application.UserService;
import com.otapp.hmis.engine.iam.application.dto.CreateUserRequest;
import com.otapp.hmis.engine.iam.application.dto.UserSummary;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
    public ResponseEntity<PageResponse<UserSummary>> list(Pageable pageable) {
        return ResponseEntity.ok(userService.list(pageable));
    }

    @GetMapping("/{uid}")
    @PreAuthorize("hasAuthority('USER_READ')")
    public ResponseEntity<UserSummary> findByUid(@PathVariable String uid) {
        return ResponseEntity.ok(userService.findByUid(uid));
    }

    @PutMapping("/{uid}/enabled")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    public ResponseEntity<UserSummary> setEnabled(@PathVariable String uid, @RequestBody EnabledRequest request) {
        return ResponseEntity.ok(userService.setEnabled(uid, request.enabled()));
    }

    @PutMapping("/{uid}/roles")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    public ResponseEntity<UserSummary> replaceRoles(@PathVariable String uid, @RequestBody Set<String> roleNames) {
        return ResponseEntity.ok(userService.replaceRoles(uid, roleNames));
    }

    @PostMapping("/{uid}/reset-password")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    public ResponseEntity<UserSummary> resetPassword(@PathVariable String uid,
                                                     @Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(userService.resetPassword(uid, request.newPassword()));
    }

    @PostMapping("/{uid}/unlock")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    public ResponseEntity<UserSummary> unlock(@PathVariable String uid) {
        return ResponseEntity.ok(userService.unlock(uid));
    }

    public record EnabledRequest(boolean enabled) {}

    public record ResetPasswordRequest(@NotBlank @Size(min = 8, max = 128) String newPassword) {}
}
