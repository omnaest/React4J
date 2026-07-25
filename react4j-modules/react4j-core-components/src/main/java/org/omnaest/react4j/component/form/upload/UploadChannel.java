package org.omnaest.react4j.component.form.upload;

import java.util.Set;

/**
 * Pluggable sink for a file upload bound to a {@link org.omnaest.react4j.component.form.Form.FileUploadFormElement}.
 * <p>
 * Ships with two implementations: {@link ByteArrayChannel} (captures the upload into memory) and {@link FileChannel} (streams the upload directly to a
 * server-controlled filesystem path). Applications may implement further sinks (e.g. S3, encryption) without any change to the upload endpoint or the form
 * element.
 * <p>
 * <b>MIME/content-type policy:</b> this component does NOT restrict content-types by default - accepting or rejecting a given content type is the hosting
 * application's responsibility. A channel may opt in to a server-side allow-list via {@link #acceptedContentTypes()}; when non-empty it is enforced before
 * any bytes are read. The frontend's {@code accept} attribute is UX hinting only and is never a security control.
 * <p>
 * <b>Usage constraint:</b> the application must retain a <b>stable</b> reference to a channel instance across renders (e.g. an instance field or a
 * session-scoped bean), not create a new one inline per render, since the registry keys the channel to the rendered element's location.
 *
 * @author omnaest
 */
public interface UploadChannel
{
    /**
     * Consumes the given {@link UploadContent}, enforcing {@link #maxSizeBytes()} and (if non-empty) {@link #acceptedContentTypes()}.
     *
     * @param content
     * @return
     * @throws UploadException
     *             if the size limit or content-type allow-list is violated, or an I/O error occurs
     */
    public UploadReceipt consume(UploadContent content);

    /**
     * The maximum number of bytes this channel accepts. Default is 10 MB.
     *
     * @return
     */
    public long maxSizeBytes();

    /**
     * The set of accepted content types, or an empty set to accept all (the default).
     *
     * @return
     */
    public Set<String> acceptedContentTypes();

}
