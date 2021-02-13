package org.omnaest.react4j.service.internal.handler.domain;

public class ResponseBody extends EventBody
{

    @Override
    public ResponseBody setTarget(Target target)
    {
        super.setTarget(target);
        return this;
    }

    @Override
    public ResponseBody setDataWithContext(DataWithContext dataWithContext)
    {
        super.setDataWithContext(dataWithContext);
        return this;
    }

}
