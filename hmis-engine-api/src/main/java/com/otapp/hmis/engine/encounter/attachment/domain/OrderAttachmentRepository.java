package com.otapp.hmis.engine.encounter.attachment.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderAttachmentRepository extends JpaRepository<OrderAttachment, Long> {

    Optional<OrderAttachment> findByUid(String uid);

    List<OrderAttachment> findAllByOrderUidOrderByUploadedAtAsc(String orderUid);
}
