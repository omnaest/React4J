package org.omnaest.react4j.component.form.upload;

import java.nio.charset.Charset;

/**
 * Result container exposed by an in-memory {@link UploadChannel} (e.g. {@link ByteArrayChannel}) so the hosting application can read back the bytes it
 * received - the upload analogue of {@link org.omnaest.react4j.domain.context.data.Value}.
 *
 * @author omnaest
 */
public interface UploadedContent
{
    public byte[] asBytes();

    public String asString(Charset charset);

    public String filename();

    public String contentType();

    public long size();

}
