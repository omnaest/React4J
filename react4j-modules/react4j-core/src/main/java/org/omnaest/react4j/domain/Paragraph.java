package org.omnaest.react4j.domain;

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

import org.omnaest.react4j.domain.Icon.StandardIcon;
import org.omnaest.react4j.domain.i18n.I18nText;

public interface Paragraph extends UIComponent<Paragraph>
{
    public Paragraph addText(String text);

    public Paragraph addText(I18nText i18nText);

    public Paragraph addText(StandardIcon icon, String text);

    public Paragraph addText(StandardIcon icon, I18nText text);

    public Paragraph addLink(Consumer<Anker> ankerConsumer);

    public Paragraph addLinkButton(Consumer<AnkerButton> ankerButtonConsumer);

    /**
     * Reads the {@link StandardCharsets#UTF_8} encoded text file from the given absolute resource path
     * 
     * @param resourcePath
     * @return
     */
    public Paragraph addTextsByClasspathResource(String resourcePath);

    public Paragraph addLineBreak();

}