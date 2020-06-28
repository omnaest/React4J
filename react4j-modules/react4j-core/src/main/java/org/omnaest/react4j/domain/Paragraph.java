package org.omnaest.react4j.domain;

import java.util.function.Consumer;

import org.omnaest.react4j.domain.i18n.I18nText;

public interface Paragraph extends UIComponent<Paragraph>
{
    public Paragraph addText(String text);

    public Paragraph addText(I18nText i18nText);

    public Paragraph addLink(Consumer<Anker> ankerConsumer);
}