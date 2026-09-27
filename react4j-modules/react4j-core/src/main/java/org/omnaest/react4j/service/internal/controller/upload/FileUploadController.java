package org.omnaest.react4j.service.internal.controller.upload;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import org.omnaest.react4j.component.form.upload.UploadException;
import org.omnaest.react4j.component.form.upload.UploadReceipt;
import org.omnaest.react4j.component.form.upload.internal.BoundedInputStream;
import org.omnaest.react4j.service.internal.service.internal.translation.component.LocaleService;
import org.omnaest.react4j.service.internal.upload.FileUploadService;
import org.omnaest.react4j.service.internal.upload.UnknownUploadIdException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.unit.DataSize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Two upload transports, deliberately kept side by side rather than unified, because they differ in exactly the property that matters for W1:
 * <p>
 * <b>{@code /ui/upload} (multipart, the default)</b> - {@link #uploadFile(MultipartFile, String, String)}. Spring's {@code StandardServletMultipartResolver}
 * detects the {@code multipart/*} content type and hands the request to the servlet container's multipart parser <em>before this class ever runs</em>.
 * Tomcat's default {@code fileSizeThreshold} is {@code 0}, so <b>every part of every multipart upload - including a plain form field - is written to a
 * plaintext temporary file on disk before {@code UploadChannel.consume(...)} is invoked</b>, and deleted only at the end of the request. This is the
 * framework's long-standing, unavoidable-on-this-transport behaviour; nothing on this class changes it.
 * <p>
 * <b>{@code /ui/upload/raw} (opt-in, unbuffered)</b> - {@link #uploadFileRaw(HttpServletRequest, String)}. A request whose {@code Content-Type} is not
 * {@code multipart/*} is never wrapped by the multipart resolver at all, so nothing parses or spills it: this method reads
 * {@link HttpServletRequest#getInputStream()} directly and never calls {@code getPart}/{@code getParts}/{@code getParameter} or anything else that would
 * trigger container-side multipart parsing. Measured (see the React4J CLAUDE.md for this module): zero temporary files and a flat, size-independent peak
 * heap at 500 MB under a 256 MB heap cap. An element opts in via {@code Form.FileUploadFormElement#withUnbufferedTransport()}; an element that does not is
 * unaffected - same multipart transport, same behaviour, same defaults, as today.
 * <p>
 * The raw transport carries its metadata as headers rather than as multipart fields, since it has no form fields to carry them in: {@code X-Upload-Id} (a
 * header, deliberately never a query parameter, so it does not appear in a request line an access log would record) and {@code X-Filename} (the client
 * filename, percent-encoded per {@link URLDecoder}/UTF-8 - a raw header value is not UTF-8 by default, so an un-encoded non-ASCII filename would be
 * mangled). The declared content type is carried on the request's own {@code Content-Type} header - any value that does not start with {@code multipart/}
 * is safe on this transport by construction, so no separate header is needed for it.
 * <p>
 * Because the servlet container's {@link jakarta.servlet.MultipartConfigElement} (see {@code FileUploadConfiguration}) enforces the coarse global upload
 * ceiling only for the multipart transport, this class enforces an equivalent ceiling itself on the raw transport, from the exact same
 * {@code react4j.upload.max-file-size} / {@code react4j.upload.max-request-size} configuration the multipart path uses, and maps a violation to the same
 * {@code 413}.
 *
 * @author omnaest
 */
@RestController
public class FileUploadController
{
    private static final String UPLOAD_ID_HEADER = "X-Upload-Id";
    private static final String FILENAME_HEADER  = "X-Filename";

    private static Logger       LOG              = LoggerFactory.getLogger(FileUploadController.class);

    @Autowired
    private FileUploadService   fileUploadService;

    @Autowired
    private LocaleService       localeService;

    @Value("${react4j.upload.max-file-size:25MB}")
    private String              rawTransportMaxFileSize;

    @Value("${react4j.upload.max-request-size:25MB}")
    private String              rawTransportMaxRequestSize;

    @PostMapping(path = {"/ui/upload", "{languageTag}/ui/upload"})
    public ResponseEntity<UploadReceipt> uploadFile(@RequestParam("file") MultipartFile file, @RequestParam("uploadId") String uploadId, @PathVariable(name = "languageTag", required = false) String languageTag)
    {
        this.localeService.setExplicitRequestLocaleByLanguageTag(languageTag);
        try
        {
            UploadReceipt receipt = this.fileUploadService.consume(uploadId, file);
            return ResponseEntity.ok(receipt);
        }
        catch (UnknownUploadIdException e)
        {
            return ResponseEntity.notFound()
                                 .build();
        }
        catch (UploadException e)
        {
            return ResponseEntity.status(this.toHttpStatus(e))
                                 .build();
        }
    }

    /**
     * The unbuffered transport. See the class javadoc for the mechanism and the header shape.
     *
     * @param request
     * @param languageTag
     * @return
     */
    @PostMapping(path = {"/ui/upload/raw", "{languageTag}/ui/upload/raw"})
    public ResponseEntity<UploadReceipt> uploadFileRaw(HttpServletRequest request, @PathVariable(name = "languageTag", required = false) String languageTag)
    {
        this.localeService.setExplicitRequestLocaleByLanguageTag(languageTag);

        String uploadId = request.getHeader(UPLOAD_ID_HEADER);
        if (uploadId == null || uploadId.isBlank())
        {
            return ResponseEntity.badRequest()
                                 .build();
        }

        long ceilingBytes = this.rawTransportCeilingBytes();
        long declaredLength = request.getContentLengthLong();
        if (declaredLength >= 0 && declaredLength > ceilingBytes)
        {
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                                 .build();
        }

        try
        {
            String filename = this.decodeFilenameHeader(request.getHeader(FILENAME_HEADER));
            InputStream boundedRequestBody = new BoundedInputStream(request.getInputStream(), ceilingBytes);
            RawBodyUploadContent content = new RawBodyUploadContent(request, boundedRequestBody, filename);

            UploadReceipt receipt = this.fileUploadService.consume(uploadId, content);
            return ResponseEntity.ok(receipt);
        }
        catch (UnknownUploadIdException e)
        {
            return ResponseEntity.notFound()
                                 .build();
        }
        catch (UploadException e)
        {
            return ResponseEntity.status(this.toHttpStatus(e))
                                 .build();
        }
        catch (IOException e)
        {
            return ResponseEntity.badRequest()
                                 .build();
        }
    }

    /**
     * The global ceiling for the raw transport, read from the same {@code react4j.upload.max-file-size} / {@code react4j.upload.max-request-size}
     * configuration the multipart transport's {@link jakarta.servlet.MultipartConfigElement} is built from - see {@code FileUploadConfiguration}.
     *
     * @return
     */
    private long rawTransportCeilingBytes()
    {
        long maxFileSizeBytes = DataSize.parse(this.rawTransportMaxFileSize)
                                        .toBytes();
        long maxRequestSizeBytes = DataSize.parse(this.rawTransportMaxRequestSize)
                                           .toBytes();
        return Math.min(maxFileSizeBytes, maxRequestSizeBytes);
    }

    /**
     * Decodes a percent-encoded (UTF-8) filename header value so a non-ASCII client filename round-trips byte-exactly. A raw HTTP header value is not UTF-8
     * by default, so the client is expected to percent-encode it; a header this method cannot decode is passed through unchanged rather than failing the
     * whole upload over a filename.
     *
     * @param encodedFilename
     * @return
     */
    private String decodeFilenameHeader(String encodedFilename)
    {
        if (encodedFilename == null)
        {
            return null;
        }
        try
        {
            return URLDecoder.decode(encodedFilename, StandardCharsets.UTF_8);
        }
        catch (IllegalArgumentException e)
        {
            return encodedFilename;
        }
    }

    private HttpStatus toHttpStatus(UploadException e)
    {
        switch (e.getReason())
        {
            case SIZE_EXCEEDED :
                return HttpStatus.PAYLOAD_TOO_LARGE;
            case CONTENT_TYPE_REJECTED :
                return HttpStatus.UNSUPPORTED_MEDIA_TYPE;
            default :
                return HttpStatus.BAD_REQUEST;
        }
    }

    @PostConstruct
    public void postInit()
    {
        LOG.info(this.getClass()
                     .getSimpleName()
                 + " enabled.");
    }

}
