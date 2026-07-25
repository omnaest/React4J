package org.omnaest.react4j.service.internal.service;

import java.util.List;

import org.omnaest.react4j.domain.Card;
import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.UIComponentFactory;
import org.omnaest.react4j.domain.markdown.MarkdownIssue;
import org.omnaest.react4j.domain.markdown.MarkdownIssueHandler;

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

        /**
         * Declares where the markdown to interpret comes from, e.g. the content file identifier. The origin ends up in every {@link MarkdownIssue} and is the
         * only way for the receiver to tell which content file has to be fixed.
         *
         * @param source
         * @return
         */
        public FactoryLoadedMarkdownInterpreter withSource(String source);

        /**
         * Routes the {@link MarkdownIssue}s of this single interpretation to the given {@link MarkdownIssueHandler} instead of the default one.
         *
         * @param issueHandler
         * @return
         */
        public FactoryLoadedMarkdownInterpreter withIssueHandler(MarkdownIssueHandler issueHandler);

    }
}
