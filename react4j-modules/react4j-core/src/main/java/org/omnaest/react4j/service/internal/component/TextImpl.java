package org.omnaest.react4j.service.internal.component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.Text;
import org.omnaest.react4j.domain.i18n.I18nText;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.raw.UIComponentRenderer;
import org.omnaest.react4j.service.internal.nodes.TextNode;

public class TextImpl extends AbstractUIComponent<Text> implements Text
{
    private List<I18nText> texts = new ArrayList<>();

    public TextImpl(ComponentContext context)
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
                Location location = Location.of(parentLocation, TextImpl.this.getId());
                return new TextNode().setTexts(TextImpl.this.texts.stream()
                                                                  .map(text -> TextImpl.this.getTextResolver()
                                                                                            .apply(text, location))
                                                                  .collect(Collectors.toList()));
            }
        };
    }

    @Override
    public Text addText(String text)
    {
        this.addText(this.toI18nText(text));
        return this;
    }

    @Override
    public Text addText(I18nText text)
    {
        this.texts.add(text);
        return this;
    }

}