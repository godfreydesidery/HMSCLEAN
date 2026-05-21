package com.otapp.hmis.engine.masterdata.store.api;

import com.otapp.hmis.engine.masterdata.store.application.StoreStaffDtos.AssignStoreStaffRequest;
import com.otapp.hmis.engine.masterdata.store.application.StoreStaffDtos.StoreStaffDto;
import com.otapp.hmis.engine.masterdata.store.application.StoreStaffService;
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

@Tag(name = "Store keepers", description = "Master data: which keepers work at a store")
@RestController
@RequestMapping("/masterdata/stores/uid/{storeUid}/staff")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('MASTERDATA_MANAGE')")
public class StoreStaffController {

    private final StoreStaffService storeStaffService;

    @GetMapping
    public ResponseEntity<List<StoreStaffDto>> list(@PathVariable String storeUid) {
        return ResponseEntity.ok(storeStaffService.listForStore(storeUid));
    }

    @PostMapping
    public ResponseEntity<StoreStaffDto> assign(@PathVariable String storeUid,
                                                @Valid @RequestBody AssignStoreStaffRequest request) {
        StoreStaffDto assigned = storeStaffService.assign(storeUid, request.userUid());
        URI location = UriComponentsBuilder
                .fromPath("/masterdata/stores/uid/{storeUid}/staff/uid/{userUid}")
                .buildAndExpand(storeUid, assigned.userUid())
                .toUri();
        return ResponseEntity.created(location).body(assigned);
    }

    @DeleteMapping("/uid/{userUid}")
    public ResponseEntity<Void> remove(@PathVariable String storeUid, @PathVariable String userUid) {
        storeStaffService.remove(storeUid, userUid);
        return ResponseEntity.noContent().build();
    }
}
