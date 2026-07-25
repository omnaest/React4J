package org.omnaest.react4j.component.form.upload.internal;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Set;

import org.omnaest.react4j.component.form.upload.UploadChannel;
import org.omnaest.react4j.component.form.upload.UploadContent;
import org.omnaest.react4j.component.form.upload.UploadException;
import org.omnaest.react4j.component.form.upload.UploadException.Reason;
import org.omnaest.react4j.component.form.upload.UploadReceipt;

/**
 * Shared {@link UploadChannel#consume(UploadContent)} skeleton: an optional content-type pre-check, followed by a size-bounded read delegated to
 * {@link #writeTo(InputStream, UploadContent)}.
 *
 * @author omnaest
 */
public abstract class AbstractUploadChannel implements UploadChannel
{
    public static final long     DEFAULT_MAX_SIZE_BYTES = 10L * 1024 * 1024;

    private volatile long        maxSizeBytes;
    private volatile Set<String> acceptedContentTypes;

    protected AbstractUploadChannel()
    {
        this(DEFAULT_MAX_SIZE_BYTES, Collections.emptySet());
    }

    protected AbstractUploadChannel(long maxSizeBytes, Set<String> acceptedContentTypes)
    {
        super();
        this.maxSizeBytes = maxSizeBytes;
        this.acceptedContentTypes = copyOf(acceptedContentTypes);
    }

    private static Set<String> copyOf(Set<String> acceptedContentTypes)
    {
        return acceptedContentTypes == null ? Collections.emptySet() : Set.copyOf(acceptedContentTypes);
    }

    protected void setMaxSizeBytes(long maxSizeBytes)
    {
        this.maxSizeBytes = maxSizeBytes;
    }

    protected void setAcceptedContentTypes(Set<String> acceptedContentTypes)
    {
        this.acceptedContentTypes = copyOf(acceptedContentTypes);
    }

    @Override
    public long maxSizeBytes()
    {
        return this.maxSizeBytes;
    }

    @Override
    public Set<String> acceptedContentTypes()
    {
        return this.acceptedContentTypes;
    }

    @Override
    public final UploadReceipt consume(UploadContent content)
    {
        if (content == null)
        {
            throw new UploadException(Reason.IO_ERROR, "No upload content provided");
        }

        if (!this.acceptedContentTypes.isEmpty() && !this.acceptedContentTypes.contains(content.contentType()))
        {
            throw new UploadException(Reason.CONTENT_TYPE_REJECTED, "Content type not accepted: " + content.contentType());
        }

        try (InputStream boundedStream = new BoundedInputStream(content.inputStream(), this.maxSizeBytes))
        {
            long size = this.writeTo(boundedStream, content);
            return UploadReceipt.builder()
                                .filename(content.filename())
                                .contentType(content.contentType())
                                .size(size)
                                .build();
        }
        catch (BoundedInputStream.SizeLimitExceededException e)
        {
            throw new UploadException(Reason.SIZE_EXCEEDED, "Upload exceeds the maximum allowed size of " + this.maxSizeBytes + " bytes", e);
        }
        catch (IOException e)
        {
            throw new UploadException(Reason.IO_ERROR, "Failed to consume upload", e);
        }
    }

    /**
     * Consumes the (already size-bounded) stream into this channel's sink and returns the number of bytes written.
     *
     * @param boundedStream
     * @param content
     * @return
     * @throws IOException
     */
    protected abstract long writeTo(InputStream boundedStream, UploadContent content) throws IOException;

}
