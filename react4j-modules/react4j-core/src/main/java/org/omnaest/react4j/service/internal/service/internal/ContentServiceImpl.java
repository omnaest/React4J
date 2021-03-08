package org.omnaest.react4j.service.internal.service.internal;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.omnaest.react4j.service.internal.service.ContentService;
import org.omnaest.utils.MatcherUtils;
import org.omnaest.utils.MatcherUtils.MatchFinder;
import org.omnaest.utils.element.bi.BiElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class ContentServiceImpl implements ContentService
{
    private static final Logger LOG                   = LoggerFactory.getLogger(ContentServiceImpl.class);
    private Map<String, File>   imageNameToFile       = new ConcurrentHashMap<>();
    private Map<String, File>   attachmentNameToFile  = new ConcurrentHashMap<>();
    private Map<String, File>   contentFileNameToFile = new ConcurrentHashMap<>();

    @Scheduled(initialDelay = 1000, fixedDelay = 5 * 1000)
    public void synchronizeFolderContent()
    {
        org.omnaest.utils.FileUtils.listDirectoryFiles(new File("content/images"))
                                   .filter(file -> !file.isDirectory())
                                   .forEach(file ->
                                   {
                                       this.imageNameToFile.put(file.getName(), file);
                                       LOG.trace("Synced image file: " + file);
                                   });
        org.omnaest.utils.FileUtils.listDirectoryFiles(new File("content/attachments"))
                                   .filter(file -> !file.isDirectory())
                                   .forEach(file ->
                                   {
                                       this.attachmentNameToFile.put(file.getName(), file);
                                       LOG.trace("Synced attachment file: " + file);
                                   });
        org.omnaest.utils.FileUtils.listDirectoryFiles(new File("content"))
                                   .filter(file -> !file.isDirectory())
                                   .forEach(file ->
                                   {
                                       this.contentFileNameToFile.put(file.getName(), file);
                                       LOG.trace("Synced content file: " + file);
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

}
