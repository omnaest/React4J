package org.omnaest.react4j.service.internal.upload;

/**
 * Thrown by {@link FileUploadService#consume(String, org.springframework.web.multipart.MultipartFile)} when the given uploadId is not (or no longer)
 * registered in the {@link UploadChannelRegistry} - mapped by {@code FileUploadController} to a 404 response.
 */
public class UnknownUploadIdException extends RuntimeException
{
    private static final long serialVersionUID = 1L;

    public UnknownUploadIdException(String uploadId)
    {
        super("Unknown uploadId: " + uploadId);
    }

}
