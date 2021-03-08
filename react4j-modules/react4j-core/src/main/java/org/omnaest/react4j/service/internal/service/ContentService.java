package org.omnaest.react4j.service.internal.service;

import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Provides access to the files of the 'content','content/attachments' and 'content/images' folder
 * 
 * @author omnaest
 */
public interface ContentService
{

    /**
     * Finds the image by its name, e.g. 'picture.jpg'
     * 
     * @param imageName
     * @return
     */
    public Optional<ContentImage> findImage(String imageName);

    public Stream<ContentImage> findImages(String imageNameRegEx);

    public static interface ContentImage extends Supplier<byte[]>
    {

        public String getImageName();

        public String getImagePath();
    }

    public Optional<ContentFile> findContentTextFile(String identifier);

    public Optional<ContentFile> findContentMarkdownFile(String identifier);

    public Optional<ContentFile> findContentCsvFile(String identifier);

    /**
     * Returns a file with the given name and file extension suffix. E.g. 'markdown.md' is addressed by 'markdown','md' as parameters.
     * 
     * @param identifier
     * @param suffix
     * @return
     */
    public Optional<ContentFile> findContentFile(String identifier, String suffix);

    public static interface ContentFile
    {
        public String asString();
    }

    public Optional<ContentAttachement> findAttachment(String attachmentName);

    public static interface ContentAttachement extends Supplier<byte[]>
    {
        public String getAttachmentName();

        public String getAttachmentPath();
    }

}
