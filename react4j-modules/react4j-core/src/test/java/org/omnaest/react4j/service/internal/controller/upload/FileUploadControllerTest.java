package org.omnaest.react4j.service.internal.controller.upload;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.omnaest.react4j.component.form.upload.ByteArrayChannel;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.service.internal.service.internal.translation.component.LocaleService;
import org.omnaest.react4j.service.internal.upload.FileUploadServiceImpl;
import org.omnaest.react4j.service.internal.upload.UploadChannelRegistryImpl;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

public class FileUploadControllerTest
{
    private UploadChannelRegistryImpl registry;
    private MockMvc                   mockMvc;

    @BeforeEach
    public void setUp()
    {
        this.registry = new UploadChannelRegistryImpl();

        FileUploadServiceImpl fileUploadService = new FileUploadServiceImpl();
        ReflectionTestUtils.setField(fileUploadService, "uploadChannelRegistry", this.registry);

        FileUploadController controller = new FileUploadController();
        ReflectionTestUtils.setField(controller, "fileUploadService", fileUploadService);
        ReflectionTestUtils.setField(controller, "localeService", new LocaleService());

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

}
