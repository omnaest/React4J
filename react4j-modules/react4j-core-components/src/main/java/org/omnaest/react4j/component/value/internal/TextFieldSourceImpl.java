package org.omnaest.react4j.component.value.internal;

import org.omnaest.react4j.component.value.field.TextFieldSource;
import org.omnaest.react4j.component.value.internal.node.TextFieldNode;
import org.omnaest.react4j.component.value.node.ValueNode;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.context.document.Document.Field;
import org.omnaest.react4j.service.internal.service.LocalizedTextResolverService.LocationAwareTextResolver;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(staticName = "of")
public class TextFieldSourceImpl implements TextFieldSource
{
    private final Field field;

    @Override
    public ValueNode asNode(Location location, LocationAwareTextResolver textResolver)
    {
        return TextFieldNode.builder()
                            .contextId(this.field.getContext()
                                                 .getId(location))
                            .fieldName(this.field.getFieldName())
                            .build();
    }

}
