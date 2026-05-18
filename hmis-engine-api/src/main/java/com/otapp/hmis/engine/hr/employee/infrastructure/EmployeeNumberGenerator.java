package com.otapp.hmis.engine.hr.employee.infrastructure;

import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** {@code EMP-YYYY-NNNNNN} employee numbers from a Postgres sequence. */
@Component
@RequiredArgsConstructor
public class EmployeeNumberGenerator {

    private final EntityManager em;

    @Transactional(propagation = Propagation.MANDATORY)
    public String next() {
        Number raw = (Number) em.createNativeQuery("SELECT nextval('hr_employee_no_seq')").getSingleResult();
        return String.format("EMP-%04d-%06d", LocalDate.now().getYear(), raw.longValue());
    }
}
