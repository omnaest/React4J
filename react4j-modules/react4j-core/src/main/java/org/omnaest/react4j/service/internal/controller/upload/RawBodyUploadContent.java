package org.omnaest.react4j.service.internal.controller.upload;

import java.io.IOException;
import java.io.InputStream;

import org.omnaest.react4j.component.form.upload.UploadContent;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Adapts the raw (non-multipart) body of an {@link HttpServletRequest} to the domain-free {@link UploadContent} facade, so that
 * {@link org.omnaest.react4j.component.form.upload.UploadChannel} implementations never need to know which transport delivered the bytes.
 * <p>
 * Sibling of {@code org.omnaest.react4j.service.internal.upload.MultipartUploadContent}, for the unbuffered upload transport
 * (see {@link FileUploadController}). Reads only plain header/metadata accessors and the already-opened, already size-bounded request {@link InputStream} it
 * is constructed with - never {@code getPart}/{@code getParts}/{@code getParameter} or anything else that would trigger container-side multipart parsing.
 *
 * @author omnaest
 */
class RawBodyUploadContent implements UploadContent
{
    private final HttpServletRequest request;
    private final InputStream        inputStream;
    private final String             filename;

    RawBodyUploadContent(HttpServletRequest request, InputStream inputStream, String filename)
    {
        super();
        this.request = request;
        this.inputStream = inputStream;
        this.filename = filename;
    }

    @Override
    public String filename()
    {
        return this.filename;
    }

    @Override
    public String contentType()
    {
        return this.request.getContentType();
    }

    @Override
    public long size()
    {
        return this.request.getContentLengthLong();
    }

    @Override
    public InputStream inputStream() throws IOException
    {
        return this.inputStream;
    }

}
