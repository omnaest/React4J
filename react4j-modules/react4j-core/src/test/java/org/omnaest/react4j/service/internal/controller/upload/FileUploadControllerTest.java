package org.omnaest.react4j.service.internal.controller.upload;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.omnaest.react4j.component.form.upload.ByteArrayChannel;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.service.internal.service.internal.translation.component.LocaleService;
import org.omnaest.react4j.service.internal.upload.FileUploadServiceImpl;
import org.omnaest.react4j.service.internal.upload.MutableClock;
import org.omnaest.react4j.service.internal.upload.UploadChannelRegistryImpl;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

public class FileUploadControllerTest
{
    private UploadChannelRegistryImpl registry;
    private MutableClock              clock;
    private MockMvc                   mockMvc;

    @BeforeEach
    public void setUp()
    {
        this.clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
        this.registry = new UploadChannelRegistryImpl(this.clock, 30, 1000);

        FileUploadServiceImpl fileUploadService = new FileUploadServiceImpl();
        ReflectionTestUtils.setField(fileUploadService, "uploadChannelRegistry", this.registry);

        FileUploadController controller = new FileUploadController();
        ReflectionTestUtils.setField(controller, "fileUploadService", fileUploadService);
        ReflectionTestUtils.setField(controller, "localeService", new LocaleService());
        ReflectionTestUtils.setField(controller, "rawTransportMaxFileSize", "25MB");
        ReflectionTestUtils.setField(controller, "rawTransportMaxRequestSize", "25MB");

        this.mockMvc = MockMvcBuilders.standaloneSetup(controller)
                                      .build();
    }

    @Test
    public void testMultipartUploadReturns200AndReceiptAndDeliversBytesToChannel() throws Exception
    {
        ByteArrayChannel channel = ByteArrayChannel.create();
        String uploadId = this.registry.register(Location.of("fileUpload"), channel);

        MockMultipartFile file = new MockMultipartFile("file", "picture.png", "image/png", "PNGDATA".getBytes());

        this.mockMvc.perform(multipart("/ui/upload").file(file)
                                                    .param("uploadId", uploadId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.uploadId", is(uploadId)))
                    .andExpect(jsonPath("$.filename", is("picture.png")))
                    .andExpect(jsonPath("$.contentType", is("image/png")));

        org.junit.jupiter.api.Assertions.assertArrayEquals("PNGDATA".getBytes(), channel.getContent()
                                                                                        .get()
                                                                                        .asBytes());
    }

    @Test
    public void testUnknownUploadIdReturns404() throws Exception
    {
        MockMultipartFile file = new MockMultipartFile("file", "x.txt", "text/plain", "x".getBytes());

        this.mockMvc.perform(multipart("/ui/upload").file(file)
                                                    .param("uploadId", "does-not-exist"))
                    .andExpect(status().isNotFound());
    }

    @Test
    public void testOversizeReturns413() throws Exception
    {
        ByteArrayChannel channel = ByteArrayChannel.create()
                                                   .withMaxSize(2);
        String uploadId = this.registry.register(Location.of("fileUpload"), channel);

        MockMultipartFile file = new MockMultipartFile("file", "big.txt", "text/plain", "way too big".getBytes());

        this.mockMvc.perform(multipart("/ui/upload").file(file)
                                                    .param("uploadId", uploadId))
                    .andExpect(status().isPayloadTooLarge());
    }

    @Test
    public void testMissingFileReturns400() throws Exception
    {
        this.mockMvc.perform(multipart("/ui/upload").param("uploadId", "any"))
                    .andExpect(status().isBadRequest());
    }

    @Test
    public void testRawUploadReturns200AndReceiptAndDeliversBytesToChannel() throws Exception
    {
        ByteArrayChannel channel = ByteArrayChannel.create();
        String uploadId = this.registry.register(Location.of("fileUpload"), channel);

        byte[] payload = "PNGDATA".getBytes(StandardCharsets.UTF_8);

        this.mockMvc.perform(post("/ui/upload/raw").content(payload)
                                                   .contentType(MediaType.IMAGE_PNG)
                                                   .header("X-Upload-Id", uploadId)
                                                   .header("X-Filename", URLEncoder.encode("picture.png", StandardCharsets.UTF_8)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.uploadId", is(uploadId)))
                    .andExpect(jsonPath("$.filename", is("picture.png")))
                    .andExpect(jsonPath("$.contentType", is("image/png")));

        assertArrayEquals(payload, channel.getContent()
                                          .get()
                                          .asBytes());
    }

    @Test
    public void testRawUploadMissingUploadIdHeaderReturns400() throws Exception
    {
        this.mockMvc.perform(post("/ui/upload/raw").content("x".getBytes(StandardCharsets.UTF_8))
                                                   .contentType(MediaType.TEXT_PLAIN))
                    .andExpect(status().isBadRequest());
    }

    @Test
    public void testRawUploadUnknownUploadIdReturns404() throws Exception
    {
        this.mockMvc.perform(post("/ui/upload/raw").content("x".getBytes(StandardCharsets.UTF_8))
                                                   .contentType(MediaType.TEXT_PLAIN)
                                                   .header("X-Upload-Id", "does-not-exist"))
                    .andExpect(status().isNotFound());
    }

    @Test
    public void testRawUploadOversizeReturns413AndChannelStaysEmpty() throws Exception
    {
        ByteArrayChannel channel = ByteArrayChannel.create()
                                                   .withMaxSize(2);
        String uploadId = this.registry.register(Location.of("fileUpload"), channel);

        this.mockMvc.perform(post("/ui/upload/raw").content("way too big".getBytes(StandardCharsets.UTF_8))
                                                   .contentType(MediaType.TEXT_PLAIN)
                                                   .header("X-Upload-Id", uploadId))
                    .andExpect(status().isPayloadTooLarge());

        assertFalse(channel.getContent()
                           .isPresent());
    }

    @Test
    public void testRawUploadDisallowedContentTypeReturns415() throws Exception
    {
        ByteArrayChannel channel = ByteArrayChannel.create()
                                                   .withAcceptedContentTypes(java.util.Set.of("image/png"));
        String uploadId = this.registry.register(Location.of("fileUpload"), channel);

        this.mockMvc.perform(post("/ui/upload/raw").content("data".getBytes(StandardCharsets.UTF_8))
                                                   .contentType(MediaType.TEXT_PLAIN)
                                                   .header("X-Upload-Id", uploadId))
                    .andExpect(status().isUnsupportedMediaType());

        assertFalse(channel.getContent()
                           .isPresent());
    }

    @Test
    public void testEvictedUploadIdReturns404() throws Exception
    {
        ByteArrayChannel channel = ByteArrayChannel.create();
        String uploadId = this.registry.register(Location.of("fileUpload"), channel);

        // Advance past the registry's time-to-live with no intervening register/lookup - the entry is swept, not merely hidden.
        this.clock.advanceBy(Duration.ofMinutes(31));

        MockMultipartFile file = new MockMultipartFile("file", "picture.png", "image/png", "PNGDATA".getBytes());

        this.mockMvc.perform(multipart("/ui/upload").file(file)
                                                    .param("uploadId", uploadId))
                    .andExpect(status().isNotFound());
    }

    @Test
    public void testRawUploadNonAsciiFilenameRoundTripsByteExactly() throws Exception
    {
        ByteArrayChannel channel = ByteArrayChannel.create();
        String uploadId = this.registry.register(Location.of("fileUpload"), channel);
        // Non-ASCII filename built from explicit code points (never a literal glyph) to keep this source file pure ASCII: accented Latin (e9) + CJK (65e5,
        // 672c, 8a9e).
        String filename = "r" + (char) 0x00e9 + "sum" + (char) 0x00e9 + "-" + (char) 0x65e5 + (char) 0x672c + (char) 0x8a9e + ".txt";

        this.mockMvc.perform(post("/ui/upload/raw").content("data".getBytes(StandardCharsets.UTF_8))
                                                   .contentType(MediaType.TEXT_PLAIN)
                                                   .header("X-Upload-Id", uploadId)
                                                   .header("X-Filename", URLEncoder.encode(filename, StandardCharsets.UTF_8)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.filename", is(filename)));
    }

}
