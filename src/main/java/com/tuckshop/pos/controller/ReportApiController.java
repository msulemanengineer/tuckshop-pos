package com.tuckshop.pos.controller;

import com.tuckshop.pos.service.ReportExportService;
import com.tuckshop.pos.service.ReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
public class ReportApiController {

    private final ReportService reportService;
    private final ReportExportService reportExportService;

    public ReportApiController(ReportService reportService, ReportExportService reportExportService) {
        this.reportService = reportService;
        this.reportExportService = reportExportService;
    }

    @GetMapping
    public Map<String, Object> report(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String paymentMethod) {
        return reportService.buildReport(from, to, customerId, paymentMethod);
    }

    @GetMapping(value = "/export/csv", produces = "text/csv")
    public ResponseEntity<String> exportCsv(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String paymentMethod) {
        String csv = reportService.toCsv(from, to, customerId, paymentMethod);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header("Content-Disposition", "attachment; filename=\"sales-report-" + from + "-to-" + to + ".csv\"")
                .body(csv);
    }

    @GetMapping("/export/xlsx")
    public ResponseEntity<byte[]> exportExcel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String paymentMethod) throws IOException {
        Map<String, Object> report = reportService.buildReport(from, to, customerId, paymentMethod);
        byte[] bytes = reportExportService.toExcel(report);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header("Content-Disposition", "attachment; filename=\"sales-report-" + from + "-to-" + to + ".xlsx\"")
                .body(bytes);
    }

    @GetMapping("/export/pdf")
    public ResponseEntity<byte[]> exportPdf(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String paymentMethod) throws IOException {
        Map<String, Object> report = reportService.buildReport(from, to, customerId, paymentMethod);
        byte[] bytes = reportExportService.toPdf(report);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header("Content-Disposition", "attachment; filename=\"sales-report-" + from + "-to-" + to + ".pdf\"")
                .body(bytes);
    }
}
