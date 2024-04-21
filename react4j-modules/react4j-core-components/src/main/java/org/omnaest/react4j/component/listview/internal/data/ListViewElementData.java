package org.omnaest.react4j.component.listview.internal.data;

import org.omnaest.react4j.domain.UIComponent;

import lombok.Builder;
import lombok.Data;

@Data
@Builder(toBuilder = true)
public class ListViewElementData
{
    private UIComponent<?> content;
}