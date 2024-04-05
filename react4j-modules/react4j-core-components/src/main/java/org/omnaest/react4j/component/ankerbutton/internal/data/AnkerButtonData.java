package org.omnaest.react4j.component.ankerbutton.internal.data;

import org.omnaest.react4j.domain.Button.Style;
import org.omnaest.react4j.domain.i18n.I18nText;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;

@Data
@Builder
public class AnkerButtonData
{
    private I18nText text;

    private String link;

    @Default
    private Style style = Style.PRIMARY;

    @Default
    private boolean isSamePage = false;
}