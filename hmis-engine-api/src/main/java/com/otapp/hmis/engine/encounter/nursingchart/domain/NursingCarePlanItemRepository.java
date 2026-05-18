package com.otapp.hmis.engine.encounter.nursingchart.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NursingCarePlanItemRepository extends JpaRepository<NursingCarePlanItem, Long> {

    Optional<NursingCarePlanItem> findByUid(String uid);

    List<NursingCarePlanItem> findByAdmissionUidOrderByOpenedAtDesc(String admissionUid);
}
