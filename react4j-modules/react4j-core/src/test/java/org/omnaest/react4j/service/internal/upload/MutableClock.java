package org.omnaest.react4j.service.internal.upload;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

/**
 * A {@link Clock} whose {@link #instant()} can be advanced explicitly, so time-based behaviour (a time-to-live sweep) can be exercised deterministically
 * in a test without sleeping.
 */
public class MutableClock extends Clock
{
    private Instant      instant;
    private final ZoneId zone;

    public MutableClock(Instant instant, ZoneId zone)
    {
        this.instant = instant;
        this.zone = zone;
    }

    @Override
    public ZoneId getZone()
    {
        return this.zone;
    }

    @Override
    public Clock withZone(ZoneId zone)
    {
        return new MutableClock(this.instant, zone);
    }

    @Override
    public Instant instant()
    {
        return this.instant;
    }

    public void advanceBy(Duration duration)
    {
        this.instant = this.instant.plus(duration);
    }

}
