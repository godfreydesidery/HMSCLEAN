package com.otapp.hmis.engine.hr.employee.domain;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Optional<Employee> findByUid(String uid);

    Optional<Employee> findByUsername(String username);

    boolean existsByEmployeeNo(String employeeNo);

    boolean existsByUsername(String username);

    boolean existsByTinNo(String tinNo);

    /**
     * Payroll import roster: every employee currently in {@code status}. Callers
     * pass {@link EmploymentStatus#ACTIVE}, matching the legacy import
     * ({@code findAllByActive(true)}), which imported all active staff regardless
     * of the {@code payable} flag. ({@code payable} stays captured-only data, as
     * in legacy, where the import never consulted it.)
     */
    java.util.List<Employee> findAllByEmploymentStatus(EmploymentStatus status);

    @Query("""
            SELECT e FROM Employee e
            WHERE (:search IS NULL OR :search = ''
                   OR LOWER(e.employeeNo) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(e.firstName)  LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(e.lastName)   LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(e.username)   LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:status      IS NULL OR e.employmentStatus = :status)
              AND (:designation IS NULL OR :designation = '' OR LOWER(e.designation) = LOWER(:designation))
              AND (:department  IS NULL OR :department  = '' OR LOWER(e.department)  = LOWER(:department))
            """)
    Page<Employee> search(@Param("search") String search,
                          @Param("status") EmploymentStatus status,
                          @Param("designation") String designation,
                          @Param("department") String department,
                          Pageable pageable);
}
