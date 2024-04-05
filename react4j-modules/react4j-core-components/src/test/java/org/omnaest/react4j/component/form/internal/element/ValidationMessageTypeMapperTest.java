package org.omnaest.react4j.component.form.internal.element;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.omnaest.react4j.component.form.Form.InputFormElement.ValidationMessageType;

public class ValidationMessageTypeMapperTest
{

    @Test
    public void testMapToNodeType()
    {
        List.of(ValidationMessageType.values())
            .forEach(validationMessageType -> assertEquals(validationMessageType.name(), ValidationMessageTypeMapper.mapToNodeType(validationMessageType)
                                                                                                                    .name()));
    }

}
