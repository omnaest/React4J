package org.omnaest.react4j.service.internal.upload;

import java.io.IOException;
import java.io.InputStream;

import org.omnaest.react4j.component.form.upload.UploadContent;
import org.springframework.web.multipart.MultipartFile;

/**
 * Adapts a Spring {@link MultipartFile} to the domain-free {@link UploadContent} facade so that react4j-core-components never needs to import Spring/servlet
 * types.
 */
class MultipartUploadContent implements UploadContent
{
    private final MultipartFile file;

    MultipartUploadContent(MultipartFile file)
    {
        super();
        this.file = file;
    }

    @Override
    public String filename()
    {
        return this.file.getOriginalFilename();
    }

    @Override
    public String contentType()
    {
        return this.file.getContentType();
    }

    @Override
    public long size()
    {
        return this.file.getSize();
    }

    @Override
    public InputStream inputStream() throws IOException
    {
        return this.file.getInputStream();
    }

}
