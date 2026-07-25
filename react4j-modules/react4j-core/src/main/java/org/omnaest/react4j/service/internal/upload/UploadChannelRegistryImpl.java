package org.omnaest.react4j.service.internal.upload;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.omnaest.react4j.component.form.upload.UploadChannel;
import org.omnaest.react4j.domain.Location;
import org.springframework.stereotype.Service;

@Service
public class UploadChannelRegistryImpl implements UploadChannelRegistry
{
    private final Map<List<String>, String>  locationToUploadId = new ConcurrentHashMap<>();
    private final Map<String, UploadChannel> uploadIdToChannel  = new ConcurrentHashMap<>();

    @Override
    public String register(Location location, UploadChannel channel)
    {
        List<String> key = location.get();
        String uploadId = this.locationToUploadId.computeIfAbsent(key, k -> UUID.randomUUID()
                                                                                .toString());
        this.uploadIdToChannel.put(uploadId, channel);
        return uploadId;
    }

    @Override
    public Optional<UploadChannel> lookup(String uploadId)
    {
        return Optional.ofNullable(this.uploadIdToChannel.get(uploadId));
    }

}
