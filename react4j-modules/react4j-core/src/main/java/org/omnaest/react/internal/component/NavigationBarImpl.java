package org.omnaest.react.internal.component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.omnaest.react.domain.Location;
import org.omnaest.react.domain.NavigationBar;
import org.omnaest.react.domain.UIComponent;
import org.omnaest.react.domain.i18n.I18nText;
import org.omnaest.react.domain.raw.Node;
import org.omnaest.react.domain.raw.UIComponentRenderer;
import org.omnaest.react.internal.nodes.NavigationBarNode;

public class NavigationBarImpl extends AbstractUIComponent implements NavigationBar
{
    private List<NavigationEntryImpl> entries = new ArrayList<>();

    public NavigationBarImpl(ComponentContext context)
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
                Location location = Location.of(parentLocation, NavigationBarImpl.this.getId());
                return new NavigationBarNode().setEntries(NavigationBarImpl.this.entries.stream()
                                                                                        .map(entry -> new NavigationBarNode.Entry().setActive(entry.isActive())
                                                                                                                                   .setDisabled(entry.isDisabled())
                                                                                                                                   .setLink(entry.getLink())
                                                                                                                                   .setLinkedId(entry.getLinkedId())
                                                                                                                                   .setText(NavigationBarImpl.this.getTextResolver()
                                                                                                                                                                  .apply(entry.getText(),
                                                                                                                                                                         location)))
                                                                                        .collect(Collectors.toList()));
            }
        };
    }

    @Override
    public NavigationBar addEntry(Consumer<NavigationBarEntry> navigationBarEntryConsumer)
    {
        NavigationEntryImpl entry = new NavigationEntryImpl(text -> this.toI18nText(text));
        navigationBarEntryConsumer.accept(entry);
        this.entries.add(entry);
        return this;
    }

    private static class NavigationEntryImpl implements NavigationBarEntry
    {
        private Function<String, I18nText> i18nTextResolver;

        private I18nText text;
        private String   link;
        private String   linkedId;
        private boolean  active;

        private boolean disabled;

        public NavigationEntryImpl(Function<String, I18nText> i18nTextResolver)
        {
            super();
            this.i18nTextResolver = i18nTextResolver;
        }

        @Override
        public NavigationBarEntry withText(String text)
        {
            this.text = this.i18nTextResolver.apply(text);
            return this;
        }

        @Override
        public NavigationBarEntry withLink(String link)
        {
            this.link = link;
            return this;
        }

        @Override
        public NavigationBarEntry withLinkedLocator(String id)
        {
            this.linkedId = id;
            return this;
        }

        @Override
        public NavigationBarEntry withLinked(UIComponent component)
        {
            return this.withLinkedLocator(component.getId());
        }

        @Override
        public NavigationBarEntry withActiveState(boolean active)
        {
            this.active = active;
            return this;
        }

        @Override
        public NavigationBarEntry withDisabledState(boolean disabled)
        {
            this.disabled = disabled;
            return this;
        }

        public boolean isActive()
        {
            return this.active;
        }

        public I18nText getText()
        {
            return this.text;
        }

        public String getLink()
        {
            return this.link;
        }

        public String getLinkedId()
        {
            return this.linkedId;
        }

        public boolean isDisabled()
        {
            return this.disabled;
        }

        @Override
        public String toString()
        {
            return "NavigationEntryImpl [i18nTextResolver=" + this.i18nTextResolver + ", text=" + this.text + ", link=" + this.link + ", linkedId="
                    + this.linkedId + ", active=" + this.active + ", disabled=" + this.disabled + "]";
        }

    }
}