package org.omnaest.react4j.service.internal.handler.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import org.junit.Test;
import org.mockito.Mockito;
import org.omnaest.react4j.service.internal.handler.domain.DataEventHandler;
import org.omnaest.react4j.service.internal.handler.domain.DataWithContext;
import org.omnaest.react4j.service.internal.handler.domain.EventBody;
import org.omnaest.react4j.service.internal.handler.domain.ResponseBody;
import org.omnaest.react4j.service.internal.handler.domain.Target;
import org.omnaest.react4j.service.internal.handler.domain.TargetNode;
import org.omnaest.react4j.service.internal.rerenderer.RerenderingService;

public class EventHandlerServiceImplTest
{
    private EventHandlerServiceImpl eventHandlerService          = this.createEventHandlerService();
    private AtomicInteger           eventHandlerExecutionCounter = new AtomicInteger();

    @Test
    public void testRegisterDataEventHandler() throws Exception
    {
        this.simulateUIRerendering();
        IntStream.range(1, 10)
                 .forEach(ii ->
                 {
                     this.eventHandlerService.handleEvent(new EventBody(Target.empty(),
                                                                        new DataWithContext("contextId", Collections.emptyMap(), Collections.emptyMap())));
                     this.simulateUIRerendering();
                     assertEquals(ii, this.eventHandlerExecutionCounter.get());
                 });
    }

    private void simulateUIRerendering()
    {
        this.eventHandlerService.executeTransactionalAndPublishStagingHandlers(() ->
        {
            this.eventHandlerService.registerEventHandler(Target.empty(), this.eventHandlerExecutionCounter::incrementAndGet);
            return null;
        });
    }

    /**
     * Plan-12 ordering test: proves that {@link EventHandlerServiceImpl#handleEvent(EventBody)} invokes the matched
     * click/data handler BEFORE the render that produces the response's {@link TargetNode} - i.e. the returned node
     * reflects server state mutated inside the handler, not the pre-handler state.
     */
    @Test
    public void testHandlerRunsBeforeResponseRender() throws Exception
    {
        Target target = Target.empty();
        AtomicBoolean handlerRan = new AtomicBoolean(false);
        List<Boolean> handlerRanFlagAtEachRender = Collections.synchronizedList(new ArrayList<>());

        EventHandlerServiceImpl service = new EventHandlerServiceImpl() {
            {
                this.rerenderingService = Mockito.mock(RerenderingService.class);
                Mockito.when(this.rerenderingService.rerenderTargetNode(any(), any()))
                       .then(invocation ->
                       {
                           handlerRanFlagAtEachRender.add(handlerRan.get());
                           return Optional.of(new TargetNode(Target.empty(), null));
                       });
            }
        };

        // Simulate the initial page render that registers the target's handler (mirrors a real component tree build).
        service.executeTransactionalAndPublishStagingHandlers(() ->
        {
            service.registerDataEventHandler(target, (data, internalData) ->
            {
                handlerRan.set(true);
                return DataEventHandler.MappedData.builder()
                                                  .data(data)
                                                  .internalData(internalData)
                                                  .build();
            });
            return null;
        });

        Optional<ResponseBody> response = service.handleEvent(new EventBody(target,
                                                                            new DataWithContext("contextId", Collections.emptyMap(), Collections.emptyMap())));

        assertTrue(response.isPresent());
        assertEquals(2, handlerRanFlagAtEachRender.size());
        assertFalse("first render (handler registration pass) must run before the handler", handlerRanFlagAtEachRender.get(0));
        assertTrue("second render (response node) must run AFTER the handler", handlerRanFlagAtEachRender.get(1));
    }

    /**
     * Regression guard (plan-12 2d): an event targeting nothing with a registered handler yields
     * {@link Optional#empty()}, unchanged by the two-pass render fix.
     */
    @Test
    public void testHandleEventWithoutMatchingHandlerReturnsEmpty() throws Exception
    {
        EventHandlerServiceImpl service = new EventHandlerServiceImpl() {
            {
                this.rerenderingService = Mockito.mock(RerenderingService.class);
                Mockito.when(this.rerenderingService.rerenderTargetNode(any(), any()))
                       .thenReturn(Optional.of(new TargetNode(Target.empty(), null)));
            }
        };

        Optional<ResponseBody> response = service.handleEvent(new EventBody(Target.empty(),
                                                                            new DataWithContext("contextId", Collections.emptyMap(), Collections.emptyMap())));

        assertFalse(response.isPresent());
    }

    /**
     * Regression guard (plan-12 2c): the response data reflects a handler's setFieldValue mutation, i.e. the
     * post-handler MappedData - not the raw incoming data - drives the response body.
     */
    @Test
    public void testResponseDataReflectsHandlerFieldMutation() throws Exception
    {
        Target target = Target.empty();

        EventHandlerServiceImpl service = new EventHandlerServiceImpl() {
            {
                this.rerenderingService = Mockito.mock(RerenderingService.class);
                Mockito.when(this.rerenderingService.rerenderTargetNode(any(), any()))
                       .thenReturn(Optional.of(new TargetNode(Target.empty(), null)));
            }
        };

        service.executeTransactionalAndPublishStagingHandlers(() ->
        {
            service.registerDataEventHandler(target, (data, internalData) -> DataEventHandler.MappedData.builder()
                                                                                                        .data(data.setFieldValue("greeting", "hello"))
                                                                                                        .internalData(internalData)
                                                                                                        .build());
            return null;
        });

        Optional<ResponseBody> response = service.handleEvent(new EventBody(target,
                                                                            new DataWithContext("contextId", new HashMap<>(), Collections.emptyMap())));

        assertTrue(response.isPresent());
        assertEquals("hello", response.get()
                                      .getDataWithContext()
                                      .getData()
                                      .get("greeting"));
    }

    /**
     * Plan-14 regression guard: proves {@link EventHandlerServiceImpl#registerDataEventHandler(Target, DataEventHandler)}
     * is idempotent per render cycle, so a Target whose owning tree is rendered more than once within a single
     * {@link EventHandlerServiceImpl#handleEvent(EventBody)} call (as caused by plan-12's two-pass render combined with
     * plan-13's per-render content rebuild) still has its handler invoked exactly once - not once per duplicate
     * registration.
     */
    @Test
    public void testHandlerInvokedExactlyOncePerEventDespiteRepeatedTreeRenders() throws Exception
    {
        Target target = Target.empty();
        AtomicInteger sideEffectCounter = new AtomicInteger();

        DataEventHandler handler = (data, internalData) ->
        {
            sideEffectCounter.incrementAndGet();
            return DataEventHandler.MappedData.builder()
                                              .data(data)
                                              .internalData(internalData)
                                              .build();
        };

        EventHandlerServiceImpl service = new EventHandlerServiceImpl() {
            {
                this.rerenderingService = Mockito.mock(RerenderingService.class);
                Mockito.when(this.rerenderingService.rerenderTargetNode(any(), any()))
                       .then(invocation ->
                       {
                           // Simulate the button's owning tree being rendered more than once within a single
                           // render pass (plan-12 two-pass render + plan-13 per-render content rebuild): the
                           // SAME logical handler gets re-registered for the SAME target more than once per call.
                           this.registerDataEventHandler(target, handler);
                           this.registerDataEventHandler(target, handler);
                           return Optional.of(new TargetNode(Target.empty(), null));
                       });
            }
        };

        // Cross-request leftover: register the handler once in a prior GET-like transactional cycle, mirroring a
        // page render that happened before this click's event. Targets not re-rendered this cycle must still be
        // carried forward (unchanged merge behavior) - this registration must NOT stack with the re-renders below.
        service.executeTransactionalAndPublishStagingHandlers(() ->
        {
            service.registerDataEventHandler(target, handler);
            return null;
        });

        Optional<ResponseBody> response = service.handleEvent(new EventBody(target,
                                                                            new DataWithContext("contextId", Collections.emptyMap(), Collections.emptyMap())));

        assertTrue(response.isPresent());
        assertEquals(1, sideEffectCounter.get());
    }

    /**
     * Plan-29 Bug 2b regression guard: registering a null {@link EventHandler} (e.g. a Button with no configured
     * {@code onClick}) must be a safe no-op - never wrapped, never registered, never invoked - so a subsequent
     * {@link EventHandlerServiceImpl#handleEvent(EventBody)} for that target does not NPE.
     */
    @Test
    public void testRegisterEventHandlerWithNullHandlerIsSafeNoOp() throws Exception
    {
        Target target = Target.empty();

        EventHandlerServiceImpl service = new EventHandlerServiceImpl() {
            {
                this.rerenderingService = Mockito.mock(RerenderingService.class);
                Mockito.when(this.rerenderingService.rerenderTargetNode(any(), any()))
                       .thenReturn(Optional.of(new TargetNode(Target.empty(), null)));
            }
        };

        service.executeTransactionalAndPublishStagingHandlers(() ->
        {
            service.registerEventHandler(target, null);
            return null;
        });

        Optional<ResponseBody> response = service.handleEvent(new EventBody(target,
                                                                            new DataWithContext("contextId", Collections.emptyMap(), Collections.emptyMap())));

        assertFalse(response.isPresent());
    }

    private EventHandlerServiceImpl createEventHandlerService()
    {
        return new EventHandlerServiceImpl() {
            {
                this.rerenderingService = Mockito.mock(RerenderingService.class);
                Mockito.when(this.rerenderingService.rerenderTargetNode(any(), any()))
                       .then(invocation ->
                       {
                           this.registerEvent();
                           return Optional.of(new TargetNode(Target.empty(), null));
                       });

            }

            private void registerEvent()
            {
                this.registerEventHandler(Target.empty(), EventHandlerServiceImplTest.this.eventHandlerExecutionCounter::incrementAndGet);
            }
        };
    }

}
