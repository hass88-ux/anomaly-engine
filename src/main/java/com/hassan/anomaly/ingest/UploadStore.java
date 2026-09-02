package com.hassan.anomaly.ingest;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.UUID;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * Holds uploaded files on local disk between upload and analysis.
 *
 * This is deliberately the simplest thing that works on a single instance.
 * Behind a load balancer the analyse request could land on a different machine
 * than the upload did, at which point this needs to become S3. Documented as a
 * known limitation rather than hidden.
 */
@Component
public class UploadStore {

    private static final Logger log = LoggerFactory.getLogger(UploadStore.class);

    private final Path root;
    private final Duration retention;

    public UploadStore(@Value("${anomaly.upload.retention-hours:6}") long retentionHours) {
        this.retention = Duration.ofHours(retentionHours);
        try {
            this.root = Files.createDirectories(
                    Path.of(System.getProperty("java.io.tmpdir"), "anomaly-uploads"));
        } catch (IOException e) {
            throw new UncheckedIOException("Could not create upload directory", e);
        }
        log.info("Upload directory: {}", root);
    }

    public String store(MultipartFile file, String username) throws IOException {
        String id = UUID.randomUUID().toString();
        Path target = pathFor(id, username);

        Files.createDirectories(target.getParent());
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }

        return id;
    }

    public boolean exists(String id, String username) {
        return Files.isRegularFile(pathFor(id, username));
    }

    public InputStream open(String id, String username) throws IOException {
        Path path = pathFor(id, username);
        if (!Files.isRegularFile(path)) {
            throw new IOException("Upload " + id + " not found");
        }
        return Files.newInputStream(path);
    }

    public long sizeOf(String id, String username) throws IOException {
        return Files.size(pathFor(id, username));
    }

    public void delete(String id, String username) {
        try {
            Files.deleteIfExists(pathFor(id, username));
        } catch (IOException e) {
            log.warn("Could not delete upload {}", id, e);
        }
    }

    private Path pathFor(String id, String username) {
        String safeUser = username.replaceAll("[^A-Za-z0-9_.-]", "_");
        String safeId = UUID.fromString(id).toString();
        return root.resolve(safeUser).resolve(safeId + ".csv");
    }

    @Scheduled(fixedDelay = 1800000)
    public void purgeExpired() {
        Instant cutoff = Instant.now().minus(retention);

        try (Stream<Path> files = Files.walk(root)) {
            files.sorted(Comparator.reverseOrder())
                    .filter(Files::isRegularFile)
                    .forEach(path -> {
                        try {
                            if (Files.getLastModifiedTime(path).toInstant().isBefore(cutoff)) {
                                Files.delete(path);
                                log.info("Purged expired upload {}", path.getFileName());
                            }
                        } catch (IOException e) {
                            log.warn("Could not purge {}", path, e);
                        }
                    });
        } catch (IOException e) {
            log.warn("Purge sweep failed", e);
        }
    }
}