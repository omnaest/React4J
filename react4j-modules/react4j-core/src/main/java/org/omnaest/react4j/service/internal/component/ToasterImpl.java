package org.omnaest.react4j.service.internal.component;

import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.Toaster;
import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.i18n.I18nText;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.raw.UIComponentRenderer;
import org.omnaest.react4j.service.internal.nodes.ToasterNode;
import org.omnaest.react4j.service.internal.service.LocalizedTextResolverService;

public class ToasterImpl extends AbstractUIComponentAndContentHolder<Toaster> implements Toaster
{
    private I18nText       title;
    private UIComponent<?> content;

    public ToasterImpl(ComponentContext context)
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
                Location location = Location.of(parentLocation, ToasterImpl.this.getId());
                LocalizedTextResolverService textResolver = ToasterImpl.this.getTextResolver();
                return new ToasterNode().setContent(ToasterImpl.this.content.asRenderer()
                                                                            .render(location))
                                        .setTitle(textResolver.apply(ToasterImpl.this.title, Location.of(parentLocation, ToasterImpl.this.getId())));
            }
        };
    }

    @Override
    public Toaster withTitle(String title)
    {
        this.title = I18nText.of(this.getLocations(), title, this.getDefaultLocale());
        return this;
    }

    @Override
    public Toaster withContent(UIComponent<?> component)
    {
        this.content = component;
        return this;
    }

}