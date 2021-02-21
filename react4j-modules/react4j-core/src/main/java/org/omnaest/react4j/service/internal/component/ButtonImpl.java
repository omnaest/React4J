package org.omnaest.react4j.service.internal.component;

import java.util.stream.Stream;

import org.omnaest.react4j.domain.Button;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.i18n.I18nText;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer;
import org.omnaest.react4j.domain.rendering.components.LocationSupport;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.domain.rendering.node.NodeRendererRegistry;
import org.omnaest.react4j.service.internal.handler.domain.EventHandler;
import org.omnaest.react4j.service.internal.handler.domain.Target;
import org.omnaest.react4j.service.internal.nodes.ButtonNode;
import org.omnaest.react4j.service.internal.nodes.handler.ServerHandler;

public class ButtonImpl extends AbstractUIComponent<Button> implements Button
{
    private I18nText     name;
    private Style        style = Style.PRIMARY;
    private EventHandler eventHandler;

    public ButtonImpl(ComponentContext context)
    {
        super(context);
    }

    @Override
    public Button withName(String name)
    {
        this.name = this.toI18nText(name);
        return this;
    }

    @Override
    public Button withStyle(Style style)
    {
        this.style = style;
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
                return locationSupport.createLocation(ButtonImpl.this.getId());
            }

            @Override
            public Node render(RenderingProcessor renderingProcessor, Location location)
            {
                Target target = Target.from(location);
                ButtonImpl.this.getEventHandlerRegistry()
                               .register(target, ButtonImpl.this.eventHandler);
                return new ButtonNode().setName(ButtonImpl.this.getTextResolver()
                                                               .apply(ButtonImpl.this.name, location))
                                       .setStyle(ButtonImpl.this.style.name()
                                                                      .toLowerCase())
                                       .setOnClick(new ServerHandler(target));
            }

            @Override
            public void manageNodeRenderers(NodeRendererRegistry registry)
            {
                // TODO Auto-generated method stub

            }

            @Override
            public Stream<UIComponent<?>> getSubComponents()
            {
                return Stream.empty();
            }

        };
    }

    @Override
    public Button onClick(EventHandler eventHandler)
    {
        this.eventHandler = eventHandler;
        return this;
    }
}