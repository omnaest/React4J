package org.omnaest.react4j.component.form.internal.element;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import org.omnaest.react4j.component.form.Form.FormElement;
import org.omnaest.react4j.component.form.internal.renderer.node.element.FormElementNode;
import org.omnaest.react4j.component.form.internal.renderer.node.element.FormElementNodeImpl;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.context.Context;
import org.omnaest.react4j.domain.context.data.DataContext;
import org.omnaest.react4j.domain.context.document.Document;
import org.omnaest.react4j.domain.context.document.Document.Field;
import org.omnaest.react4j.domain.i18n.I18nText;
import org.omnaest.react4j.service.internal.handler.EventHandlerRegistry;
import org.omnaest.react4j.service.internal.service.LocalizedTextResolverService;

public abstract class AbstractFormElementImpl<FE extends FormElement<?>> implements FormElement<FE>
{
    protected final LocalizedTextResolverService textResolver;
    protected final Function<String, I18nText>   i18nTextMapper;
    protected final EventHandlerRegistry         eventHandlerRegistry;

    protected Field                 field;
    protected Document              document;
    protected Supplier<DataContext> parentDataContext;

    private I18nText label;
    private I18nText description;
    private String   id;

    @SuppressWarnings("unchecked")
    protected AbstractFormElementImpl(Function<Class<?>, String> identitiyProvider, LocalizedTextResolverService textResolver,
                                      Function<String, I18nText> i18nTextMapper, EventHandlerRegistry eventHandlerRegistry,
                                      Supplier<? extends DataContext> parentDataContext)
    {
        this.id = identitiyProvider.apply(this.getClass());
        this.textResolver = textResolver;
        this.i18nTextMapper = i18nTextMapper;
        this.eventHandlerRegistry = eventHandlerRegistry;
        this.parentDataContext = (Supplier<DataContext>) parentDataContext;
    }

    @Override
    public FormElementNode render(Location parentLocation)
    {
        Context context = this.getEffectiveContext();
        Location location = Optional.ofNullable(this.getField())
                                    .map(field -> Location.of(Location.of(parentLocation, this.id), field))
                                    .orElse(Location.of(parentLocation, this.id));
        return this.renderNode(new FormElementNodeImpl().setField(this.getField())
                                                        .setContextId(context.getId(location))
                                                        .setLabel(this.textResolver.apply(this.label, location))
                                                        .setDescription(this.textResolver.apply(this.description, location)),
                               location);
    }

    protected Context getEffectiveContext()
    {
        return Optional.ofNullable(this.document)
                       .map(Document::getContext)
                       .orElseGet(this.parentDataContext::get);
    }

    protected abstract FormElementNode renderNode(FormElementNodeImpl node, Location location);

    private String getField()
    {
        return Optional.ofNullable(this.field)
                       .map(Field::getFieldName)
                       .orElse(null);
    }

    @SuppressWarnings("unchecked")
    @Override
    public FE withLabel(String label)
    {
        this.label = this.i18nTextMapper.apply(label);
        return (FE) this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public FE withDescription(String description)
    {
        this.description = this.i18nTextMapper.apply(description);
        return (FE) this;
    }

}