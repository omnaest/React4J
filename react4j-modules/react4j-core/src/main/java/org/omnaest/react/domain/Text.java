package org.omnaest.react.domain;

import org.omnaest.react.domain.i18n.I18nText;

public interface Text extends UIComponent<Text>
{
    public Text addText(String text);

    public Text addText(I18nText text);
}