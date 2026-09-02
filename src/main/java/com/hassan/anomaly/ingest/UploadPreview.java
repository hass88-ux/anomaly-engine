package com.hassan.anomaly.ingest;

import java.util.List;
import java.util.Map;

public record UploadPreview(
        String uploadId,
        String filename,
        long sizeBytes,
        List<String> headers,
        ColumnMapping detected,
        List<Map<String, String>> sampleRows,
        List<String> warnings
) {}