package org.omnaest.react4j.service.internal.component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.UIComponentFactory;
import org.omnaest.react4j.domain.VerticalContentSwitcher;
import org.omnaest.react4j.domain.i18n.I18nText;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.raw.UIComponentRenderer;
import org.omnaest.react4j.service.internal.nodes.VerticalContentSwitcherNode;
import org.omnaest.react4j.service.internal.service.LocalizedTextResolverService;

public class VerticalContentSwitcherImpl extends AbstractUIComponent<VerticalContentSwitcher> implements VerticalContentSwitcher
{
    private List<VerticalContentImpl> elements = new ArrayList<>();

    public VerticalContentSwitcherImpl(ComponentContext context)
    {
        super(context);
    }

    @Override
    public UIComponentRenderer asRenderer()
    {
        return new UIComponentRenderer()
        {
            @Override
            public Node render(Location parentLocation)
            {
                Location location = Location.of(parentLocation, VerticalContentSwitcherImpl.this.getId());
                LocalizedTextResolverService textResolver = VerticalContentSwitcherImpl.this.getTextResolver();
                return new VerticalContentSwitcherNode().setElements(VerticalContentSwitcherImpl.this.elements.stream()
                                                                                                              .map(element -> new VerticalContentSwitcherNode.ContentElement().setTitle(textResolver.apply(element.getTitle(),
                                                                                                                                                                                                           location))
                                                                                                                                                                              .setActive(element.isActive())
                                                                                                                                                                              .setDisabled(element.isDisabled())
                                                                                                                                                                              .setContent(Optional.ofNullable(element.getComponent())
                                                                                                                                                                                                  .map(component -> component.asRenderer()
                                                                                                                                                                                                                             .render())
                                                                                                                                                                                                  .orElse(null))

                                                                                                              )
                                                                                                              .collect(Collectors.toList()));
            }
        };
    }

    @Override
    public VerticalContentSwitcher addContentEntry(Consumer<VerticalContent> contentConsumer)
    {
        VerticalContentImpl verticalContent = new VerticalContentImpl(this.getUiComponentFactory(), this.i18nTextMapper());
        contentConsumer.accept(verticalContent);
        this.elements.add(verticalContent);
        return this;
    }

    private static class VerticalContentImpl extends AbstractUIContentHolder<VerticalContent> implements VerticalContent
    {
        private final Function<String, I18nText> i18nTextMapper;

        private UIComponent<?> component;
        private State          state;
        private I18nText       title;

        public VerticalContentImpl(UIComponentFactory uiComponentFactory, Function<String, I18nText> i18nTextMapper)
        {
            super(uiComponentFactory);
            this.i18nTextMapper = i18nTextMapper;
        }

        @Override
        public VerticalContent withTitle(String title)
        {
            this.title = this.i18nTextMapper.apply(title);
            return this;
        }

        @Override
        public VerticalContent withState(State state)
        {
            this.state = state;
            return this;
        }

        @Override
        public VerticalContent withContent(UIComponent<?> component)
        {
            this.component = component;
            return this;
        }

        public UIComponent<?> getComponent()
        {
            return this.component;
        }

        public boolean isDisabled()
        {
            return State.DISABLED.equals(this.state);
        }

        public boolean isActive()
        {
            return State.ACTIVE.equals(this.state);
        }

        public I18nText getTitle()
        {
            return this.title;
        }

    }

}