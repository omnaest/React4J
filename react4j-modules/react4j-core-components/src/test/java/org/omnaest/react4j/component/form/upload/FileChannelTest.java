package org.omnaest.react4j.component.form.upload;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class FileChannelTest
{

    @Test
    public void testToPathWritesExactBytesToDisk(@TempDir Path tempDir) throws IOException
    {
        Path destination = tempDir.resolve("result.bin");
        FileChannel channel = FileChannel.toPath(destination);
        byte[] payload = "streamed to disk".getBytes();

        UploadReceipt receipt = channel.consume(new StubUploadContent(payload, "client-name-is-ignored.bin", "application/octet-stream"));

        assertEquals(payload.length, receipt.getSize());
        assertTrue(Files.exists(destination));
        assertArrayEquals(payload, Files.readAllBytes(destination));
    }

    @Test
    public void testOversizeAbortsAndDeletesPartialFile(@TempDir Path tempDir) throws IOException
    {
        Path destination = tempDir.resolve("result.bin");
        FileChannel channel = FileChannel.toPath(destination)
                                         .withMaxSize(4);
        byte[] payload = "way too big for this channel".getBytes();

        assertThrows(UploadException.class, () -> channel.consume(new StubUploadContent(payload, "file.bin", "application/octet-stream")));

        assertFalse(Files.exists(destination));
    }

    @Test
    public void testIntoDirectorySanitizesFilename(@TempDir Path tempDir) throws IOException
    {
        FileChannel channel = FileChannel.intoDirectory(tempDir);
        byte[] payload = "some content".getBytes();

        channel.consume(new StubUploadContent(payload, "../../etc/passwd", "text/plain"));

        Path expected = tempDir.resolve("passwd");
        assertTrue(Files.exists(expected));
        assertArrayEquals(payload, Files.readAllBytes(expected));
        assertEquals(tempDir, expected.getParent());
    }

    @Test
    public void testIntoDirectoryRejectsTraversalEscapeAndStaysUnderDirectory(@TempDir Path tempDir) throws IOException
    {
        FileChannel channel = FileChannel.intoDirectory(tempDir);

        channel.consume(new StubUploadContent("x".getBytes(), "..", "text/plain"));

        // ".." alone sanitizes to the fallback name, so it must land as a plain file directly under tempDir
        try (var children = Files.list(tempDir))
        {
            assertTrue(children.allMatch(child -> child.getParent()
                                                       .equals(tempDir)));
        }
    }

    @Test
    public void testDefaultMaxSizeIsTenMegabytes(@TempDir Path tempDir)
    {
        assertEquals(10L * 1024 * 1024, FileChannel.toPath(tempDir.resolve("f"))
                                                   .maxSizeBytes());
    }

}
