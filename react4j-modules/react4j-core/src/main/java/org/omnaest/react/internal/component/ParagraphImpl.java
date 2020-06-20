package org.omnaest.react.internal.component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.omnaest.react.domain.Anker;
import org.omnaest.react.domain.Location;
import org.omnaest.react.domain.Paragraph;
import org.omnaest.react.domain.UIComponent;
import org.omnaest.react.domain.i18n.I18nText;
import org.omnaest.react.domain.raw.Node;
import org.omnaest.react.domain.raw.UIComponentRenderer;
import org.omnaest.react.internal.nodes.ParagraphNode;

public class ParagraphImpl extends AbstractUIComponent<Paragraph> implements Paragraph
{
    private List<UIComponent<?>> elements = new ArrayList<>();

    public ParagraphImpl(ComponentContext context)
    {
        super(context);
    }

    @Override
    public Paragraph addText(I18nText text)
    {
        this.elements.add(this.getUiComponentFactory()
                              .newText()
                              .addText(text));
        return this;
    }

    @Override
    public Paragraph addText(String text)
    {
        return this.addText(this.toI18nText(text));
    }

    @Override
    public Paragraph addLink(Consumer<Anker> ankerConsumer)
    {
        Anker anker = this.getUiComponentFactory()
                          .newAnker();
        ankerConsumer.accept(anker);
        this.elements.add(anker);
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
                Location location = Location.of(parentLocation, ParagraphImpl.this.getId());
                return new ParagraphNode().setId(ParagraphImpl.this.getId())
                                          .setElements(ParagraphImpl.this.elements.stream()
                                                                                  .map(element -> element.asRenderer()
                                                                                                         .render(location))
                                                                                  .collect(Collectors.toList()));
            }
        };
    }
}