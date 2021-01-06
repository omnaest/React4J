package org.omnaest.react4j.service.internal.component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.omnaest.react4j.domain.ImageIndex;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.i18n.I18nText;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.raw.UIComponentRenderer;
import org.omnaest.react4j.service.internal.nodes.ImageIndexNode;
import org.omnaest.react4j.service.internal.service.LocalizedTextResolverService;
import org.omnaest.utils.element.tri.TriElement;

public class ImageIndexImpl extends AbstractUIComponent<ImageIndex> implements ImageIndex
{
    private List<TriElement<I18nText, String, String>> entries = new ArrayList<>();

    public ImageIndexImpl(ComponentContext context)
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
                Location location = Location.of(parentLocation, ImageIndexImpl.this.getId());
                LocalizedTextResolverService textResolver = ImageIndexImpl.this.getTextResolver();

                return new ImageIndexNode().setEntries(ImageIndexImpl.this.entries.stream()
                                                                                  .map(tri -> new ImageIndexNode.Entry().setTitle(textResolver.apply(tri.getFirst(),
                                                                                                                                                     location))
                                                                                                                        .setId(tri.getSecond())
                                                                                                                        .setImage(tri.getThird()))
                                                                                  .collect(Collectors.toList()));
            }
        };
    }

    @Override
    public ImageIndex addEntry(String title, String id, String image)
    {
        this.entries.add(TriElement.of(this.toI18nText(title), id, image));
        return this;
    }

}