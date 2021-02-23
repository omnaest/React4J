package org.omnaest.react4j.service.internal.component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.NavigationBar;
import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.i18n.I18nText;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer;
import org.omnaest.react4j.domain.rendering.components.LocationSupport;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.domain.rendering.node.NodeRenderType;
import org.omnaest.react4j.domain.rendering.node.NodeRenderer;
import org.omnaest.react4j.domain.rendering.node.NodeRendererRegistry;
import org.omnaest.react4j.domain.rendering.node.NodeRenderingProcessor;
import org.omnaest.react4j.service.internal.nodes.NavigationBarNode;
import org.omnaest.utils.MapUtils;
import org.omnaest.utils.template.TemplateUtils;

public class NavigationBarImpl extends AbstractUIComponent<NavigationBar> implements NavigationBar
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
            public Location getLocation(LocationSupport locationSupport)
            {
                return locationSupport.createLocation(NavigationBarImpl.this.getId());
            }

            @Override
            public Node render(RenderingProcessor renderingProcessor, Location location)
            {
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

            @Override
            public void manageNodeRenderers(NodeRendererRegistry registry)
            {
                registry.register(NavigationBarNode.class, NodeRenderType.HTML, new NodeRenderer<NavigationBarNode>()
                {
                    @Override
                    public String render(NavigationBarNode node, NodeRenderingProcessor nodeRenderingProcessor)
                    {
                        return TemplateUtils.builder()
                                            .useTemplateClassResource(this.getClass(), "/render/templates/html/navigationbar.html")
                                            .add("entries", node.getEntries()
                                                                .stream()
                                                                .map(entry -> MapUtils.builder()
                                                                                      .put("link", Optional.ofNullable(entry.getLinkedId())
                                                                                                           .map(linkedId -> "#" + linkedId)
                                                                                                           .orElse(entry.getLink()))
                                                                                      .put("text", nodeRenderingProcessor.render(entry.getText()))
                                                                                      .build())
                                                                .collect(Collectors.toList()))
                                            .build()
                                            .get();
                    }
                });
            }

            @Override
            public Stream<UIComponent<?>> getSubComponents()
            {
                return Stream.empty();
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