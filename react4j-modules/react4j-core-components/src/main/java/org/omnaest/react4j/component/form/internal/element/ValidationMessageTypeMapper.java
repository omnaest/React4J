package org.omnaest.react4j.component.form.internal.element;

import java.util.Map;

import org.omnaest.react4j.component.form.Form.InputFormElement.ValidationMessageType;
import org.omnaest.react4j.component.form.internal.renderer.node.element.validation.ValidationMessageNode;
import org.omnaest.utils.MapUtils;

public class ValidationMessageTypeMapper
{
    private static Map<ValidationMessageType, ValidationMessageNode.ValidationMessageType> typeToNodeType = MapUtils.builder()
                                                                                                                    .put(ValidationMessageType.VALID,
                                                                                                                         ValidationMessageNode.ValidationMessageType.VALID)
                                                                                                                    .put(ValidationMessageType.INVALID,
                                                                                                                         ValidationMessageNode.ValidationMessageType.INVALID)
                                                                                                                    .build();

    public static ValidationMessageNode.ValidationMessageType mapToNodeType(ValidationMessageType validationMessageType)
    {
        return typeToNodeType.get(validationMessageType);
    }
}