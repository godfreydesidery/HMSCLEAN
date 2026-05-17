package com.otapp.hmis.engine.iam.application;

import com.otapp.hmis.engine.common.api.PageResponse;
import com.otapp.hmis.engine.iam.application.dto.LoginAttemptDto;
import com.otapp.hmis.engine.iam.domain.LoginAttempt;
import com.otapp.hmis.engine.iam.domain.LoginAttemptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final LoginAttemptRepository attemptRepository;

    @Transactional(readOnly = true)
    public PageResponse<LoginAttemptDto> searchLoginAttempts(String username,
                                                             LoginAttempt.Outcome outcome,
                                                             Pageable pageable) {
        return PageResponse.from(
                attemptRepository.search(emptyToNull(username), outcome, pageable)
                        .map(AuditService::toDto));
    }

    private static LoginAttemptDto toDto(LoginAttempt a) {
        return new LoginAttemptDto(
                a.getUid(),
                a.getUsername(),
                a.getOutcome(),
                a.getIpAddress(),
                a.getUserAgent(),
                a.getAttemptedAt());
    }

    private static String emptyToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
