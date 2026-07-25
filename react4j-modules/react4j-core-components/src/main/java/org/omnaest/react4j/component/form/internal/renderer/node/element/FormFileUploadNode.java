package org.omnaest.react4j.component.form.internal.renderer.node.element;

import org.omnaest.react4j.service.internal.nodes.handler.Handler;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FormFileUploadNode
{
    @JsonProperty
    private String  uploadUrl;

    @JsonProperty
    private String  uploadId;

    @JsonProperty
    private String  accept;

    @JsonProperty
    private long    maxSize;

    @JsonProperty
    private Handler onComplete;

}
