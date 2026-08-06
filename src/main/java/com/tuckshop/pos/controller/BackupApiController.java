package com.tuckshop.pos.controller;

import com.tuckshop.pos.dto.ApiError;
import com.tuckshop.pos.service.BackupService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

// Owner-only, enforced both by SecurityConfig (/api/backups/**) and here with @PreAuthorize,
// matching the pattern used for /api/users. A backup file contains the entire khata ledger
// and sales history, so it gets the same access level as staff account management.
@RestController
@RequestMapping("/api/backups")
@PreAuthorize("hasRole('OWNER')")
public class BackupApiController {

    private final BackupService backupService;

    public BackupApiController(BackupService backupService) {
        this.backupService = backupService;
    }

    @GetMapping
    public List<BackupService.BackupFile> list() {
        return backupService.list();
    }

    @PostMapping("/run")
    public ResponseEntity<?> runNow() {
        try {
            Path created = backupService.createBackup("manual");
            return ResponseEntity.ok(Map.of("filename", created.getFileName().toString()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new ApiError(e.getMessage()));
        }
    }

    @GetMapping("/download/{filename}")
    public ResponseEntity<?> download(@PathVariable String filename) {
        try {
            Path file = backupService.resolveForDownload(filename);
            FileSystemResource resource = new FileSystemResource(file);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            ContentDisposition.attachment().filename(file.getFileName().toString()).build().toString())
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(new ApiError(e.getMessage()));
        }
    }
}
