package com.hassan.anomaly.ingest;

import java.io.IOException;
import java.io.InputStream;

/**
 * Strips a UTF-8 byte order mark from the head of a stream if one is present.
 * Excel writes a BOM when saving CSV, which would otherwise become part of the
 * first header name and break column matching.
 */
public class BomStrippingStream extends InputStream {

    private static final int[] UTF8_BOM = { 0xEF, 0xBB, 0xBF };

    private final InputStream delegate;
    private int[] buffered = new int[0];
    private int bufferedIndex;
    private boolean checked;

    public BomStrippingStream(InputStream delegate) {
        this.delegate = delegate;
    }

    @Override
    public int read() throws IOException {
        if (!checked) {
            checkForBom();
        }

        if (bufferedIndex < buffered.length) {
            return buffered[bufferedIndex++];
        }

        return delegate.read();
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }

    private void checkForBom() throws IOException {
        checked = true;

        int[] head = new int[3];
        int read = 0;

        while (read < 3) {
            int b = delegate.read();
            if (b == -1) {
                break;
            }
            head[read++] = b;
        }

        if (read == 3
                && head[0] == UTF8_BOM[0]
                && head[1] == UTF8_BOM[1]
                && head[2] == UTF8_BOM[2]) {
            buffered = new int[0];
            return;
        }

        buffered = new int[read];
        System.arraycopy(head, 0, buffered, 0, read);
        bufferedIndex = 0;
    }
}