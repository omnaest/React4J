package org.omnaest.react4j.exposureprobe;

import org.omnaest.react4j.exposureprobe.internal.HiddenProbeType;

/**
 * Fixture for {@code PublicApiInternalTypeExposureTest}: a non-public type is no published surface, however much it
 * mentions.
 */
class ProbeNonPublic
{
    public void parameter(HiddenProbeType parameter)
    {
    }
}
