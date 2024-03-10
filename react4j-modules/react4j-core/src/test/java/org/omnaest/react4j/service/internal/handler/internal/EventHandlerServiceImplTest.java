package org.omnaest.react4j.service.internal.handler.internal;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;

import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import org.junit.Test;
import org.mockito.Mockito;
import org.omnaest.react4j.service.internal.handler.domain.DataWithContext;
import org.omnaest.react4j.service.internal.handler.domain.EventBody;
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
                     this.eventHandlerService.handleEvent(new EventBody(Target.empty(), new DataWithContext("contextId", Collections.emptyMap())));
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

    private EventHandlerServiceImpl createEventHandlerService()
    {
        return new EventHandlerServiceImpl()
        {
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
