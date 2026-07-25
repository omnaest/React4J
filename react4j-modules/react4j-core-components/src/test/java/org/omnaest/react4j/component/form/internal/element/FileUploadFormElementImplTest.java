package org.omnaest.react4j.component.form.internal.element;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.Test;
import org.omnaest.react4j.component.form.Form.FileUploadFormElement;
import org.omnaest.react4j.component.form.internal.renderer.node.element.FormElementNode;
import org.omnaest.react4j.component.form.upload.ByteArrayChannel;
import org.omnaest.react4j.component.form.upload.UploadChannel;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.context.data.Data;
import org.omnaest.react4j.domain.context.data.DataContext;
import org.omnaest.react4j.domain.context.data.DataContext.PersistResult;
import org.omnaest.react4j.domain.context.data.TypedDataContext;
import org.omnaest.react4j.service.internal.handler.EventHandlerRegistry;
import org.omnaest.react4j.service.internal.handler.domain.DataEventHandler;
import org.omnaest.react4j.service.internal.handler.domain.EventHandler;
import org.omnaest.react4j.service.internal.handler.domain.Target;
import org.omnaest.react4j.service.internal.upload.UploadChannelRegistry;

public class FileUploadFormElementImplTest
{
    private FakeEventHandlerRegistry  eventHandlerRegistry  = new FakeEventHandlerRegistry();
    private FakeUploadChannelRegistry uploadChannelRegistry = new FakeUploadChannelRegistry();

    private FileUploadFormElementImpl newElement()
    {
        return new FileUploadFormElementImpl(type -> type.getSimpleName()
                                                         .toLowerCase(),
                                             (text, location) -> null, text -> null, this.eventHandlerRegistry, FakeDataContext::new,
                                             this.uploadChannelRegistry);
    }

    @Test
    public void testRenderProducesFileUploadNodeWithExpectedFields()
    {
        FileUploadFormElementImpl element = this.newElement();
        ByteArrayChannel channel = ByteArrayChannel.create();
        element.withUploadChannel(channel)
               .withAccept("image/*");

        FormElementNode node = element.render(Location.of("form"));

        assertEquals("FILE_UPLOAD", node.getType());
        assertNotNull(node.getFileUpload());
        assertEquals("ui/upload", node.getFileUpload()
                                      .getUploadUrl());
        assertNotNull(node.getFileUpload()
                          .getUploadId());
        assertEquals("image/*", node.getFileUpload()
                                    .getAccept());
        assertEquals(channel.maxSizeBytes(), node.getFileUpload()
                                                 .getMaxSize());
        assertNotNull(node.getFileUpload()
                          .getOnComplete());
        assertNotNull(node.getContextId());
    }

    @Test
    public void testRegistryContainsChannelUnderReturnedUploadId()
    {
        FileUploadFormElementImpl element = this.newElement();
        ByteArrayChannel channel = ByteArrayChannel.create();
        element.withUploadChannel(channel);

        FormElementNode node = element.render(Location.of("form"));

        Optional<UploadChannel> registered = this.uploadChannelRegistry.lookup(node.getFileUpload()
                                                                                   .getUploadId());
        assertTrue(registered.isPresent());
        assertEquals(channel, registered.get());
    }

    @Test
    public void testRerenderKeepsSameUploadId()
    {
        FileUploadFormElementImpl element = this.newElement();
        element.withUploadChannel(ByteArrayChannel.create());

        FormElementNode firstRender = element.render(Location.of("form"));
        FormElementNode secondRender = element.render(Location.of("form"));

        assertEquals(firstRender.getFileUpload()
                                .getUploadId(),
                     secondRender.getFileUpload()
                                 .getUploadId());
    }

    private static class FakeEventHandlerRegistry implements EventHandlerRegistry
    {
        private final Map<Target, DataEventHandler> handlers = new HashMap<>();

        @Override
        public void registerEventHandler(Target target, EventHandler eventHandler)
        {
            // not exercised by this test
        }

        @Override
        public void registerDataEventHandler(Target target, DataEventHandler eventHandler)
        {
            this.handlers.put(target, eventHandler);
        }
    }

    private static class FakeUploadChannelRegistry implements UploadChannelRegistry
    {
        private final Map<List<String>, String>  locationToUploadId = new ConcurrentHashMap<>();
        private final Map<String, UploadChannel> uploadIdToChannel  = new ConcurrentHashMap<>();

        @Override
        public String register(Location location, UploadChannel channel)
        {
            String uploadId = this.locationToUploadId.computeIfAbsent(location.get(), key -> UUID.randomUUID()
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

    private static class FakeDataContext implements DataContext
    {
        @Override
        public String getId(Location location)
        {
            return "ctx-" + String.join("/", location.get());
        }

        @Override
        public Optional<DataContext> asDataContext()
        {
            return Optional.of(this);
        }

        @Override
        public PersistResult persist(Data data)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public Selector selector()
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public View view()
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> TypedDataContext<T> asTypedDataContext(Class<T> type)
        {
            throw new UnsupportedOperationException();
        }
    }

}
