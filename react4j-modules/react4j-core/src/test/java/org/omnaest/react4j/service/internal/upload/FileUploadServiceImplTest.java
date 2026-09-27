package org.omnaest.react4j.service.internal.upload;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;

import org.junit.jupiter.api.Test;
import org.omnaest.react4j.component.form.upload.ByteArrayChannel;
import org.omnaest.react4j.component.form.upload.UploadContent;
import org.omnaest.react4j.component.form.upload.UploadException;
import org.omnaest.react4j.component.form.upload.UploadReceipt;
import org.omnaest.react4j.domain.Location;
import org.springframework.mock.web.MockMultipartFile;

public class FileUploadServiceImplTest
{
    private UploadChannelRegistryImpl registry = new UploadChannelRegistryImpl(Clock.systemUTC(), 30, 1000);

    private FileUploadServiceImpl createService()
    {
        UploadChannelRegistryImpl registryReference = this.registry;
        return new FileUploadServiceImpl() {
            {
                this.uploadChannelRegistry = registryReference;
            }
        };
    }

    @Test
    public void testValidUploadIdDeliversBytesToChannelAndReturnsCorrectReceipt() throws Exception
    {
        ByteArrayChannel channel = ByteArrayChannel.create();
        String uploadId = this.registry.register(Location.of("fileUpload"), channel);
        FileUploadServiceImpl service = this.createService();

        byte[] payload = "hello from multipart".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "greeting.txt", "text/plain", payload);

        UploadReceipt receipt = service.consume(uploadId, file);

        assertEquals(uploadId, receipt.getUploadId());
        assertEquals("greeting.txt", receipt.getFilename());
        assertEquals("text/plain", receipt.getContentType());
        assertEquals(payload.length, receipt.getSize());
        assertArrayEquals(payload, channel.getContent()
                                          .get()
                                          .asBytes());
    }

    @Test
    public void testUnknownUploadIdThrows()
    {
        FileUploadServiceImpl service = this.createService();
        MockMultipartFile file = new MockMultipartFile("file", "x.txt", "text/plain", "x".getBytes());

        assertThrows(UnknownUploadIdException.class, () -> service.consume("does-not-exist", file));
    }

    @Test
    public void testOversizeUploadPropagatesUploadException()
    {
        ByteArrayChannel channel = ByteArrayChannel.create()
                                                   .withMaxSize(2);
        String uploadId = this.registry.register(Location.of("fileUpload"), channel);
        FileUploadServiceImpl service = this.createService();

        MockMultipartFile file = new MockMultipartFile("file", "big.txt", "text/plain", "way too big".getBytes());

        UploadException exception = assertThrows(UploadException.class, () -> service.consume(uploadId, file));
        assertEquals(UploadException.Reason.SIZE_EXCEEDED, exception.getReason());
    }

    @Test
    public void testDisallowedContentTypePropagatesUploadException()
    {
        ByteArrayChannel channel = ByteArrayChannel.create()
                                                   .withAcceptedContentTypes(java.util.Set.of("image/png"));
        String uploadId = this.registry.register(Location.of("fileUpload"), channel);
        FileUploadServiceImpl service = this.createService();

        MockMultipartFile file = new MockMultipartFile("file", "doc.txt", "text/plain", "data".getBytes());

        UploadException exception = assertThrows(UploadException.class, () -> service.consume(uploadId, file));
        assertEquals(UploadException.Reason.CONTENT_TYPE_REJECTED, exception.getReason());
    }

    @Test
    public void testConsumeUploadContentOverloadDeliversBytesAndStampsUploadId() throws Exception
    {
        ByteArrayChannel channel = ByteArrayChannel.create();
        String uploadId = this.registry.register(Location.of("fileUpload"), channel);
        FileUploadServiceImpl service = this.createService();

        byte[] payload = "hello from a non-multipart transport".getBytes();
        UploadContent content = new StubUploadContent(payload, "greeting.txt", "text/plain");

        UploadReceipt receipt = service.consume(uploadId, content);

        assertEquals(uploadId, receipt.getUploadId());
        assertEquals("greeting.txt", receipt.getFilename());
        assertEquals("text/plain", receipt.getContentType());
        assertEquals(payload.length, receipt.getSize());
        assertArrayEquals(payload, channel.getContent()
                                          .get()
                                          .asBytes());
    }

    @Test
    public void testConsumeUploadContentOverloadUnknownUploadIdThrows()
    {
        FileUploadServiceImpl service = this.createService();
        UploadContent content = new StubUploadContent("x".getBytes(), "x.txt", "text/plain");

        assertThrows(UnknownUploadIdException.class, () -> service.consume("does-not-exist", content));
    }

    private static final class StubUploadContent implements UploadContent
    {
        private final byte[] bytes;
        private final String filename;
        private final String contentType;

        private StubUploadContent(byte[] bytes, String filename, String contentType)
        {
            super();
            this.bytes = bytes;
            this.filename = filename;
            this.contentType = contentType;
        }

        @Override
        public String filename()
        {
            return this.filename;
        }

        @Override
        public String contentType()
        {
            return this.contentType;
        }

        @Override
        public long size()
        {
            return this.bytes.length;
        }

        @Override
        public InputStream inputStream() throws IOException
        {
            return new ByteArrayInputStream(this.bytes);
        }
    }

}
