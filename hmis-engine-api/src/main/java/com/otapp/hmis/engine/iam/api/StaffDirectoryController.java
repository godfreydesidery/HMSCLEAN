package com.otapp.hmis.engine.iam.api;

import com.otapp.hmis.engine.iam.application.StaffDirectoryService;
import com.otapp.hmis.engine.iam.application.dto.StaffOption;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only directory of staff by role, accessible to any authenticated user
 * so the encounter / orders / pharmacy modules can populate staff pickers.
 */
@Tag(name = "Staff directory")
@RestController
@RequestMapping("/iam/staff")
@RequiredArgsConstructor
public class StaffDirectoryController {

    private final StaffDirectoryService staffDirectoryService;

    @GetMapping("/by-role/{roleName}")
    public ResponseEntity<List<StaffOption>> byRole(@PathVariable String roleName) {
        return ResponseEntity.ok(staffDirectoryService.findByRole(roleName.toUpperCase()));
    }
}
