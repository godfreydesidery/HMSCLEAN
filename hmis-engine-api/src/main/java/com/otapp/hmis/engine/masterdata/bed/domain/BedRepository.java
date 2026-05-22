package com.otapp.hmis.engine.masterdata.bed.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BedRepository extends JpaRepository<Bed, Long> {

    Optional<Bed> findByUid(String uid);

    boolean existsByWardUidAndLabel(String wardUid, String label);

    List<Bed> findByWardUidOrderByLabelAsc(String wardUid);

    /** [wardUid, status, count] roll-ups for the bed-occupancy report. */
    @Query("""
            SELECT b.wardUid, b.status, COUNT(b)
            FROM Bed b
            WHERE b.active = TRUE
            GROUP BY b.wardUid, b.status
            """)
    List<Object[]> countByWardAndStatus();
}
