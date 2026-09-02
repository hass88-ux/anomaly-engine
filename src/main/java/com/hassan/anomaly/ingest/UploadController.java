package com.hassan.anomaly.ingest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/uploads")
public class UploadController {

    private static final int SAMPLE_ROWS = 5;

    private final UploadStore store;

    public UploadController(UploadStore store) {
        this.store = store;
    }

    @PostMapping
    public ResponseEntity<?> upload(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal String username) throws IOException {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "The uploaded file is empty"));
        }

        String name = file.getOriginalFilename() == null ? "upload.csv" : file.getOriginalFilename();
        if (!name.toLowerCase().endsWith(".csv") && !name.toLowerCase().endsWith(".txt")) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Only .csv and .txt files are accepted"));
        }

        String uploadId = store.store(file, username);

        try {
            UploadPreview preview = preview(uploadId, name, file.getSize(), username);
            if (preview.headers().isEmpty()) {
                store.delete(uploadId, username);
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Could not read a header row from this file"));
            }
            return ResponseEntity.ok(preview);
        } catch (IOException e) {
            store.delete(uploadId, username);
            throw e;
        }
    }

    private UploadPreview preview(String uploadId, String filename, long size, String username)
            throws IOException {

        List<String> headers = List.of();
        List<Map<String, String>> samples = new ArrayList<>();

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreSurroundingSpaces(true)
                .setIgnoreEmptyLines(true)
                .setTrim(true)
                .build();

        try (InputStream in = store.open(uploadId, username);
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(new BomStrippingStream(in), StandardCharsets.UTF_8));
             CSVParser parser = CSVParser.parse(reader, format)) {

            headers = List.copyOf(parser.getHeaderNames());

            for (CSVRecord record : parser) {
                if (samples.size() >= SAMPLE_ROWS) {
                    break;
                }
                Map<String, String> row = new LinkedHashMap<>();
                for (String header : headers) {
                    row.put(header, record.isMapped(header) ? record.get(header) : "");
                }
                samples.add(row);
            }
        }

        ColumnMapping detected = ColumnDetector.detect(headers);

        List<String> warnings = new ArrayList<>();
        if (!detected.hasRequired()) {
            warnings.add("Could not identify the account, timestamp and amount columns. "
                    + "Please choose them below.");
        }
        if (!detected.hasGeo()) {
            warnings.add("No latitude and longitude columns found. "
                    + "The impossible-travel rule will be skipped.");
        }
        if (!detected.hasGroundTruth()) {
            warnings.add("No fraud label column found. Alerts will be produced, "
                    + "but precision and recall cannot be measured on this file.");
        }

        return new UploadPreview(uploadId, filename, size, headers, detected, samples, warnings);
    }
}