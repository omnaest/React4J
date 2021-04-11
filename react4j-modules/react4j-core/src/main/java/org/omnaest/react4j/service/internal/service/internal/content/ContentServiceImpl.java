package org.omnaest.react4j.service.internal.service.internal.content;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;
import org.omnaest.react4j.service.internal.service.ContentService;
import org.omnaest.react4j.service.internal.service.ContentUploadService;
import org.omnaest.react4j.service.internal.service.internal.content.configuration.ContentConfigurationService;
import org.omnaest.utils.MatcherUtils;
import org.omnaest.utils.MatcherUtils.MatchFinder;
import org.omnaest.utils.element.bi.BiElement;
import org.omnaest.utils.exception.RuntimeIOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ContentServiceImpl implements ContentService, ContentUploadService
{
    private static final Logger LOG = LoggerFactory.getLogger(ContentServiceImpl.class);

    @Autowired
    private ContentConfigurationService contentConfigurationService;

    private Map<String, File> imageNameToFile       = new ConcurrentHashMap<>();
    private Map<String, File> attachmentNameToFile  = new ConcurrentHashMap<>();
    private Map<String, File> contentFileNameToFile = new ConcurrentHashMap<>();

    @Scheduled(initialDelay = 1000, fixedDelay = 5 * 1000)
    public void synchronizeFolderContent()
    {
        this.contentConfigurationService.getContentImagesFolder()
                                        .ifPresent(contentImagesFolder ->
                                        {
                                            org.omnaest.utils.FileUtils.listDirectoryFiles(contentImagesFolder)
                                                                       .filter(file -> !file.isDirectory())
                                                                       .forEach(file ->
                                                                       {
                                                                           this.imageNameToFile.put(file.getName(), file);
                                                                           LOG.trace("Synced image file: " + file);
                                                                       });
                                        });
        this.contentConfigurationService.getContentAttachmentFolder()
                                        .ifPresent(contentAttachmentFolder -> org.omnaest.utils.FileUtils.listDirectoryFiles(contentAttachmentFolder)
                                                                                                         .filter(file -> !file.isDirectory())
                                                                                                         .forEach(file ->
                                                                                                         {
                                                                                                             this.attachmentNameToFile.put(file.getName(),
                                                                                                                                           file);
                                                                                                             LOG.trace("Synced attachment file: " + file);
                                                                                                         }));

        this.contentConfigurationService.getContentFolder()
                                        .ifPresent(contentFolder ->
                                        {
                                            org.omnaest.utils.FileUtils.listDirectoryFiles(contentFolder)
                                                                       .filter(file -> !file.isDirectory())
                                                                       .forEach(file ->
                                                                       {
                                                                           this.contentFileNameToFile.put(file.getName(), file);
                                                                           LOG.trace("Synced content file: " + file);
                                                                       });
                                        });
    }

    @Override
    public Optional<ContentImage> findImage(String imageName)
    {
        return Optional.ofNullable(this.imageNameToFile.get(imageName))
                       .map(file -> new ContentImage()
                       {
                           @Override
                           public byte[] get()
                           {
                               try
                               {
                                   return FileUtils.readFileToByteArray(file);
                               }
                               catch (IOException e)
                               {
                                   throw new IllegalStateException("File not available anymore" + file);
                               }
                           }

                           @Override
                           public String getImageName()
                           {
                               return imageName;
                           }

                           @Override
                           public String getImagePath()
                           {
                               return "content/" + this.getImageName();
                           }
                       });
    }

    @Override
    public Stream<ContentImage> findImages(String imageNameRegEx)
    {
        MatchFinder matchFinder = MatcherUtils.matcher()
                                              .ofRegEx(imageNameRegEx);
        return this.imageNameToFile.keySet()
                                   .stream()
                                   .flatMap(imageName -> matchFinder.findInAnd(imageName)
                                                                    .stream())
                                   .map(match -> match.getMatchRegion())
                                   .map(imageName -> this.findImage(imageName)
                                                         .get());
    }

    @Override
    public Optional<ContentFile> findContentTextFile(String identifier)
    {
        return this.findContentFile(identifier, "txt");
    }

    @Override
    public Optional<ContentFile> findContentMarkdownFile(String identifier)
    {
        return this.findContentFile(identifier, "md");
    }

    @Override
    public Optional<ContentFile> findContentCsvFile(String identifier)
    {
        return this.findContentFile(identifier, "csv");
    }

    @Override
    public Optional<ContentFile> findContentFile(String identifier, String suffix)
    {
        return Optional.ofNullable(identifier)
                       .map(id -> id + "." + suffix)
                       .map(fileName -> this.contentFileNameToFile.get(fileName))
                       .map(file -> new ContentFile()
                       {
                           @Override
                           public String asString()
                           {
                               try
                               {
                                   return FileUtils.readFileToString(file, StandardCharsets.UTF_8);
                               }
                               catch (IOException e)
                               {
                                   throw new IllegalStateException("File not available anymore" + file);
                               }
                           }
                       });
    }

    @Override
    public Optional<ContentAttachement> findAttachment(String name)
    {
        return Optional.ofNullable(name)
                       .map(n -> BiElement.of(n, this.attachmentNameToFile.get(n)))
                       .filter(BiElement::hasNoNullValue)
                       .map(fileNameAndFile -> new ContentAttachement()
                       {

                           @Override
                           public byte[] get()
                           {
                               try
                               {
                                   return FileUtils.readFileToByteArray(fileNameAndFile.getSecond());
                               }
                               catch (IOException e)
                               {
                                   throw new IllegalStateException("File not available anymore" + fileNameAndFile.getFirst());
                               }
                           }

                           @Override
                           public String getAttachmentName()
                           {
                               return fileNameAndFile.getFirst();
                           }

                           @Override
                           public String getAttachmentPath()
                           {
                               return "content/" + this.getAttachmentName();
                           }

                       });
    }

    @Override
    public ValidatedStorageAccess validateToken(String token)
    {
        if (!this.contentConfigurationService.isFileUploadEnabled())
        {
            throw new IllegalArgumentException("File upload is disabled");
        }

        boolean isValidToken = this.contentConfigurationService.getAccessToken()
                                                               .map(expectedToken -> StringUtils.equals(token, expectedToken))
                                                               .orElse(false);

        return new ValidatedStorageAccess()
        {
            @Override
            public ValidatedStorageAccess ifValidConsume(MultipartFile file)
            {
                if (isValidToken && file != null)
                {
                    String fileName = this.validateAndDetermineEffectiveFileName(file);

                    try
                    {
                        byte[] data = file.getBytes();
                        ContentServiceImpl.this.contentConfigurationService.getContentAttachmentFolder()
                                                                           .ifPresent(contentAttachmentFolder ->
                                                                           {
                                                                               File targetFile = new File(contentAttachmentFolder, fileName);
                                                                               org.omnaest.utils.FileUtils.toByteArrayConsumer(targetFile)
                                                                                                          .accept(data);
                                                                           });

                    }
                    catch (IOException e)
                    {
                        throw new RuntimeIOException("Failed to consume file input stream", e);
                    }
                }
                return this;
            }

            private String validateAndDetermineEffectiveFileName(MultipartFile file)
            {
                return Optional.ofNullable(file.getOriginalFilename())
                               .map(name -> RegExUtils.replaceAll(name, "[^a-zA-Z0-9\\.]+", "_"))
                               .map(name -> RegExUtils.replaceAll(name, "[\\.]+", "\\."))
                               .orElseThrow(() -> new IllegalArgumentException("Invalid upload file name"));
            }

            @Override
            public ValidatedStorageAccess ifInvalidThrow(Supplier<RuntimeException> exceptionProvider)
            {
                if (!isValidToken)
                {
                    throw exceptionProvider.get();
                }
                return this;
            }
        };
    }

}
