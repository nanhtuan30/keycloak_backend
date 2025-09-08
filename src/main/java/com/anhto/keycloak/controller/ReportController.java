package com.anhto.keycloak.controller;

import com.anhto.keycloak.service.ReportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
@CrossOrigin(origins = "*") // hoặc đổi thành http://localhost:4200 khi dùng Angular
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/export/{format}")
    public ResponseEntity<byte[]> exportReport(@PathVariable String format) {
        try {
            byte[] reportData = reportService.exportReport(format);

            String filename = "products_report" + reportService.getFileExtension(format);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .header(HttpHeaders.CONTENT_TYPE, reportService.getContentType(format))
                    .body(reportData);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage().getBytes());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(("Error: " + e.getMessage()).getBytes());
        }
    }
}
