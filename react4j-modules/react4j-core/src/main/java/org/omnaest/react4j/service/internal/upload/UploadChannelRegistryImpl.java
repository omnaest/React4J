package org.omnaest.react4j.service.internal.upload;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.omnaest.react4j.component.form.upload.UploadChannel;
import org.omnaest.react4j.domain.Location;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Self-bounding {@link UploadChannelRegistry}: an entry that has not been registered or looked up for longer than the configured time-to-live is removed
 * from every internal map, and the total entry count is additionally capped with least-recently-used eviction. Both bounds are enforced independently -
 * neither substitutes for the other, because a time-to-live bounds growth per unit time while a capacity cap bounds a burst inside one window.
 * <p>
 * {@code locationToUploadId} and {@code uploadIdToChannel} are two halves of one edge, and every removal path in this class scrubs both together (plus the
 * {@code uploadIdToLocationKey} reverse index that makes that possible when only the uploadId is known, e.g. during an eviction). Evicting only the
 * channel side would leave a location permanently mapped to an id that resolves to nothing.
 * <p>
 * Time is read from an injected {@link Clock} rather than {@link Clock#systemUTC()} directly, so tests can advance it deterministically without sleeping.
 *
 * @author omnaest
 */
@Service
public class UploadChannelRegistryImpl implements UploadChannelRegistry
{
    private final Object                     lock                    = new Object();

    private final Map<List<String>, String>  locationToUploadId      = new HashMap<>();
    private final Map<String, UploadChannel> uploadIdToChannel       = new HashMap<>();
    private final Map<String, List<String>>  uploadIdToLocationKey   = new HashMap<>();
    private final Map<String, Instant>       uploadIdToLastTouchedAt = new LinkedHashMap<>(16, 0.75f, true);

    private final Clock                      clock;
    private final Duration                   timeToLive;
    private final int                        maxEntries;

    /**
     * Spring-facing constructor. Deliberately does NOT depend on a {@link Clock} bean: publishing one would widen this library's published surface for
     * every one of React4J's consuming applications, none of which asked for it (plan-156 cliff X8). {@link Clock#systemUTC()} is supplied directly instead.
     */
    @Autowired
    public UploadChannelRegistryImpl(@Value("${react4j.upload.registry.time-to-live-minutes:30}") long timeToLiveMinutes, @Value("${react4j.upload.registry.max-entries:1000}") int maxEntries)
    {
        this(Clock.systemUTC(), timeToLiveMinutes, maxEntries);
    }

    /**
     * Test-facing constructor: takes an explicit {@link Clock} so the time-to-live sweep is deterministically testable without sleeping. Not
     * Spring-managed - {@link #UploadChannelRegistryImpl(long, int)} is the one Spring autowires.
     */
    public UploadChannelRegistryImpl(Clock clock, long timeToLiveMinutes, int maxEntries)
    {
        this.clock = clock;
        this.timeToLive = Duration.ofMinutes(timeToLiveMinutes);
        this.maxEntries = maxEntries;
    }

    @Override
    public String register(Location location, UploadChannel channel)
    {
        synchronized (this.lock)
        {
            this.sweepExpired();

            List<String> locationKey = location.get();
            String uploadId = this.locationToUploadId.get(locationKey);
            if (uploadId == null)
            {
                uploadId = UUID.randomUUID()
                               .toString();
                this.locationToUploadId.put(locationKey, uploadId);
                this.uploadIdToLocationKey.put(uploadId, locationKey);
            }
            this.uploadIdToChannel.put(uploadId, channel);
            this.touch(uploadId);
            this.evictExcessEntries();

            return uploadId;
        }
    }

    @Override
    public Optional<UploadChannel> lookup(String uploadId)
    {
        synchronized (this.lock)
        {
            this.sweepExpired();

            UploadChannel channel = this.uploadIdToChannel.get(uploadId);
            if (channel == null)
            {
                return Optional.empty();
            }

            this.touch(uploadId);
            return Optional.of(channel);
        }
    }

    /**
     * Periodic bound: ensures expired entries are reclaimed within a bounded time even if nothing ever registers or looks up again.
     */
    @Scheduled(initialDelay = 60_000, fixedDelay = 60_000)
    protected void sweepExpiredEntriesPeriodically()
    {
        synchronized (this.lock)
        {
            this.sweepExpired();
        }
    }

    /**
     * {@code uploadIdToLastTouchedAt} is an access-order map, so its iteration order is oldest-touched-first. Removing from the front while entries are
     * expired, then stopping at the first non-expired entry, is therefore a correct and minimal sweep.
     */
    private void sweepExpired()
    {
        Instant expiryThreshold = this.clock.instant()
                                            .minus(this.timeToLive);

        Iterator<Map.Entry<String, Instant>> iterator = this.uploadIdToLastTouchedAt.entrySet()
                                                                                    .iterator();
        while (iterator.hasNext())
        {
            Map.Entry<String, Instant> entry = iterator.next();
            if (entry.getValue()
                     .isBefore(expiryThreshold))
            {
                iterator.remove();
                this.removeFromChannelAndLocationMaps(entry.getKey());
            }
            else
            {
                break;
            }
        }
    }

    /**
     * Same access-order property makes the front of {@code uploadIdToLastTouchedAt} the least-recently-used entry.
     */
    private void evictExcessEntries()
    {
        while (this.uploadIdToLastTouchedAt.size() > this.maxEntries)
        {
            Iterator<String> iterator = this.uploadIdToLastTouchedAt.keySet()
                                                                    .iterator();
            if (!iterator.hasNext())
            {
                break;
            }
            String leastRecentlyUsedUploadId = iterator.next();
            iterator.remove();
            this.removeFromChannelAndLocationMaps(leastRecentlyUsedUploadId);
        }
    }

    private void removeFromChannelAndLocationMaps(String uploadId)
    {
        this.uploadIdToChannel.remove(uploadId);
        List<String> locationKey = this.uploadIdToLocationKey.remove(uploadId);
        if (locationKey != null)
        {
            this.locationToUploadId.remove(locationKey);
        }
    }

    private void touch(String uploadId)
    {
        this.uploadIdToLastTouchedAt.put(uploadId, this.clock.instant());
    }

}
