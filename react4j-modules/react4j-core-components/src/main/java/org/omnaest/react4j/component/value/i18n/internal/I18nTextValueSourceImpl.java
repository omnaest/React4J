package org.omnaest.react4j.component.value.i18n.internal;

import org.omnaest.react4j.component.value.i18n.I18nTextValueSource;
import org.omnaest.react4j.component.value.i18n.internal.node.I18nTextNode;
import org.omnaest.react4j.component.value.node.ValueNode;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.i18n.I18nText;
import org.omnaest.react4j.service.internal.service.LocalizedTextResolverService.LocationAwareTextResolver;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(staticName = "of")
public class I18nTextValueSourceImpl implements I18nTextValueSource
{
    private final I18nText text;

    @Override
    public ValueNode asNode(Location location, LocationAwareTextResolver textResolver)
    {
        return I18nTextNode.builder()
                           .value(textResolver.apply(this.text))
                           .build();
    }

}
