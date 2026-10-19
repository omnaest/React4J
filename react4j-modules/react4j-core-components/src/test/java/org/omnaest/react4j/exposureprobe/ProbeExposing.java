package org.omnaest.react4j.exposureprobe;

import java.util.List;
import java.util.function.Supplier;

import org.omnaest.react4j.exposureprobe.internal.HiddenProbeType;

/**
 * Fixture for {@code PublicApiInternalTypeExposureTest}: mentions a hidden type at every position a type can occupy
 * on a public signature, and also in positions that are not published surface.
 */
public class ProbeExposing<T extends HiddenProbeType> extends ProbeBase<HiddenProbeType> implements Supplier<HiddenProbeType>
{
    public HiddenProbeType  publicField;
    @SuppressWarnings("unused")
    private HiddenProbeType privateField;
    HiddenProbeType         packagePrivateField;

    public ProbeExposing(HiddenProbeType constructorParameter)
    {
    }

    public void parameter(HiddenProbeType parameter)
    {
    }

    public HiddenProbeType returnType()
    {
        return null;
    }

    public void genericArgument(List<HiddenProbeType> parameter)
    {
    }

    public void wildcardBound(List<? extends HiddenProbeType> parameter)
    {
    }

    public void arrayParameter(HiddenProbeType[] parameter)
    {
    }

    protected void protectedMethod(HiddenProbeType parameter)
    {
    }

    public void typeVariableBound(T parameter)
    {
    }

    void packagePrivateMethod(HiddenProbeType parameter)
    {
    }

    @SuppressWarnings("unused")
    private void privateMethod(HiddenProbeType parameter)
    {
    }

    @Override
    public HiddenProbeType get()
    {
        return null;
    }
}
