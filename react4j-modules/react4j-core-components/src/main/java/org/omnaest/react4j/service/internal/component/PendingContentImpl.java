package org.omnaest.react4j.service.internal.component;

import java.util.Optional;
import java.util.stream.Stream;

import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.PendingContent;
import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.context.data.Data;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer;
import org.omnaest.react4j.domain.rendering.components.LocationSupport;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.domain.rendering.node.NodeRenderType;
import org.omnaest.react4j.domain.rendering.node.NodeRendererRegistry;
import org.omnaest.react4j.domain.support.UIComponentProvider;
import org.omnaest.react4j.service.internal.nodes.PendingContentNode;

public class PendingContentImpl extends AbstractUIComponentAndContentHolder<PendingContent> implements PendingContent
{
    /**
     * Long enough that a fast interaction never flashes a block into the middle of a transcript, short enough that
     * a genuinely slow one is reported before a user starts to wonder.
     */
    private static final int DEFAULT_APPEAR_AFTER_MILLIS = 300;

    private UIComponent<?>   content;

    private int              appearAfterMillis           = DEFAULT_APPEAR_AFTER_MILLIS;

    public PendingContentImpl(ComponentContext context)
    {
        super(context);
    }

    public PendingContentImpl(ComponentContext context, UIComponent<?> content, int appearAfterMillis)
    {
        super(context);
        this.content = content;
        this.appearAfterMillis = appearAfterMillis;
    }

    @Override
    public PendingContent withContent(UIComponent<?> component)
    {
        this.content = component;
        return this;
    }

    @Override
    public PendingContent withAppearAfterMillis(int appearAfterMillis)
    {
        this.appearAfterMillis = appearAfterMillis;
        return this;
    }

    @Override
    public UIComponentRenderer asRenderer()
    {
        return new UIComponentRenderer()
        {
            @Override
            public Location getLocation(LocationSupport locationSupport)
            {
                return locationSupport.createLocation(PendingContentImpl.this.getId());
            }

            @Override
            public Node render(RenderingProcessor renderingProcessor, Location location, Optional<Data> data)
            {
                return new PendingContentNode().setContent(Optional.ofNullable(PendingContentImpl.this.content)
                                                                   .map(content -> renderingProcessor.process(content, location))
                                                                   .orElse(null))
                                               .setAppearAfterMillis(PendingContentImpl.this.appearAfterMillis);
            }

            @Override
            public void manageNodeRenderers(NodeRendererRegistry registry)
            {
                // The static HTML rendering omits it entirely rather than showing it. A printed or crawled page has
                // no round trip in flight, so a "waiting for a response" block there would be describing a state
                // that cannot exist in that medium.
                registry.register(PendingContentNode.class, NodeRenderType.HTML, (node, nodeRenderingProcessor) -> "");
            }

            @Override
            public void manageEventHandler(EventHandlerRegistrationSupport eventHandlerRegistrationSupport)
            {
            }

            @Override
            public Stream<ParentLocationAndComponent> getSubComponents(Location parentLocation)
            {
                // Enumerated even though the content is usually hidden: this traversal is what registers event
                // handlers, and it runs once, on the initial page render. Content skipped here would render but do
                // nothing if it ever became visible.
                return Stream.of(ParentLocationAndComponent.of(parentLocation, PendingContentImpl.this.content));
            }
        };
    }

    @Override
    public UIComponentProvider<PendingContent> asTemplateProvider()
    {
        return () -> new PendingContentImpl(this.context, this.content, this.appearAfterMillis);
    }
}
