package com.otapp.hmis.engine.encounter.order.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClinicalOrderRepository extends JpaRepository<ClinicalOrder, Long> {

    Optional<ClinicalOrder> findByUid(String uid);

    List<ClinicalOrder> findAllByConsultationUidOrderByRequestedAtDesc(String consultationUid);

    List<ClinicalOrder> findAllByConsultationUidAndKindOrderByRequestedAtDesc(String consultationUid, ClinicalOrderKind kind);
}
