package org.omnaest.react4j.component.form.internal.element;

import java.util.function.Function;
import java.util.function.Supplier;

import org.omnaest.react4j.component.form.Form.InputFormElement;
import org.omnaest.react4j.component.form.internal.renderer.node.element.FormElementNode;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.context.data.DataContext;
import org.omnaest.react4j.domain.context.document.Document;
import org.omnaest.react4j.domain.i18n.I18nText;
import org.omnaest.react4j.service.internal.handler.EventHandlerRegistry;
import org.omnaest.react4j.service.internal.service.LocalizedTextResolverService;

public class InputFormElementImpl extends AbstractFormElementImpl<InputFormElement> implements InputFormElement
{
    private I18nText placeholder;

    public InputFormElementImpl(Function<Class<?>, String> identitiyProvider, LocalizedTextResolverService textResolver,
                                Function<String, I18nText> i18nTextMapper, EventHandlerRegistry eventHandlerRegistry,
                                Supplier<? extends DataContext> parentDataContext)
    {
        super(identitiyProvider, textResolver, i18nTextMapper, eventHandlerRegistry, parentDataContext);
    }

    @Override
    public InputFormElement withPlaceholder(String placeholder)
    {
        this.placeholder = this.i18nTextMapper.apply(placeholder);
        return this;
    }

    @Override
    protected FormElementNode renderNode(FormElementNode node, Location location)
    {
        return node.toBuilder()
                   .type("INPUT")
                   .placeholder(this.textResolver.apply(this.placeholder, location))
                   .build();
    }

    @Override
    public InputFormElement attachToField(Document.Field field)
    {
        this.field = field;
        this.document = field.getDocument();
        return this;
    }

}