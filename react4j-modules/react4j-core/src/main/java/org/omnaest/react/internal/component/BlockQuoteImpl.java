package org.omnaest.react.internal.component;

import java.util.ArrayList;
import java.util.List;

import org.omnaest.react.domain.BlockQuote;
import org.omnaest.react.domain.Location;
import org.omnaest.react.domain.i18n.I18nText;
import org.omnaest.react.domain.raw.Node;
import org.omnaest.react.domain.raw.UIComponentRenderer;
import org.omnaest.react.internal.nodes.BlockQuoteNode;

public class BlockQuoteImpl extends AbstractUIComponent implements BlockQuote
{
    private List<I18nText> texts = new ArrayList<>();
    private I18nText       footer;

    public BlockQuoteImpl(ComponentContext context)
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
                Location location = Location.of(parentLocation, BlockQuoteImpl.this.getId());
                return new BlockQuoteNode().setTexts(BlockQuoteImpl.this.getTextResolver()
                                                                        .apply(BlockQuoteImpl.this.texts, location))
                                           .setFooter(BlockQuoteImpl.this.getTextResolver()
                                                                         .apply(BlockQuoteImpl.this.footer, location));
            }
        };
    }

    @Override
    public BlockQuote addText(String text)
    {
        this.texts.add(this.toI18nText(text));
        return this;
    }

    @Override
    public BlockQuote withFooter(String footer)
    {
        this.footer = this.toI18nText(footer);
        return this;
    }

}