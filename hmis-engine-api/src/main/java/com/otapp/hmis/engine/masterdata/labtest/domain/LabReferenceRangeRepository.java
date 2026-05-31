package com.otapp.hmis.engine.masterdata.labtest.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LabReferenceRangeRepository extends JpaRepository<LabReferenceRange, Long> {

    Optional<LabReferenceRange> findByUid(String uid);

    List<LabReferenceRange> findByAnalyteUidOrderBySexAscCreatedAtAsc(String analyteUid);

    List<LabReferenceRange> findByAnalyteUidAndActiveTrue(String analyteUid);

    void deleteByAnalyteUid(String analyteUid);
}
