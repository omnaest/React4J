package org.omnaest.react4j.service.internal.upload;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.omnaest.react4j.component.form.upload.ByteArrayChannel;
import org.omnaest.react4j.component.form.upload.UploadChannel;
import org.omnaest.react4j.domain.Location;

public class UploadChannelRegistryImplTest
{
    private UploadChannelRegistryImpl registry = new UploadChannelRegistryImpl(Clock.systemUTC(), 30, 1000);

    @Test
    public void testRegisterIsIdempotentPerLocation()
    {
        Location location = Location.of(Location.of("form"), "fileUpload");
        UploadChannel channel = ByteArrayChannel.create();

        String firstId = this.registry.register(location, channel);
        String secondId = this.registry.register(location, channel);

        assertEquals(firstId, secondId);
    }

    @Test
    public void testLookupHit()
    {
        Location location = Location.of(Location.of("form"), "fileUpload");
        UploadChannel channel = ByteArrayChannel.create();

        String uploadId = this.registry.register(location, channel);

        assertTrue(this.registry.lookup(uploadId)
                                .isPresent());
        assertEquals(channel, this.registry.lookup(uploadId)
                                           .get());
    }

    @Test
    public void testLookupMiss()
    {
        assertFalse(this.registry.lookup("does-not-exist")
                                 .isPresent());
    }

    @Test
    public void testDifferentLocationsYieldDifferentIds()
    {
        Location locationA = Location.of(Location.of("pageA"), "fileUpload");
        Location locationB = Location.of(Location.of("pageB"), "fileUpload");

        String idA = this.registry.register(locationA, ByteArrayChannel.create());
        String idB = this.registry.register(locationB, ByteArrayChannel.create());

        assertNotEquals(idA, idB);
    }

    @Test
    public void testUploadIdFromOneContextIsNotResolvableAsBelongingToAnotherContextsLocation()
    {
        Location contextALocation = Location.of(Location.of("contextA"), "fileUpload");
        Location contextBLocation = Location.of(Location.of("contextB"), "fileUpload");

        String idFromContextA = this.registry.register(contextALocation, ByteArrayChannel.create());

        // Re-registering context B's (different) location must not collide with or return context A's id.
        String idFromContextB = this.registry.register(contextBLocation, ByteArrayChannel.create());

        assertNotEquals(idFromContextA, idFromContextB);
        assertTrue(this.registry.lookup(idFromContextA)
                                .isPresent());
        assertTrue(this.registry.lookup(idFromContextB)
                                .isPresent());
    }

    @Test
    public void testExpiredRegistrationIsGoneFromBothMaps()
    {
        MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
        UploadChannelRegistryImpl registryWithControllableClock = new UploadChannelRegistryImpl(clock, 30, 1000);
        Location location = Location.of(Location.of("form"), "fileUpload");

        String oldUploadId = registryWithControllableClock.register(location, ByteArrayChannel.create());

        clock.advanceBy(Duration.ofMinutes(31));

        // Half 1: the expired id no longer resolves.
        assertFalse(registryWithControllableClock.lookup(oldUploadId)
                                                 .isPresent());

        // Half 2: locationToUploadId was scrubbed too, not just the channel map - re-registering the SAME location mints a DIFFERENT id.
        String newUploadId = registryWithControllableClock.register(location, ByteArrayChannel.create());
        assertNotEquals(oldUploadId, newUploadId);
    }

    @Test
    public void testReRegisteringWithinTimeToLiveStillReturnsSameId()
    {
        MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
        UploadChannelRegistryImpl registryWithControllableClock = new UploadChannelRegistryImpl(clock, 30, 1000);
        Location location = Location.of(Location.of("form"), "fileUpload");
        UploadChannel channel = ByteArrayChannel.create();

        String firstId = registryWithControllableClock.register(location, channel);
        clock.advanceBy(Duration.ofMinutes(29));
        String secondId = registryWithControllableClock.register(location, channel);

        assertEquals(firstId, secondId);
    }

    @Test
    public void testCapacityBoundEvictsLeastRecentlyUsedEntryAndSurvivesMoreRecentOnes()
    {
        MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
        UploadChannelRegistryImpl registryWithSmallCapacity = new UploadChannelRegistryImpl(clock, 30, 2);

        Location locationA = Location.of(Location.of("pageA"), "fileUpload");
        Location locationB = Location.of(Location.of("pageB"), "fileUpload");
        Location locationC = Location.of(Location.of("pageC"), "fileUpload");

        String idA = registryWithSmallCapacity.register(locationA, ByteArrayChannel.create());
        String idB = registryWithSmallCapacity.register(locationB, ByteArrayChannel.create());

        // Touch A again so B becomes the least-recently-used entry.
        registryWithSmallCapacity.lookup(idA);

        // Capacity is 2; adding a third entry must evict the LRU one (B), not A.
        String idC = registryWithSmallCapacity.register(locationC, ByteArrayChannel.create());

        assertTrue(registryWithSmallCapacity.lookup(idA)
                                            .isPresent(),
                   "recently used entry A must survive the eviction");
        assertTrue(registryWithSmallCapacity.lookup(idC)
                                            .isPresent(),
                   "just-registered entry C must be present");
        assertFalse(registryWithSmallCapacity.lookup(idB)
                                             .isPresent(),
                    "least-recently-used entry B must have been evicted");
    }

}
