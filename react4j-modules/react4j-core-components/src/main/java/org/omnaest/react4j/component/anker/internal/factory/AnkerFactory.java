package org.omnaest.react4j.component.anker.internal.factory;

import org.omnaest.react4j.component.anker.Anker;
import org.omnaest.react4j.component.anker.internal.AnkerImpl;
import org.omnaest.react4j.domain.support.CustomUIComponentFactory;
import org.omnaest.react4j.service.internal.component.ComponentContext;
import org.springframework.stereotype.Component;

@Component
public class AnkerFactory implements CustomUIComponentFactory<Anker>
{
    @Override
    public Anker newInstance(ComponentContext context)
    {
        return new AnkerImpl(context);
    }

    @Override
    public Class<Anker> getType()
    {
        return Anker.class;
    }

}
