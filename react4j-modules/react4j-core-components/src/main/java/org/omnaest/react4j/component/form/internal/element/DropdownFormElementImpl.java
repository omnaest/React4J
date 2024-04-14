package org.omnaest.react4j.component.form.internal.element;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.omnaest.react4j.component.form.Form.DropDownFormElement;
import org.omnaest.react4j.component.form.internal.element.DropdownFormElementImpl.DropDownData.DropDownDataBuilder;
import org.omnaest.react4j.component.form.internal.renderer.node.element.FormDropDownNode;
import org.omnaest.react4j.component.form.internal.renderer.node.element.FormDropDownNode.DropDownOptionNode;
import org.omnaest.react4j.component.form.internal.renderer.node.element.FormElementNode;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.context.data.DataContext;
import org.omnaest.react4j.domain.context.document.Document;
import org.omnaest.react4j.domain.i18n.I18nText;
import org.omnaest.react4j.service.internal.handler.EventHandlerRegistry;
import org.omnaest.react4j.service.internal.service.LocalizedTextResolverService;
import org.omnaest.utils.ConsumerUtils;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.Singular;

public class DropdownFormElementImpl extends AbstractFormElementImpl<DropDownFormElement> implements DropDownFormElement
{
    private DropDownDataBuilder data = DropDownData.builder();

    public DropdownFormElementImpl(Function<Class<?>, String> identitiyProvider, LocalizedTextResolverService textResolver,
                                   Function<String, I18nText> i18nTextMapper, EventHandlerRegistry eventHandlerRegistry,
                                   Supplier<? extends DataContext> parentDataContext)
    {
        super(identitiyProvider, textResolver, i18nTextMapper, eventHandlerRegistry, parentDataContext);
    }

    @Override
    protected FormElementNode renderNode(FormElementNode node, Location location)
    {
        DropDownData data = this.data.build();
        return node.toBuilder()
                   .type("DROPDOWN")
                   .dropDown(FormDropDownNode.builder()
                                             .options(data.getOptions()
                                                          .entrySet()
                                                          .stream()
                                                          .map(keyAndLabel -> DropDownOptionNode.builder()
                                                                                                .key(keyAndLabel.getKey())
                                                                                                .label(this.textResolver.apply(keyAndLabel.getValue()
                                                                                                                                          .getLabel(),
                                                                                                                               location))
                                                                                                .disabled(keyAndLabel.getValue()
                                                                                                                     .isDisabled())
                                                                                                .build())
                                                          .toList())
                                             .multiselect(data.isMultiselect())
                                             .build())
                   .build();
    }

    @Override
    public DropDownFormElement attachToField(Document.Field field)
    {
        this.field = field;
        this.document = field.getDocument();
        return this;
    }

    @Override
    public DropDownFormElement withOptions(Consumer<DropDownOptions> optionsConsumer)
    {
        Function<String, I18nText> i18nTextMapper = this.i18nTextMapper;
        DropDownDataBuilder data = this.data;
        DropDownOptions options = new DropDownOptionsImpl(i18nTextMapper, data);
        ConsumerUtils.consumeWithAndGet(options, optionsConsumer);
        return this;
    }

    @Data
    @Builder(toBuilder = true)
    public static class DropDownData
    {
        @Singular("addOption")
        private final Map<String, DropDownEntry> options;

        @Default
        private final boolean multiselect = false;
    }

    @Data
    @Builder
    public static class DropDownEntry
    {
        private I18nText label;

        @Default
        private boolean disabled = false;
    }

    private static class DropDownOptionsImpl implements DropDownOptions
    {
        private final Function<String, I18nText> i18nTextMapper;
        private final DropDownDataBuilder        data;

        public DropDownOptionsImpl(Function<String, I18nText> i18nTextMapper, DropDownDataBuilder data)
        {
            this.i18nTextMapper = i18nTextMapper;
            this.data = data;
        }

        @Override
        public DropDownOptions addOptions(Map<String, String> options)
        {
            Optional.ofNullable(options)
                    .orElse(Collections.emptyMap())
                    .forEach(this::addOption);
            return this;
        }

        @Override
        public DropDownOptions addOption(String key, String label)
        {
            return this.addOption(key, label, false);
        }

        @Override
        public DropDownOptions addDisabledOption(String key, String label)
        {
            return this.addOption(key, label, true);
        }

        @Override
        public DropDownOptions addOption(String key, String label, boolean disabled)
        {
            this.data.addOption(key, DropDownEntry.builder()
                                                  .label(this.i18nTextMapper.apply(label))
                                                  .disabled(disabled)
                                                  .build());
            return this;
        }

    }

    @Override
    public DropDownFormElement withMultiselectSupport(boolean enabled)
    {
        this.data.multiselect(enabled);
        return this;
    }

    @Override
    public DropDownFormElement withMultiselectSupport()
    {
        return this.withMultiselectSupport(true);
    }

}