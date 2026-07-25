package org.omnaest.react4j.component.form.upload.internal;

import java.io.IOException;
import java.io.InputStream;

/**
 * {@link InputStream} decorator that aborts as soon as more than a fixed number of bytes have been read, so a caller can bound memory/disk usage while
 * streaming without ever buffering the offending bytes.
 * <p>
 * Domain-free - imports neither Spring nor any React4J UI type.
 *
 * @author omnaest
 */
public class BoundedInputStream extends InputStream
{
    private final InputStream delegate;
    private final long        maxBytes;
    private long              bytesRead = 0;

    public BoundedInputStream(InputStream delegate, long maxBytes)
    {
        super();
        this.delegate = delegate;
        this.maxBytes = maxBytes;
    }

    @Override
    public int read() throws IOException
    {
        int value = this.delegate.read();
        if (value != -1)
        {
            this.bytesRead++;
            this.assertWithinLimit();
        }
        return value;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException
    {
        int numberOfBytesRead = this.delegate.read(b, off, len);
        if (numberOfBytesRead > 0)
        {
            this.bytesRead += numberOfBytesRead;
            this.assertWithinLimit();
        }
        return numberOfBytesRead;
    }

    private void assertWithinLimit() throws IOException
    {
        if (this.bytesRead > this.maxBytes)
        {
            throw new SizeLimitExceededException("Stream exceeded the maximum allowed size of " + this.maxBytes + " bytes");
        }
    }

    @Override
    public void close() throws IOException
    {
        this.delegate.close();
    }

    /**
     * Thrown when the wrapped stream has produced more bytes than the configured limit.
     */
    public static class SizeLimitExceededException extends IOException
    {
        private static final long serialVersionUID = 1L;

        public SizeLimitExceededException(String message)
        {
            super(message);
        }
    }

}
