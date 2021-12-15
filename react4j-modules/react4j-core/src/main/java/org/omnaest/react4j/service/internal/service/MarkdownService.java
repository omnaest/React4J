package org.omnaest.react4j.service.internal.service;

import java.util.List;
import java.util.stream.Stream;

import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.UIComponentFactory;
import org.omnaest.utils.markdown.MarkdownUtils.Element;

/**
 * @author omnaest
 */
public interface MarkdownService
{

    public static interface FactoryLoadedMarkdownInterpreter
    {

        List<UIComponent<?>> parseMarkdownElements(Stream<Element> elements);

    }

    FactoryLoadedMarkdownInterpreter interpreterWith(UIComponentFactory uiComponentFactory);
}
