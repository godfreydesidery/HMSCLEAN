package com.otapp.hmis.engine.iam.api;

import com.otapp.hmis.engine.iam.application.RoleService;
import com.otapp.hmis.engine.iam.application.dto.CreateRoleRequest;
import com.otapp.hmis.engine.iam.application.dto.PrivilegeDto;
import com.otapp.hmis.engine.iam.application.dto.RoleDto;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Roles")
@RestController
@RequestMapping("/iam")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @PostMapping("/roles")
    @PreAuthorize("hasAuthority('ROLE_CREATE')")
    public ResponseEntity<RoleDto> create(@Valid @RequestBody CreateRoleRequest request) {
        return ResponseEntity.ok(roleService.create(request));
    }

    @GetMapping("/roles")
    @PreAuthorize("hasAuthority('ROLE_READ')")
    public ResponseEntity<List<RoleDto>> list() {
        return ResponseEntity.ok(roleService.list());
    }

    @GetMapping("/roles/uid/{roleUid}")
    @PreAuthorize("hasAuthority('ROLE_READ')")
    public ResponseEntity<RoleDto> findByUid(@PathVariable String roleUid) {
        return ResponseEntity.ok(roleService.findByUid(roleUid));
    }

    @PutMapping("/roles/uid/{roleUid}/privileges")
    @PreAuthorize("hasAuthority('ROLE_UPDATE')")
    public ResponseEntity<RoleDto> replacePrivileges(@PathVariable String roleUid, @RequestBody Set<String> privilegeNames) {
        return ResponseEntity.ok(roleService.replacePrivileges(roleUid, privilegeNames));
    }

    @GetMapping("/privileges")
    @PreAuthorize("hasAuthority('ROLE_READ')")
    public ResponseEntity<List<PrivilegeDto>> listPrivileges() {
        return ResponseEntity.ok(roleService.listPrivileges());
    }
}
