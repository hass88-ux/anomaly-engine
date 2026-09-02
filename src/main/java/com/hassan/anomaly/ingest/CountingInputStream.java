package com.hassan.anomaly.ingest;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Counts bytes as they pass through, so analysis progress can be reported
 * against the known file size without a counting pass over the rows.
 */
public class CountingInputStream extends FilterInputStream {

    private volatile long count;

    public CountingInputStream(InputStream in) {
        super(in);
    }

    public long count() {
        return count;
    }

    @Override
    public int read() throws IOException {
        int b = super.read();
        if (b != -1) {
            count++;
        }
        return b;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int read = super.read(b, off, len);
        if (read > 0) {
            count += read;
        }
        return read;
    }
}
