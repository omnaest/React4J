package org.omnaest.react4j.service.internal.component;

import java.util.stream.Stream;

import org.omnaest.react4j.domain.Image;
import org.omnaest.react4j.domain.Location;
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
import org.omnaest.react4j.service.internal.nodes.ImageNode;
import org.omnaest.utils.template.TemplateUtils;

public class ImageImpl extends AbstractUIComponent<Image> implements Image
{
    private I18nText name;
    private String   image;

    public ImageImpl(ComponentContext context)
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
                return locationSupport.createLocation(ImageImpl.this.getId());
            }

            @Override
            public Node render(RenderingProcessor renderingProcessor, Location location)
            {
                return new ImageNode().setImage(ImageImpl.this.image)
                                      .setName(ImageImpl.this.getTextResolver()
                                                             .apply(ImageImpl.this.name, location));
            }

            @Override
            public void manageNodeRenderers(NodeRendererRegistry registry)
            {
                registry.register(ImageNode.class, NodeRenderType.HTML, new NodeRenderer<ImageNode>()
                {
                    @Override
                    public String render(ImageNode node, NodeRenderingProcessor nodeRenderingProcessor)
                    {
                        return TemplateUtils.builder()
                                            .useTemplateClassResource(this.getClass(), "/render/templates/html/image.html")
                                            .add("image", node.getImage())
                                            .add("alternativeText", nodeRenderingProcessor.render(node.getName()))
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
    public Image withName(String name)
    {
        this.name = I18nText.of(this.getLocations(), name, this.getDefaultLocale());
        return this;
    }

    @Override
    public Image withImage(String imageName)
    {
        this.image = imageName;
        return this;
    }
}