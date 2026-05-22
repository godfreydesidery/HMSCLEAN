package com.otapp.hmis.engine.hr.asset.api;

import com.otapp.hmis.engine.common.api.PageResponse;
import com.otapp.hmis.engine.hr.asset.application.AssetDtos.AssetDto;
import com.otapp.hmis.engine.hr.asset.application.AssetDtos.CreateAssetRequest;
import com.otapp.hmis.engine.hr.asset.application.AssetDtos.RetireAssetRequest;
import com.otapp.hmis.engine.hr.asset.application.AssetDtos.UpdateAssetRequest;
import com.otapp.hmis.engine.hr.asset.application.AssetService;
import com.otapp.hmis.engine.hr.asset.domain.AssetStatus;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@Tag(name = "HR — asset register")
@RestController
@RequestMapping("/hr/assets")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('HR_ACCESS')")
public class AssetController {

    private final AssetService assetService;

    @PostMapping
    public ResponseEntity<AssetDto> create(@Valid @RequestBody CreateAssetRequest request) {
        AssetDto created = assetService.create(request);
        URI loc = UriComponentsBuilder.fromPath("/hr/assets/uid/{assetUid}")
                .buildAndExpand(created.uid()).toUri();
        return ResponseEntity.created(loc).body(created);
    }

    @GetMapping
    public ResponseEntity<PageResponse<AssetDto>> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) AssetStatus status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String location,
            Pageable pageable) {
        return ResponseEntity.ok(assetService.search(query, status, category, location, pageable));
    }

    @GetMapping("/uid/{assetUid}")
    public ResponseEntity<AssetDto> findByUid(@PathVariable String assetUid) {
        return ResponseEntity.ok(assetService.findByUid(assetUid));
    }

    @GetMapping("/by-tag/{tag}")
    public ResponseEntity<AssetDto> findByTag(@PathVariable String tag) {
        return ResponseEntity.ok(assetService.findByTag(tag));
    }

    @PutMapping("/uid/{assetUid}")
    public ResponseEntity<AssetDto> update(@PathVariable String assetUid,
                                           @Valid @RequestBody UpdateAssetRequest request) {
        return ResponseEntity.ok(assetService.update(assetUid, request));
    }

    @PostMapping("/uid/{assetUid}/retire")
    public ResponseEntity<AssetDto> retire(@PathVariable String assetUid,
                                           @Valid @RequestBody RetireAssetRequest request) {
        return ResponseEntity.ok(assetService.retire(assetUid, request));
    }

    @PostMapping("/uid/{assetUid}/reinstate")
    public ResponseEntity<AssetDto> reinstate(@PathVariable String assetUid) {
        return ResponseEntity.ok(assetService.reinstate(assetUid));
    }
}
