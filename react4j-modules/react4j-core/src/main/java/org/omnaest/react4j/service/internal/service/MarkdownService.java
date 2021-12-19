package org.omnaest.react4j.service.internal.service;

import java.util.List;
import java.util.stream.Stream;

import org.omnaest.react4j.domain.Card;
import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.UIComponentFactory;
import org.omnaest.utils.markdown.MarkdownUtils.Element;

/**
 * @author omnaest
 */
public interface MarkdownService
{
    public FactoryLoadedMarkdownInterpreter interpreterWith(UIComponentFactory uiComponentFactory);

    public static interface FactoryLoadedMarkdownInterpreter
    {

        public List<UIComponent<?>> parseMarkdownElements(Stream<Element> elements);

        public List<Card> newMarkdownCards(String markdown);

    }
}
