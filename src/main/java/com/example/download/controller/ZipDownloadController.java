package com.example.download.controller;

import com.example.download.service.ZipDownloadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@Controller
public class ZipDownloadController {

    private static final Logger log = LoggerFactory.getLogger(ZipDownloadController.class);
    private static final String CONTENT_DISPOSITION = "attachment; filename=\"download.zip\"";

    private final ZipDownloadService zipDownloadService;

    public ZipDownloadController(ZipDownloadService zipDownloadService) {
        this.zipDownloadService = zipDownloadService;
    }

    @GetMapping("/download-zip")
    public ResponseEntity<StreamingResponseBody> downloadZip(@RequestParam("dirSeq") Long dirSeq) {
        log.info("Start download dirSeq={}", dirSeq);

        StreamingResponseBody responseBody =
                outputStream -> zipDownloadService.streamZip(dirSeq, outputStream);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .header(HttpHeaders.CONTENT_DISPOSITION, CONTENT_DISPOSITION)
                .body(responseBody);
    }
}
