package org.omnaest.react.internal.component;

import org.omnaest.react.domain.Image;
import org.omnaest.react.domain.Location;
import org.omnaest.react.domain.i18n.I18nText;
import org.omnaest.react.domain.raw.Node;
import org.omnaest.react.domain.raw.UIComponentRenderer;
import org.omnaest.react.internal.nodes.ImageNode;

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
            public Node render(Location parentLocation)
            {
                Location location = Location.of(parentLocation, ImageImpl.this.getId());
                return new ImageNode().setImage(ImageImpl.this.image)
                                      .setName(ImageImpl.this.getTextResolver()
                                                             .apply(ImageImpl.this.name, location));
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