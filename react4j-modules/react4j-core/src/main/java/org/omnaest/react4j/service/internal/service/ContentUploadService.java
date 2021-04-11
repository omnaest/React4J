package org.omnaest.react4j.service.internal.service;

import java.util.function.Supplier;

import org.springframework.web.multipart.MultipartFile;

/**
 * Services which handles the upload of content files
 * 
 * @author omnaest
 */
public interface ContentUploadService
{
    public ValidatedStorageAccess validateToken(String token);

    public static interface ValidatedStorageAccess
    {

        public ValidatedStorageAccess ifValidConsume(MultipartFile file);

        public ValidatedStorageAccess ifInvalidThrow(Supplier<RuntimeException> exceptionProvider);

    }
}
