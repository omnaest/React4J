package org.omnaest.react4j.component.form.internal.data;

import java.util.List;

import org.omnaest.react4j.component.form.Form.FormElement;
import org.omnaest.react4j.domain.context.document.Document;
import org.omnaest.react4j.service.internal.handler.domain.DataEventHandler;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.Singular;

@Data
@Builder(toBuilder = true)
public class FormData
{
    @Singular("addElement")
    private List<FormElement<?>> elements;

    @Default
    private boolean responsive = true;

    private Document document;

    private DataEventHandler eventHandler;
}