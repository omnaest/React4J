package org.omnaest.react4j.service.internal.upload;

import org.omnaest.react4j.component.form.upload.UploadChannel;
import org.omnaest.react4j.component.form.upload.UploadReceipt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileUploadServiceImpl implements FileUploadService
{
    @Autowired
    protected UploadChannelRegistry uploadChannelRegistry;

    @Override
    public UploadReceipt consume(String uploadId, MultipartFile file)
    {
        UploadChannel channel = this.uploadChannelRegistry.lookup(uploadId)
                                                          .orElseThrow(() -> new UnknownUploadIdException(uploadId));
        UploadReceipt receipt = channel.consume(new MultipartUploadContent(file));
        return receipt.toBuilder()
                      .uploadId(uploadId)
                      .build();
    }

}
