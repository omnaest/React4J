package org.omnaest.react4j.component.form.internal.data;

import java.util.List;

import org.omnaest.react4j.component.form.Form.FormElement;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;

@Data
@Builder(toBuilder = true)
public class FormData
{
    @Singular("addElement")
    private List<FormElement<?>> elements;
}