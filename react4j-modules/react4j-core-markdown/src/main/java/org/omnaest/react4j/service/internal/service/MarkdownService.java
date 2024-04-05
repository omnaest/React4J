package org.omnaest.react4j.service.internal.service;

import java.util.List;

import org.omnaest.react4j.domain.Card;
import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.UIComponentFactory;

/**
 * @author omnaest
 */
public interface MarkdownService
{
    public FactoryLoadedMarkdownInterpreter interpreterWith(UIComponentFactory uiComponentFactory);

    public static interface FactoryLoadedMarkdownInterpreter
    {

        public List<UIComponent<?>> parseMarkdownElements(String markdown);

        public List<Card> newMarkdownCards(String markdown);

    }
}
