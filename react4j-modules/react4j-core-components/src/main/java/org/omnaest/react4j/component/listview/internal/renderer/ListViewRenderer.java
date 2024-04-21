package org.omnaest.react4j.component.listview.internal.renderer;

import java.util.Optional;
import java.util.stream.Stream;

import org.omnaest.react4j.component.listview.internal.data.ListViewData;
import org.omnaest.react4j.component.listview.internal.renderer.node.ListViewNode;
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
import org.omnaest.utils.functional.Provider;
import org.omnaest.utils.template.TemplateUtils;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ListViewRenderer implements UIComponentRenderer
{
    private final ListViewData     data;
    private final Provider<String> idProvider;

    @Override
    public Location getLocation(LocationSupport locationSupport)
    {
        return locationSupport.createLocation(this.idProvider.get());
    }

    @Override
    public Node render(RenderingProcessor renderingProcessor, Location location, Optional<Data> data)
    {
        return ListViewNode.builder()
                           .element(renderingProcessor.process(this.data.getElement(), location))
                           .build();
    }

    @Override
    public void manageNodeRenderers(NodeRendererRegistry registry)
    {
        registry.register(ListViewNode.class, NodeRenderType.HTML, new NodeRenderer<ListViewNode>()
        {
            @Override
            public String render(ListViewNode node, NodeRenderingProcessor nodeRenderingProcessor)
            {
                return TemplateUtils.builder()
                                    .useTemplateClassResource(this.getClass(), "/render/templates/html/listview.html")
                                    .add("element", nodeRenderingProcessor.render(node.getElement()))
                                    .build()
                                    .get();
            }
        });
    }

    @Override
    public Stream<ParentLocationAndComponent> getSubComponents(Location parentLocation)
    {
        return Optional.ofNullable(this.data.getElement())
                       .map(component -> ParentLocationAndComponent.of(parentLocation, component))
                       .stream();
    }

    @Override
    public void manageEventHandler(EventHandlerRegistrationSupport eventHandlerRegistrationSupport)
    {
    }
}