package org.omnaest.react4j.component.form.upload;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.Set;

import org.omnaest.react4j.component.form.upload.internal.AbstractUploadChannel;
import org.omnaest.react4j.component.form.upload.internal.SafeFilename;

/**
 * {@link UploadChannel} that streams an upload directly to a server-controlled filesystem path (not buffered in memory first).
 * <p>
 * The destination is always fixed server-side: either a single {@link #toPath(Path) fixed path} chosen by the application, or a
 * {@link #intoDirectory(Path) directory} into which the (sanitized) client filename is placed. The client's multipart request never carries a path - only
 * the opaque {@code uploadId} and the bytes - so a client cannot dictate an arbitrary filesystem destination.
 * <p>
 * Instances are meant to be created once and reused across renders - see the usage constraint documented on {@link UploadChannel}.
 *
 * @author omnaest
 */
public class FileChannel extends AbstractUploadChannel
{
    private final Path fixedDestination;
    private final Path directory;

    public FileChannel(Path fixedDestination)
    {
        super();
        this.fixedDestination = Objects.requireNonNull(fixedDestination, "fixedDestination must not be null");
        this.directory = null;
    }

    private FileChannel(Path directory, boolean directoryMode)
    {
        super();
        this.fixedDestination = null;
        this.directory = Objects.requireNonNull(directory, "directory must not be null");
    }

    public static FileChannel toPath(Path fixedDestination)
    {
        return new FileChannel(fixedDestination);
    }

    public static FileChannel intoDirectory(Path directory)
    {
        return new FileChannel(directory, true);
    }

    public FileChannel withMaxSize(long maxSizeBytes)
    {
        this.setMaxSizeBytes(maxSizeBytes);
        return this;
    }

    public FileChannel withAcceptedContentTypes(Set<String> acceptedContentTypes)
    {
        this.setAcceptedContentTypes(acceptedContentTypes);
        return this;
    }

    @Override
    protected long writeTo(InputStream boundedStream, UploadContent content) throws IOException
    {
        Path destination = this.resolveDestination(content);
        try
        {
            if (destination.getParent() != null)
            {
                Files.createDirectories(destination.getParent());
            }
            try (OutputStream out = Files.newOutputStream(destination, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))
            {
                return boundedStream.transferTo(out);
            }
        }
        catch (IOException e)
        {
            Files.deleteIfExists(destination);
            throw e;
        }
    }

    private Path resolveDestination(UploadContent content)
    {
        if (this.fixedDestination != null)
        {
            return this.fixedDestination;
        }
        try
        {
            return SafeFilename.resolveWithinDirectory(this.directory, content.filename());
        }
        catch (IllegalArgumentException e)
        {
            throw new UploadException(UploadException.Reason.IO_ERROR, e.getMessage(), e);
        }
    }

}
