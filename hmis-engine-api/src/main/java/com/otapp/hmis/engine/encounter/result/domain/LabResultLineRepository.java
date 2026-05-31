package com.otapp.hmis.engine.encounter.result.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LabResultLineRepository extends JpaRepository<LabResultLine, Long> {

    List<LabResultLine> findByOrderResultUidOrderByDisplayOrderAscAnalyteCodeAsc(String orderResultUid);

    void deleteByOrderResultUid(String orderResultUid);
}
