package org.omnaest.react4j.component.form.internal.element;

import java.util.function.Function;
import java.util.function.Supplier;

import org.omnaest.react4j.component.form.Form.RangeFormElement;
import org.omnaest.react4j.component.form.internal.renderer.node.FormNode.FormRangeNode;
import org.omnaest.react4j.component.form.internal.renderer.node.element.FormElementNode;
import org.omnaest.react4j.component.form.internal.renderer.node.element.FormElementNodeImpl;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.context.data.DataContext;
import org.omnaest.react4j.domain.context.document.Document;
import org.omnaest.react4j.domain.i18n.I18nText;
import org.omnaest.react4j.service.internal.handler.EventHandlerRegistry;
import org.omnaest.react4j.service.internal.service.LocalizedTextResolverService;

public class RangeFormElementImpl extends AbstractFormElementImpl<RangeFormElement> implements RangeFormElement
{
    private int     min      = 0;
    private int     max      = 100;
    private int     step     = 1;
    private boolean disabled = false;

    public RangeFormElementImpl(Function<Class<?>, String> identitiyProvider, LocalizedTextResolverService textResolver,
                                Function<String, I18nText> i18nTextMapper, EventHandlerRegistry eventHandlerRegistry,
                                Supplier<? extends DataContext> parentDataContext)
    {
        super(identitiyProvider, textResolver, i18nTextMapper, eventHandlerRegistry, parentDataContext);
    }

    @Override
    public RangeFormElement withMin(int min)
    {
        this.min = min;
        return this;
    }

    @Override
    public RangeFormElement withMax(int max)
    {
        this.max = max;
        return this;
    }

    @Override
    public RangeFormElement withStep(int step)
    {
        this.step = step;
        return this;
    }

    @Override
    public RangeFormElement withDisabled(boolean disabled)
    {
        this.disabled = disabled;
        return this;
    }

    @Override
    protected FormElementNode renderNode(FormElementNodeImpl node, Location location)
    {
        node.setType("RANGE")
            .setRange(new FormRangeNode().setMin(Integer.toString(this.min))
                                         .setMax(Integer.toString(this.max))
                                         .setStep(Integer.toString(this.step)))
            .setDisabled(this.disabled);
        return node;
    }

    @Override
    public RangeFormElement attachToField(Document.Field field)
    {
        this.field = field;
        this.document = field.getDocument();
        return this;
    }
}