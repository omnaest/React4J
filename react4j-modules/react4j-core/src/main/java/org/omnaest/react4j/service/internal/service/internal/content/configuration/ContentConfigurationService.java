package org.omnaest.react4j.service.internal.service.internal.content.configuration;

import java.io.File;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.omnaest.utils.FileUtils;
import org.omnaest.utils.JSONHelper;
import org.omnaest.utils.StringUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Handles the content-configuration.json file which configures the content folder and upload
 * 
 * @author omnaest
 */
@Component
public class ContentConfigurationService
{
    private AtomicReference<ContentConfiguration> configuration = new AtomicReference<>();

    public Optional<String> getAccessToken()
    {
        return Optional.ofNullable(this.configuration.get())
                       .map(ContentConfiguration::getAccessToken);
    }

    public boolean isFileUploadEnabled()
    {
        return Optional.ofNullable(this.configuration.get())
                       .map(ContentConfiguration::isEnableFileUpload)
                       .orElse(false);
    }

    @Scheduled(fixedDelay = 10 * 1000)
    private void synchronize()
    {
        File contentConfigurationFile = new File("content-configuration.json");
        ContentConfiguration contentConfiguration = FileUtils.toSupplier(contentConfigurationFile)
                                                             .with(JSONHelper.deserializer(ContentConfiguration.class))
                                                             .get();
        if (contentConfiguration != null)
        {
            this.configuration.set(contentConfiguration);
        }
        else
        {
            FileUtils.toConsumer(contentConfigurationFile)
                     .with(JSONHelper.serializer()
                                     .withPrettyPrint())
                     .accept(new ContentConfiguration());
        }
    }

    public static class ContentConfiguration
    {
        private static final String DEAFAULT_TOKEN = StringUtils.repeat(UUID.randomUUID()
                                                                            .toString(),
                                                                        4);

        @JsonProperty
        private String accessToken = DEAFAULT_TOKEN;

        @JsonProperty
        private boolean enableFileUpload = false;

        @JsonProperty
        private String contentAttachmentFolder = "content/attachments";

        @JsonProperty
        private String contentImagesFolder = "content/images";

        @JsonProperty
        private String contentFolder = "content";

        public String getAccessToken()
        {
            return this.accessToken;
        }

        public boolean isEnableFileUpload()
        {
            return this.enableFileUpload;
        }

        public String getContentAttachmentFolder()
        {
            return this.contentAttachmentFolder;
        }

        public String getContentImagesFolder()
        {
            return this.contentImagesFolder;
        }

        public String getContentFolder()
        {
            return this.contentFolder;
        }

        @Override
        public String toString()
        {
            return "ContentConfiguration [accessToken=" + this.accessToken + ", enableFileUpload=" + this.enableFileUpload + "]";
        }

    }

    public Optional<File> getContentAttachmentFolder()
    {
        return Optional.ofNullable(this.configuration.get())
                       .map(ContentConfiguration::getContentAttachmentFolder)
                       .map(File::new);
    }

    public Optional<File> getContentImagesFolder()
    {
        return Optional.ofNullable(this.configuration.get())
                       .map(ContentConfiguration::getContentImagesFolder)
                       .map(File::new);
    }

    public Optional<File> getContentFolder()
    {
        return Optional.ofNullable(this.configuration.get())
                       .map(ContentConfiguration::getContentFolder)
                       .map(File::new);
    }
}
