package org.omnaest.react4j.component.ankerbutton.internal.renderer;

import java.util.Optional;
import java.util.stream.Stream;

import org.omnaest.react4j.component.ankerbutton.internal.data.AnkerButtonData;
import org.omnaest.react4j.component.ankerbutton.internal.renderer.node.AnkerButtonNode;
import org.omnaest.react4j.component.ankerbutton.internal.renderer.node.AnkerButtonNode.Page;
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
import org.omnaest.react4j.service.internal.service.LocalizedTextResolverService;
import org.omnaest.utils.functional.Provider;
import org.omnaest.utils.template.TemplateUtils;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AnkerButtonRenderer implements UIComponentRenderer
{
    private final AnkerButtonData              data;
    private final LocalizedTextResolverService textResolver;
    private final Provider<String>             idProvider;

    @Override
    public Location getLocation(LocationSupport locationSupport)
    {
        return locationSupport.createLocation(this.idProvider.get());
    }

    @Override
    public Node render(RenderingProcessor renderingProcessor, Location location, Optional<Data> data)
    {
        return new AnkerButtonNode().setText(this.textResolver.apply(this.data.getText(), location))
                                    .setLink(this.data.getLink())
                                    .setStyle(this.data.getStyle()
                                                       .name()
                                                       .toLowerCase())
                                    .setPage(this.data.isSamePage() ? Page.SELF : Page.BLANK);
    }

    @Override
    public void manageNodeRenderers(NodeRendererRegistry registry)
    {
        registry.register(AnkerButtonNode.class, NodeRenderType.HTML, new NodeRenderer<AnkerButtonNode>()
        {
            @Override
            public String render(AnkerButtonNode node, NodeRenderingProcessor nodeRenderingProcessor)
            {
                return TemplateUtils.builder()
                                    .useTemplateClassResource(this.getClass(), "/render/templates/html/anker_button.html")
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