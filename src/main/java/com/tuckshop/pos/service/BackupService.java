package com.tuckshop.pos.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Backs up the H2 database to a timestamped zip file. Uses H2's own BACKUP TO command
 * rather than copying tuckshop.mv.db directly - a plain file copy can grab the database
 * mid-write, but BACKUP TO takes a consistent snapshot while the shop keeps using the app.
 */
@Service
public class BackupService {

    private static final String FILE_PREFIX = "tuckshop-backup-";
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final DataSource dataSource;
    private final ActivityLogService activityLogService;

    @Value("${backup.dir:data/backups}")
    private String backupDir;

    @Value("${backup.keep:30}")
    private int keepCount;

    public BackupService(DataSource dataSource, ActivityLogService activityLogService) {
        this.dataSource = dataSource;
        this.activityLogService = activityLogService;
    }

    // Runs once a day; time is configurable via backup.cron for shops with different quiet hours.
    @Scheduled(cron = "${backup.cron:0 0 3 * * *}")
    public void scheduledBackup() {
        createBackup("scheduled");
    }

    public Path createBackup(String trigger) {
        try {
            Path dir = Paths.get(backupDir);
            Files.createDirectories(dir);

            String filename = FILE_PREFIX + LocalDateTime.now().format(STAMP) + ".zip";
            Path target = dir.resolve(filename);

            try (Connection conn = dataSource.getConnection(); Statement st = conn.createStatement()) {
                st.execute("BACKUP TO '" + target.toAbsolutePath().toString().replace("'", "''") + "'");
            }

            activityLogService.log("BACKUP_CREATED", "Backup created (" + trigger + "): " + filename);
            pruneOldBackups(dir);
            return target;
        } catch (Exception e) {
            throw new RuntimeException("Backup failed: " + e.getMessage(), e);
        }
    }

    private void pruneOldBackups(Path dir) throws IOException {
        try (Stream<Path> files = Files.list(dir)) {
            List<Path> backups = files
                    .filter(p -> p.getFileName().toString().startsWith(FILE_PREFIX))
                    .sorted(Comparator.comparing(this::lastModifiedSafe).reversed())
                    .toList();
            for (int i = keepCount; i < backups.size(); i++) {
                Files.deleteIfExists(backups.get(i));
            }
        }
    }

    private Instant lastModifiedSafe(Path p) {
        try {
            return Files.getLastModifiedTime(p).toInstant();
        } catch (IOException e) {
            return Instant.EPOCH;
        }
    }

    public List<BackupFile> list() {
        Path dir = Paths.get(backupDir);
        if (!Files.exists(dir)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(dir)) {
            return files
                    .filter(p -> p.getFileName().toString().startsWith(FILE_PREFIX))
                    .map(this::toBackupFile)
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparing(BackupFile::createdAt).reversed())
                    .toList();
        } catch (IOException e) {
            throw new RuntimeException("Could not list backups: " + e.getMessage(), e);
        }
    }

    private BackupFile toBackupFile(Path p) {
        try {
            return new BackupFile(p.getFileName().toString(), Files.size(p), Files.getLastModifiedTime(p).toInstant());
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Resolves a backup filename for download, rejecting anything that escapes the backup
     * directory (blocks path traversal like "../../application.properties").
     */
    public Path resolveForDownload(String filename) {
        Path dir = Paths.get(backupDir).toAbsolutePath().normalize();
        Path target = dir.resolve(filename).normalize();
        if (!target.startsWith(dir) || !Files.isRegularFile(target)) {
            throw new IllegalArgumentException("Backup not found.");
        }
        return target;
    }

    public record BackupFile(String filename, long sizeBytes, Instant createdAt) {}
}
