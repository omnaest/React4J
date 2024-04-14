package org.omnaest.react4j.component.form.internal.element;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.commons.lang3.StringUtils;
import org.omnaest.react4j.component.form.Form.CheckboxFormElement;
import org.omnaest.react4j.component.form.internal.element.CheckboxFormElementImpl.CheckboxData.CheckboxDataBuilder;
import org.omnaest.react4j.component.form.internal.renderer.node.element.FormElementNode;
import org.omnaest.react4j.component.form.internal.renderer.node.element.FormElementNode.FormCheckboxNode;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.context.data.DataContext;
import org.omnaest.react4j.domain.context.document.Document;
import org.omnaest.react4j.domain.i18n.I18nText;
import org.omnaest.react4j.service.internal.component.uicontext.UIContextManager;
import org.omnaest.react4j.service.internal.handler.EventHandlerRegistry;
import org.omnaest.react4j.service.internal.service.LocalizedTextResolverService;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;

public class CheckboxFormElementImpl extends AbstractFormElementImpl<CheckboxFormElement> implements CheckboxFormElement
{
    private final UIContextManager uiContextManager;

    private CheckboxDataBuilder data = CheckboxData.builder();

    public CheckboxFormElementImpl(Function<Class<?>, String> identitiyProvider, LocalizedTextResolverService textResolver,
                                   Function<String, I18nText> i18nTextMapper, EventHandlerRegistry eventHandlerRegistry,
                                   Supplier<? extends DataContext> parentDataContext, UIContextManager uiContextManager)
    {
        super(identitiyProvider, textResolver, i18nTextMapper, eventHandlerRegistry, parentDataContext);
        this.uiContextManager = uiContextManager;
    }

    @Override
    protected FormElementNode renderNode(FormElementNode node, Location location)
    {
        CheckboxData data = this.data.build();
        data.getInitialValue()
            .ifPresent(value -> this.uiContextManager.updateInitialDataFieldValue(this.field, value));
        return node.toBuilder()
                   .type("FORMCHECKBOX")
                   .disabled(data.isDisabled())
                   .checkbox(FormCheckboxNode.builder()
                                             .checkboxType(StringUtils.lowerCase(data.getType()
                                                                                     .name()))
                                             .build())
                   .build();
    }

    @Override
    public CheckboxFormElement attachToField(Document.Field field)
    {
        this.field = field;
        this.document = field.getDocument();
        return this;
    }

    @Data
    @Builder(toBuilder = true)
    public static class CheckboxData
    {
        @Default
        private CheckboxType type = CheckboxType.REGULAR;

        @Default
        private boolean disabled = false;

        @Default
        private Optional<Boolean> initialValue = Optional.empty();
    }

    @Override
    public CheckboxFormElement withInitialValue(boolean initialValue)
    {
        this.data.initialValue(Optional.of(initialValue));
        return this;
    }

    @Override
    public CheckboxFormElement withDisabled(boolean disabled)
    {
        this.data.disabled(disabled);
        return this;
    }

    @Override
    public CheckboxFormElement withType(CheckboxType checkboxType)
    {
        if (checkboxType != null)
        {
            this.data.type(checkboxType);
        }
        return this;
    }

}