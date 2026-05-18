package com.otapp.hmis.engine.masterdata.company.application;

import com.otapp.hmis.engine.masterdata.company.application.CompanyProfileDtos.CompanyProfileDto;
import com.otapp.hmis.engine.masterdata.company.application.CompanyProfileDtos.UpdateCompanyProfileRequest;
import com.otapp.hmis.engine.masterdata.company.domain.CompanyProfile;
import com.otapp.hmis.engine.masterdata.company.domain.CompanyProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CompanyProfileService {

    private final CompanyProfileRepository repo;

    @Transactional(readOnly = true)
    public CompanyProfileDto current() {
        return repo.findAll().stream()
                .findFirst()
                .map(CompanyProfileService::toDto)
                .orElseGet(() -> toDto(new CompanyProfile("HMIS Engine")));
    }

    /**
     * Upserts the single profile row. The first call creates it; later
     * calls update the existing row in place.
     */
    @Transactional
    public CompanyProfileDto update(UpdateCompanyProfileRequest request) {
        CompanyProfile p = repo.findAll().stream().findFirst()
                .orElseGet(() -> repo.save(new CompanyProfile(request.name().trim())));
        p.setName(request.name().trim());
        p.setShortName(emptyToNull(request.shortName()));
        p.setAddress(emptyToNull(request.address()));
        p.setCity(emptyToNull(request.city()));
        p.setCountry(emptyToNull(request.country()));
        p.setPhone(emptyToNull(request.phone()));
        p.setEmail(emptyToNull(request.email()));
        p.setWebsite(emptyToNull(request.website()));
        p.setTaxId(emptyToNull(request.taxId()));
        p.setMotto(emptyToNull(request.motto()));
        p.setLogoUrl(emptyToNull(request.logoUrl()));
        if (request.currency() != null && !request.currency().isBlank()) {
            p.setCurrency(request.currency().toUpperCase());
        }
        p.setTimezone(emptyToNull(request.timezone()));
        return toDto(p);
    }

    private static CompanyProfileDto toDto(CompanyProfile p) {
        return new CompanyProfileDto(
                p.getName(), p.getShortName(),
                p.getAddress(), p.getCity(), p.getCountry(),
                p.getPhone(), p.getEmail(), p.getWebsite(),
                p.getTaxId(), p.getMotto(), p.getLogoUrl(),
                p.getCurrency(), p.getTimezone(),
                p.getUpdatedAt(), p.getUpdatedBy());
    }

    private static String emptyToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
