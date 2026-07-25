package org.omnaest.react4j.component.form.upload;

import java.io.IOException;
import java.io.InputStream;

/**
 * Domain-free facade over an incoming upload part (e.g. a multipart file), so that {@link UploadChannel} implementations never need to import Spring or
 * servlet types.
 *
 * @author omnaest
 */
public interface UploadContent
{
    /**
     * The original filename as supplied by the client. This is metadata only - it must never be used directly to build a filesystem destination path (see
     * {@link FileChannel}).
     *
     * @return
     */
    public String filename();

    /**
     * The declared content type of the upload, or null if unknown.
     *
     * @return
     */
    public String contentType();

    /**
     * The declared size in bytes, or -1 if unknown. This is a hint only - {@link UploadChannel} implementations must enforce their real size limit while
     * reading the {@link #inputStream()}.
     *
     * @return
     */
    public long size();

    /**
     * The raw bytes of the upload.
     *
     * @return
     * @throws IOException
     */
    public InputStream inputStream() throws IOException;

}
