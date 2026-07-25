package org.omnaest.react4j.component.form.upload;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * Minimal, real (non-mocked) {@link UploadContent} test fixture backed by an in-memory byte array.
 */
public class StubUploadContent implements UploadContent
{
    private final byte[] bytes;
    private final String filename;
    private final String contentType;

    public StubUploadContent(byte[] bytes, String filename, String contentType)
    {
        super();
        this.bytes = bytes;
        this.filename = filename;
        this.contentType = contentType;
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

    @Override
    public InputStream inputStream()
    {
        return new ByteArrayInputStream(this.bytes);
    }

}
