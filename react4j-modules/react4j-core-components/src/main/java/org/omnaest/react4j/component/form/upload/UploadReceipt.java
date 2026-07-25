package org.omnaest.react4j.component.form.upload;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Data;

/**
 * JSON-serializable receipt returned to the client after an upload has been consumed by an {@link UploadChannel}.
 *
 * @author omnaest
 */
@Data
@Builder(toBuilder = true)
public class UploadReceipt
{
    @JsonProperty
    private String uploadId;

    @JsonProperty
    private String filename;

    @JsonProperty
    private long   size;

    @JsonProperty
    private String contentType;

}
