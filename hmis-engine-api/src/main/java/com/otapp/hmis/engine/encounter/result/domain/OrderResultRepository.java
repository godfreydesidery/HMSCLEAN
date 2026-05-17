package com.otapp.hmis.engine.encounter.result.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderResultRepository extends JpaRepository<OrderResult, Long> {

    Optional<OrderResult> findByOrderUid(String orderUid);
}
