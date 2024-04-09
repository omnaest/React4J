package org.omnaest.react4j.component.form.internal.element;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import org.omnaest.react4j.component.form.Form.FormElement;
import org.omnaest.react4j.component.form.internal.renderer.node.element.FormElementNode;
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

    private I18nText             label;
    private I18nText             description;
    private Optional<ColumnSpan> columnSpan = Optional.empty();
    private String               id;

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
        return this.renderNode(FormElementNode.builder()
                                              .field(this.getField())
                                              .contextId(context.getId(location))
                                              .label(this.textResolver.apply(this.label, location))
                                              .description(this.textResolver.apply(this.description, location))
                                              .colspan(this.columnSpan.map(ColumnSpan::ordinal)
                                                                      .map(value -> value + 1)
                                                                      .map(String::valueOf)
                                                                      .orElse(null))
                                              .build(),
                               location);
    }

    protected Context getEffectiveContext()
    {
        return Optional.ofNullable(this.document)
                       .map(Document::getContext)
                       .orElseGet(this.parentDataContext::get);
    }

    protected abstract FormElementNode renderNode(FormElementNode node, Location location);

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

    @SuppressWarnings("unchecked")
    @Override
    public FE withColumnSpan(ColumnSpan columnSpan)
    {
        this.columnSpan = Optional.ofNullable(columnSpan);
        return (FE) this;
    }

    @Override
    public FE withColumnSpan(int columnSpan)
    {
        if (columnSpan < 1)
        {
            throw new IllegalArgumentException("columnSpan must be greater then or at least 1");
        }
        else if (columnSpan > 12)
        {
            throw new IllegalArgumentException("columnSpan must be smaller then or exactly 12");
        }
        return this.withColumnSpan(ColumnSpan.values()[columnSpan - 1]);
    }

}