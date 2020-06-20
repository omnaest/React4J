package org.omnaest.react.internal.component;

import org.omnaest.react.domain.Button;
import org.omnaest.react.domain.Location;
import org.omnaest.react.domain.i18n.I18nText;
import org.omnaest.react.domain.raw.Node;
import org.omnaest.react.domain.raw.UIComponentRenderer;
import org.omnaest.react.internal.handler.domain.EventHandler;
import org.omnaest.react.internal.handler.domain.Target;
import org.omnaest.react.internal.nodes.ButtonNode;
import org.omnaest.react.internal.nodes.handler.ServerHandler;

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
            public Node render(Location parentLocation)
            {
                Location location = Location.of(parentLocation, ButtonImpl.this.getId());
                Target target = Target.from(location);
                ButtonImpl.this.getEventHandlerRegistry()
                               .register(target, ButtonImpl.this.eventHandler);
                return new ButtonNode().setName(ButtonImpl.this.getTextResolver()
                                                               .apply(ButtonImpl.this.name, location))
                                       .setStyle(ButtonImpl.this.style.name()
                                                                      .toLowerCase())
                                       .setOnClick(new ServerHandler(target));
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