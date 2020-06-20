package org.omnaest.react.internal.handler.domain;

public class ResponseBody extends EventBody
{

    @Override
    protected ResponseBody setTarget(Target target)
    {
        super.setTarget(target);
        return this;
    }

    @Override
    protected ResponseBody setDataWithContext(DataWithContext dataWithContext)
    {
        super.setDataWithContext(dataWithContext);
        return this;
    }

}
