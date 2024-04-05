package org.omnaest.react4j.component.ankerbutton.internal.factory;

import org.omnaest.react4j.component.ankerbutton.AnkerButton;
import org.omnaest.react4j.component.ankerbutton.internal.AnkerButtonImpl;
import org.omnaest.react4j.domain.support.CustomUIComponentFactory;
import org.omnaest.react4j.service.internal.component.ComponentContext;
import org.springframework.stereotype.Component;

@Component
public class AnkerButtonFactory implements CustomUIComponentFactory<AnkerButton>
{
    @Override
    public Class<AnkerButton> getType()
    {
        return AnkerButton.class;
    }

    @Override
    public AnkerButton newInstance(ComponentContext context)
    {
        return new AnkerButtonImpl(context);
    }
}
