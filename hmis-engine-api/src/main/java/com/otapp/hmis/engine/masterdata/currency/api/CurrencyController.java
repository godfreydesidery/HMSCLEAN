package com.otapp.hmis.engine.masterdata.currency.api;

import com.otapp.hmis.engine.common.api.PageResponse;
import com.otapp.hmis.engine.masterdata.currency.application.CurrencyDtos.CreateCurrencyRequest;
import com.otapp.hmis.engine.masterdata.currency.application.CurrencyDtos.CurrencyDto;
import com.otapp.hmis.engine.masterdata.currency.application.CurrencyDtos.UpdateCurrencyRequest;
import com.otapp.hmis.engine.masterdata.currency.application.CurrencyService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

@Tag(name = "Currencies")
@RestController
@RequestMapping("/masterdata/currencies")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('MASTERDATA_MANAGE')")
public class CurrencyController {

    private final CurrencyService currencyService;

    @PostMapping
    public ResponseEntity<CurrencyDto> create(@Valid @RequestBody CreateCurrencyRequest request) {
        CurrencyDto created = currencyService.create(request);
        URI loc = UriComponentsBuilder.fromPath("/masterdata/currencies/uid/{uid}").buildAndExpand(created.uid()).toUri();
        return ResponseEntity.created(loc).body(created);
    }

    @GetMapping
    public ResponseEntity<PageResponse<CurrencyDto>> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Boolean active,
            Pageable pageable) {
        return ResponseEntity.ok(currencyService.search(query, active, pageable));
    }

    @GetMapping("/default")
    public ResponseEntity<CurrencyDto> findDefault() {
        CurrencyDto dto = currencyService.findDefault();
        return dto == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(dto);
    }

    @GetMapping("/uid/{currencyUid}")
    public ResponseEntity<CurrencyDto> findByUid(@PathVariable String currencyUid) {
        return ResponseEntity.ok(currencyService.findByUid(currencyUid));
    }

    @PutMapping("/uid/{currencyUid}")
    public ResponseEntity<CurrencyDto> update(@PathVariable String currencyUid,
                                              @Valid @RequestBody UpdateCurrencyRequest request) {
        return ResponseEntity.ok(currencyService.update(currencyUid, request));
    }

    @PutMapping("/uid/{currencyUid}/active")
    public ResponseEntity<CurrencyDto> setActive(@PathVariable String currencyUid,
                                                 @RequestBody ActiveRequest request) {
        return ResponseEntity.ok(currencyService.setActive(currencyUid, request.active()));
    }

    @PostMapping("/uid/{currencyUid}/default")
    public ResponseEntity<CurrencyDto> setDefault(@PathVariable String currencyUid) {
        return ResponseEntity.ok(currencyService.setDefault(currencyUid));
    }

    @DeleteMapping("/uid/{currencyUid}")
    public ResponseEntity<Void> delete(@PathVariable String currencyUid) {
        currencyService.delete(currencyUid);
        return ResponseEntity.noContent().build();
    }

    public record ActiveRequest(boolean active) {}
}
