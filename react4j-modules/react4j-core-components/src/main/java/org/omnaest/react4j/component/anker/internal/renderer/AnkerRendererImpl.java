package org.omnaest.react4j.component.anker.internal.renderer;

import java.util.Optional;
import java.util.stream.Stream;

import org.omnaest.react4j.component.anker.internal.data.AnkerData;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.context.data.Data;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer;
import org.omnaest.react4j.domain.rendering.components.LocationSupport;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.domain.rendering.node.NodeRenderType;
import org.omnaest.react4j.domain.rendering.node.NodeRenderer;
import org.omnaest.react4j.domain.rendering.node.NodeRendererRegistry;
import org.omnaest.react4j.domain.rendering.node.NodeRenderingProcessor;
import org.omnaest.react4j.service.internal.nodes.AnkerNode;
import org.omnaest.react4j.service.internal.nodes.AnkerNode.Page;
import org.omnaest.react4j.service.internal.service.LocalizedTextResolverService;
import org.omnaest.utils.functional.Provider;
import org.omnaest.utils.template.TemplateUtils;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AnkerRendererImpl implements UIComponentRenderer
{
    private final LocalizedTextResolverService textResolver;
    private final AnkerData                    ankerData;
    private final Provider<String>             idProvider;

    @Override
    public Location getLocation(LocationSupport locationSupport)
    {
        return locationSupport.createLocation(this.idProvider.get());
    }

    @Override
    public Node render(RenderingProcessor renderingProcessor, Location location, Optional<Data> data)
    {
        return new AnkerNode().setText(this.textResolver.apply(this.ankerData.getText(), location))
                              .setTitle(this.textResolver.apply(this.ankerData.getTitle(), location))
                              .setLink(this.ankerData.getLink())
                              .setPage(this.ankerData.isSamePage() ? Page.SELF : Page.BLANK);
    }

    @Override
    public void manageNodeRenderers(NodeRendererRegistry registry)
    {
        registry.register(AnkerNode.class, NodeRenderType.HTML, new NodeRenderer<AnkerNode>()
        {
            @Override
            public String render(AnkerNode node, NodeRenderingProcessor nodeRenderingProcessor)
            {
                return TemplateUtils.builder()
                                    .useTemplateClassResource(this.getClass(), "/render/templates/html/anker.html")
                                    .add("link", node.getLink())
                                    .add("text", nodeRenderingProcessor.render(node.getText()))
                                    .build()
                                    .get();
            }
        });
    }

    @Override
    public Stream<ParentLocationAndComponent> getSubComponents(Location parentLocation)
    {
        return Stream.empty();
    }

    @Override
    public void manageEventHandler(EventHandlerRegistrationSupport eventHandlerRegistrationSupport)
    {
    }
}