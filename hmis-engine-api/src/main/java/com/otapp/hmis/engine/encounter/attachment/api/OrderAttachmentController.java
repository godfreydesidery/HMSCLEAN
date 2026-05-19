package com.otapp.hmis.engine.encounter.attachment.api;

import com.otapp.hmis.engine.encounter.attachment.application.OrderAttachmentDtos.OrderAttachmentDto;
import com.otapp.hmis.engine.encounter.attachment.application.OrderAttachmentService;
import com.otapp.hmis.engine.encounter.attachment.application.OrderAttachmentService.DownloadHandle;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

@Tag(name = "Clinical order attachments")
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ENCOUNTER_ACCESS')")
public class OrderAttachmentController {

    private final OrderAttachmentService attachmentService;

    @PostMapping(value = "/encounters/orders/uid/{orderUid}/attachments",
                 consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<OrderAttachmentDto> upload(@PathVariable String orderUid,
                                                     @RequestParam("file") MultipartFile file,
                                                     @RequestParam(value = "description", required = false) String description) {
        OrderAttachmentDto dto = attachmentService.upload(orderUid, file, description);
        URI location = UriComponentsBuilder
                .fromPath("/encounters/attachments/uid/{attachmentUid}/download")
                .buildAndExpand(dto.uid()).toUri();
        return ResponseEntity.created(location).body(dto);
    }

    @GetMapping("/encounters/orders/uid/{orderUid}/attachments")
    public ResponseEntity<List<OrderAttachmentDto>> list(@PathVariable String orderUid) {
        return ResponseEntity.ok(attachmentService.list(orderUid));
    }

    @GetMapping("/encounters/attachments/uid/{attachmentUid}/download")
    public ResponseEntity<Resource> download(@PathVariable String attachmentUid) {
        DownloadHandle handle = attachmentService.download(attachmentUid);
        // Quote the filename so spaces / non-ASCII don't break the Content-Disposition header.
        String dispositionFilename = handle.filename().replace("\"", "");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + dispositionFilename + "\"")
                .header(HttpHeaders.CONTENT_TYPE,
                        handle.contentType() == null
                                ? MediaType.APPLICATION_OCTET_STREAM_VALUE
                                : handle.contentType())
                .header(HttpHeaders.CONTENT_LENGTH, Long.toString(handle.sizeBytes()))
                .body(handle.resource());
    }

    @DeleteMapping("/encounters/attachments/uid/{attachmentUid}")
    public ResponseEntity<Void> delete(@PathVariable String attachmentUid) {
        attachmentService.delete(attachmentUid);
        return ResponseEntity.noContent().build();
    }
}
