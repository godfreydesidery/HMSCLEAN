package com.otapp.hmis.engine.masterdata.labtest.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LabTestAnalyteRepository extends JpaRepository<LabTestAnalyte, Long> {

    Optional<LabTestAnalyte> findByUid(String uid);

    List<LabTestAnalyte> findByLabTestTypeUidOrderByDisplayOrderAscCodeAsc(String labTestTypeUid);

    List<LabTestAnalyte> findByLabTestTypeUidAndActiveTrueOrderByDisplayOrderAscCodeAsc(String labTestTypeUid);

    boolean existsByLabTestTypeUidAndCode(String labTestTypeUid, String code);

    long countByLabTestTypeUid(String labTestTypeUid);
}
