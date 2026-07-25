package org.omnaest.react4j.service.internal.upload;

import org.omnaest.react4j.component.form.upload.UploadException;
import org.omnaest.react4j.component.form.upload.UploadReceipt;
import org.springframework.web.multipart.MultipartFile;

/**
 * Looks up the {@link org.omnaest.react4j.component.form.upload.UploadChannel} registered for a given uploadId and consumes the given multipart file into
 * it.
 */
public interface FileUploadService
{
    /**
     * @param uploadId
     * @param file
     * @return
     * @throws UnknownUploadIdException
     *             if no channel is registered for {@code uploadId}
     * @throws UploadException
     *             if the channel rejects the upload (size/content-type/IO)
     */
    public UploadReceipt consume(String uploadId, MultipartFile file);

}
