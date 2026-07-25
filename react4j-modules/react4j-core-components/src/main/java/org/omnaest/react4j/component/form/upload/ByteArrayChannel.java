package org.omnaest.react4j.component.form.upload;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Optional;
import java.util.Set;

import org.omnaest.react4j.component.form.upload.internal.AbstractUploadChannel;

/**
 * {@link UploadChannel} that captures an upload into memory. The hosting application reads the result back via {@link #getContent()}.
 * <p>
 * Instances are meant to be created once (e.g. {@code new ByteArrayChannel()} held as an instance/session field) and reused across renders - see the usage
 * constraint documented on {@link UploadChannel}.
 *
 * @author omnaest
 */
public class ByteArrayChannel extends AbstractUploadChannel
{
    private volatile UploadedContent content;

    public ByteArrayChannel()
    {
        super();
    }

    public static ByteArrayChannel create()
    {
        return new ByteArrayChannel();
    }

    public ByteArrayChannel withMaxSize(long maxSizeBytes)
    {
        this.setMaxSizeBytes(maxSizeBytes);
        return this;
    }

    public ByteArrayChannel withAcceptedContentTypes(Set<String> acceptedContentTypes)
    {
        this.setAcceptedContentTypes(acceptedContentTypes);
        return this;
    }

    /**
     * The most recently consumed upload, if any.
     *
     * @return
     */
    public Optional<UploadedContent> getContent()
    {
        return Optional.ofNullable(this.content);
    }

    @Override
    protected long writeTo(InputStream boundedStream, UploadContent content) throws IOException
    {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        boundedStream.transferTo(buffer);
        byte[] bytes = buffer.toByteArray();
        this.content = new DefaultUploadedContent(bytes, content.filename(), content.contentType());
        return bytes.length;
    }

    private static class DefaultUploadedContent implements UploadedContent
    {
        private final byte[] bytes;
        private final String filename;
        private final String contentType;

        private DefaultUploadedContent(byte[] bytes, String filename, String contentType)
        {
            super();
            this.bytes = bytes;
            this.filename = filename;
            this.contentType = contentType;
        }

        @Override
        public byte[] asBytes()
        {
            return this.bytes.clone();
        }

        @Override
        public String asString(Charset charset)
        {
            return new String(this.bytes, charset);
        }

        @Override
        public String filename()
        {
            return this.filename;
        }

        @Override
        public String contentType()
        {
            return this.contentType;
        }

        @Override
        public long size()
        {
            return this.bytes.length;
        }

    }

}
