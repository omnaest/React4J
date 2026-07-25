package org.omnaest.react4j.service.internal.upload;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;

import jakarta.servlet.MultipartConfigElement;

/**
 * Contributes the servlet-level {@link MultipartConfigElement} global ceiling for uploads, sized from {@code react4j.upload.max-file-size} /
 * {@code react4j.upload.max-request-size} (default 25 MB each). This is the coarse global limit; the finer per-element bound and the actual DoS guard is
 * each {@link org.omnaest.react4j.component.form.upload.UploadChannel#maxSizeBytes()}.
 */
@Configuration
public class FileUploadConfiguration
{
    @Bean
    public MultipartConfigElement multipartConfigElement(@Value("${react4j.upload.max-file-size:25MB}") String maxFileSize, @Value("${react4j.upload.max-request-size:25MB}") String maxRequestSize)
    {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        factory.setMaxFileSize(DataSize.parse(maxFileSize));
        factory.setMaxRequestSize(DataSize.parse(maxRequestSize));
        return factory.createMultipartConfig();
    }

}
