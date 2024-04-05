package org.omnaest.react4j.component.anker.internal.data;

import org.omnaest.react4j.domain.i18n.I18nText;

import lombok.Builder;
import lombok.Builder.Default;

@lombok.Data
@Builder
public class AnkerData
{
    private I18nText text;

    private I18nText title;

    private String link;

    @Default
    private boolean isSamePage = false;
}