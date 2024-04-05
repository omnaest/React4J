package org.omnaest.react4j.component.form.internal.factory;

import org.omnaest.react4j.component.form.Form;
import org.omnaest.react4j.component.form.internal.FormImpl;
import org.omnaest.react4j.domain.support.CustomUIComponentFactory;
import org.omnaest.react4j.service.internal.component.ComponentContext;
import org.springframework.stereotype.Component;

@Component
public class FormFactory implements CustomUIComponentFactory<Form>
{

    @Override
    public Class<Form> getType()
    {
        return Form.class;
    }

    @Override
    public Form newInstance(ComponentContext context)
    {
        return new FormImpl(context);
    }

}
