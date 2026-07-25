package org.omnaest.react4j.service.internal.upload;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.omnaest.react4j.component.form.upload.ByteArrayChannel;
import org.omnaest.react4j.component.form.upload.UploadException;
import org.omnaest.react4j.component.form.upload.UploadReceipt;
import org.omnaest.react4j.domain.Location;
import org.springframework.mock.web.MockMultipartFile;

public class FileUploadServiceImplTest
{
    private UploadChannelRegistryImpl registry = new UploadChannelRegistryImpl();

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

}
