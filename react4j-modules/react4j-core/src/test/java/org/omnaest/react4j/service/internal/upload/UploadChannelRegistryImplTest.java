package org.omnaest.react4j.service.internal.upload;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.omnaest.react4j.component.form.upload.ByteArrayChannel;
import org.omnaest.react4j.component.form.upload.UploadChannel;
import org.omnaest.react4j.domain.Location;

public class UploadChannelRegistryImplTest
{
    private UploadChannelRegistryImpl registry = new UploadChannelRegistryImpl();

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

}
