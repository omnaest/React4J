package org.omnaest.react4j.service.internal.controller.upload;

import org.omnaest.react4j.component.form.upload.UploadException;
import org.omnaest.react4j.component.form.upload.UploadReceipt;
import org.omnaest.react4j.service.internal.service.internal.translation.component.LocaleService;
import org.omnaest.react4j.service.internal.upload.FileUploadService;
import org.omnaest.react4j.service.internal.upload.UnknownUploadIdException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;

@RestController
public class FileUploadController
{
    private static Logger     LOG = LoggerFactory.getLogger(FileUploadController.class);

    @Autowired
    private FileUploadService fileUploadService;

    @Autowired
    private LocaleService     localeService;

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
