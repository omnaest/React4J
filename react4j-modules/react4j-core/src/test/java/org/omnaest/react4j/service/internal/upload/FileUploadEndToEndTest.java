package org.omnaest.react4j.service.internal.upload;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.omnaest.react4j.EnableReactUI;
import org.omnaest.react4j.component.form.upload.ByteArrayChannel;
import org.omnaest.react4j.component.form.upload.FileChannel;
import org.omnaest.react4j.component.form.upload.UploadChannel;
import org.omnaest.react4j.data.annotations.EnableReactUIInMemoryRepository;
import org.omnaest.react4j.service.ReactUIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Proves the file-upload feature end-to-end through the real Spring MVC stack: render a {@code Form} containing a
 * {@code FileUploadFormElement} via {@code GET /ui}, extract the server-issued {@code uploadId} from the rendered node tree, then
 * {@code POST /ui/upload} a multipart payload and verify the exact bytes reach the channel - for both the in-memory {@link ByteArrayChannel} and the
 * disk-streaming {@link FileChannel}.
 */
@SpringBootTest(classes = FileUploadEndToEndTest.TestApplication.class, webEnvironment = WebEnvironment.MOCK)
@AutoConfigureMockMvc
public class FileUploadEndToEndTest
{
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private MockMvc                   mockMvc;

    @Autowired
    private ReactUIService            reactUIService;

    @SpringBootApplication
    @EnableReactUI
    @EnableReactUIInMemoryRepository
    public static class TestApplication
    {
    }

    @Test
    public void testByteArrayChannelReceivesExactBytesEndToEnd() throws Exception
    {
        ByteArrayChannel channel = ByteArrayChannel.create();
        this.registerFormWithChannel(channel);

        String uploadId = this.extractUploadId(this.renderUI());

        byte[] payload = "hello from the browser".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "greeting.txt", "text/plain", payload);

        this.mockMvc.perform(multipart("/ui/upload").file(file)
                                                    .param("uploadId", uploadId))
                    .andExpect(status().isOk());

        assertTrue(channel.getContent()
                          .isPresent());
        assertArrayEquals(payload, channel.getContent()
                                          .get()
                                          .asBytes());
    }

    @Test
    public void testFileChannelStreamsExactBytesToDiskEndToEnd(@TempDir Path tempDir) throws Exception
    {
        Path destination = tempDir.resolve("upload.bin");
        FileChannel channel = FileChannel.toPath(destination);
        this.registerFormWithChannel(channel);

        String uploadId = this.extractUploadId(this.renderUI());

        byte[] payload = "streamed straight to disk".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "data.bin", "application/octet-stream", payload);

        this.mockMvc.perform(multipart("/ui/upload").file(file)
                                                    .param("uploadId", uploadId))
                    .andExpect(status().isOk());

        assertTrue(Files.exists(destination));
        assertArrayEquals(payload, Files.readAllBytes(destination));
    }

    @Test
    public void testOversizeUploadIsRejectedAndChannelStaysEmpty() throws Exception
    {
        ByteArrayChannel channel = ByteArrayChannel.create()
                                                   .withMaxSize(4);
        this.registerFormWithChannel(channel);

        String uploadId = this.extractUploadId(this.renderUI());

        MockMultipartFile file = new MockMultipartFile("file", "big.txt", "text/plain", "way too big for this".getBytes());

        this.mockMvc.perform(multipart("/ui/upload").file(file)
                                                    .param("uploadId", uploadId))
                    .andExpect(status().isPayloadTooLarge());

        assertFalse(channel.getContent()
                           .isPresent());
    }

    @Test
    public void testUnknownUploadIdReturns404() throws Exception
    {
        MockMultipartFile file = new MockMultipartFile("file", "x.txt", "text/plain", "x".getBytes());

        this.mockMvc.perform(multipart("/ui/upload").file(file)
                                                    .param("uploadId", "does-not-exist-" + System.nanoTime()))
                    .andExpect(status().isNotFound());
    }

    /**
     * Registers a fresh default-root {@code Form} carrying a single {@code FileUploadFormElement} bound to {@code channel}. The form is
     * attached to its {@link org.omnaest.react4j.domain.context.document.Document} via {@code withUIContext} - required so
     * {@code FormRendererImpl.getEffectiveContext()} (which has no fallback, unlike the component-level {@code FormImpl}) does not NPE at
     * render time.
     */
    private void registerFormWithChannel(UploadChannel channel)
    {
        this.reactUIService.createDefaultRoot(reactUI -> reactUI.addNewComponent(factory -> factory.newForm()
                                                                                                   .withUIContext((form, context) ->
                                                                                                   {
                                                                                                       form.attachTo(context.getFirstDocument());
                                                                                                       form.addFileUpload(fileUpload -> fileUpload.withUploadChannel(channel));
                                                                                                   })));
    }

    private String renderUI() throws Exception
    {
        return this.mockMvc.perform(get("/ui"))
                           .andExpect(status().isOk())
                           .andReturn()
                           .getResponse()
                           .getContentAsString();
    }

    private String extractUploadId(String json) throws Exception
    {
        JsonNode root = OBJECT_MAPPER.readTree(json);
        JsonNode fileUploadNode = findFileUploadNode(root);
        assertNotNull(fileUploadNode, "Expected to find a fileUpload node in the rendered UI JSON: " + json);
        return fileUploadNode.get("uploadId")
                             .asText();
    }

    private static JsonNode findFileUploadNode(JsonNode node)
    {
        if (node == null)
        {
            return null;
        }
        if (node.isObject())
        {
            if (node.has("fileUpload") && node.get("fileUpload")
                                              .isObject())
            {
                return node.get("fileUpload");
            }
            Iterator<String> fieldNames = node.fieldNames();
            while (fieldNames.hasNext())
            {
                JsonNode result = findFileUploadNode(node.get(fieldNames.next()));
                if (result != null)
                {
                    return result;
                }
            }
        }
        else if (node.isArray())
        {
            for (JsonNode child : node)
            {
                JsonNode result = findFileUploadNode(child);
                if (result != null)
                {
                    return result;
                }
            }
        }
        return null;
    }

}
