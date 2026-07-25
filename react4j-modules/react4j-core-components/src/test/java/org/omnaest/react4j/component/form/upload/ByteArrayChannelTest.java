package org.omnaest.react4j.component.form.upload;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

public class ByteArrayChannelTest
{

    @Test
    public void testConsumeStoresExactBytesAndReceiptFields()
    {
        ByteArrayChannel channel = ByteArrayChannel.create();
        byte[] payload = "hello upload".getBytes();

        UploadReceipt receipt = channel.consume(new StubUploadContent(payload, "greeting.txt", "text/plain"));

        assertEquals("greeting.txt", receipt.getFilename());
        assertEquals("text/plain", receipt.getContentType());
        assertEquals(payload.length, receipt.getSize());

        assertTrue(channel.getContent()
                          .isPresent());
        assertArrayEquals(payload, channel.getContent()
                                          .get()
                                          .asBytes());
        assertEquals("greeting.txt", channel.getContent()
                                            .get()
                                            .filename());
    }

    @Test
    public void testOversizeRejectedWithNoPartialState()
    {
        ByteArrayChannel channel = ByteArrayChannel.create()
                                                   .withMaxSize(4);
        byte[] payload = "way too big".getBytes();

        assertThrows(UploadException.class, () -> channel.consume(new StubUploadContent(payload, "big.txt", "text/plain")));

        assertFalse(channel.getContent()
                           .isPresent());
    }

    @Test
    public void testDisallowedContentTypeRejected()
    {
        ByteArrayChannel channel = ByteArrayChannel.create()
                                                   .withAcceptedContentTypes(Set.of("image/png"));

        UploadException exception = assertThrows(UploadException.class,
                                                 () -> channel.consume(new StubUploadContent("data".getBytes(), "file.txt", "text/plain")));

        assertEquals(UploadException.Reason.CONTENT_TYPE_REJECTED, exception.getReason());
        assertFalse(channel.getContent()
                           .isPresent());
    }

    @Test
    public void testAcceptedContentTypeIsConsumed()
    {
        ByteArrayChannel channel = ByteArrayChannel.create()
                                                   .withAcceptedContentTypes(Set.of("image/png"));

        channel.consume(new StubUploadContent("PNGDATA".getBytes(), "picture.png", "image/png"));

        assertTrue(channel.getContent()
                          .isPresent());
    }

    @Test
    public void testDefaultMaxSizeIsTenMegabytes()
    {
        assertEquals(10L * 1024 * 1024, ByteArrayChannel.create()
                                                        .maxSizeBytes());
    }

    @Test
    public void testEmptyAcceptedContentTypesMeansAcceptAll()
    {
        assertTrue(ByteArrayChannel.create()
                                   .acceptedContentTypes()
                                   .isEmpty());
    }

}
