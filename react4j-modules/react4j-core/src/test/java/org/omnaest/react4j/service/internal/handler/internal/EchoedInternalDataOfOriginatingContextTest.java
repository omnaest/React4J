package org.omnaest.react4j.service.internal.handler.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.omnaest.react4j.domain.context.data.Data;
import org.omnaest.react4j.service.internal.handler.domain.DataEventHandler;
import org.omnaest.react4j.service.internal.handler.domain.DataWithContext;
import org.omnaest.react4j.service.internal.handler.domain.EventBody;
import org.omnaest.react4j.service.internal.handler.domain.ResponseBody;
import org.omnaest.react4j.service.internal.handler.domain.Target;
import org.omnaest.react4j.service.internal.handler.domain.TargetNode;
import org.omnaest.react4j.service.internal.rerenderer.RerenderingService;

/**
 * The response's {@code dataWithContexts} must carry what the handler wrote into the ORIGINATING context's
 * internal data, and must leave every other context's internal data exactly as the request sent it.
 *
 * <h2>The defect this exists for</h2>
 * The client applies {@code dataWithContexts} when it is non-empty and uses {@code dataWithContext} only as a
 * fallback. {@code EventHandlerServiceImpl#echoEveryContext} used to emit the REQUEST's internal data for every
 * context, the originating one included, so the handler's {@code validationFeedback} (written through
 * {@code Form.Messaging#addValidationMessage}, kept in internal data) never reached the browser: the click round
 * trip showed no {@code is-invalid} / {@code is-valid} state and no feedback text.
 *
 * <h2>Why the other contexts keep the request's internal data</h2>
 * Internal data is per-form feedback, read only by the form that wrote it. Giving one form's messages to another
 * would let them surface under the wrong form (see the "deliberately NOT merged" note on
 * {@code submittedDataAcrossAllContexts}).
 *
 * @see EventHandlerServiceImpl
 */
public class EchoedInternalDataOfOriginatingContextTest
{
    private static final String ORIGINATING_CONTEXT_ID = "originating.form";
    private static final String OTHER_CONTEXT_ID       = "other.form";

    private static final String FEEDBACK_KEY           = "validationFeedback";

    private final Target        target                 = Target.empty();

    @Test
    public void testOriginatingContextEntryCarriesTheHandlersInternalDataAndTheOtherContextKeepsTheRequests()
    {
        EventHandlerServiceImpl service = this.serviceWithHandlerWritingFeedback();

        EventBody request = this.requestCarrying(List.of(this.originatingContext(), this.otherContext()));
        ResponseBody response = service.handleEvent(request)
                                       .orElseThrow();

        DataWithContext originatingEntry = this.entryOf(response, ORIGINATING_CONTEXT_ID);
        assertEquals("written by the handler", originatingEntry.getInternalData()
                                                               .get(FEEDBACK_KEY),
                     "the originating context's entry must carry the internalData the handler wrote, or the browser never sees the feedback. Got: "
                                                                                   + originatingEntry.getInternalData());
        assertEquals("kept", originatingEntry.getInternalData()
                                             .get("originatingKey"),
                     "the handler built on the request's internalData, so what the request carried must still be there");

        DataWithContext otherEntry = this.entryOf(response, OTHER_CONTEXT_ID);
        assertFalse(otherEntry.getInternalData()
                              .containsKey(FEEDBACK_KEY),
                    "a context that did not originate the event must NOT receive the handler's feedback (no cross-form leakage). Got: "
                                                          + otherEntry.getInternalData());
        assertEquals(Map.of("otherKey", "untouched"), otherEntry.getInternalData(), "the other context keeps exactly the internalData the request sent");
    }

    @Test
    public void testOriginatingContextAddedBecauseTheRequestListedNoSuchContextAlsoCarriesTheHandlersInternalData()
    {
        EventHandlerServiceImpl service = this.serviceWithHandlerWritingFeedback();

        EventBody request = this.requestCarrying(List.of(this.otherContext()));
        ResponseBody response = service.handleEvent(request)
                                       .orElseThrow();

        DataWithContext originatingEntry = this.entryOf(response, ORIGINATING_CONTEXT_ID);
        assertEquals("written by the handler", originatingEntry.getInternalData()
                                                               .get(FEEDBACK_KEY),
                     "the originating context is added to dataWithContexts when the request did not list it, and that added entry must carry the handler's internalData too");
        assertFalse(this.entryOf(response, OTHER_CONTEXT_ID)
                        .getInternalData()
                        .containsKey(FEEDBACK_KEY),
                    "the other context must still not receive the feedback");
    }

    @Test
    public void testSingularDataWithContextStillCarriesTheHandlersInternalData()
    {
        EventHandlerServiceImpl service = this.serviceWithHandlerWritingFeedback();

        ResponseBody response = service.handleEvent(this.requestCarrying(List.of(this.originatingContext())))
                                       .orElseThrow();

        assertEquals("written by the handler", response.getDataWithContext()
                                                       .getInternalData()
                                                       .get(FEEDBACK_KEY));
    }

    private DataWithContext entryOf(ResponseBody response, String contextId)
    {
        DataWithContext entry = response.getDataWithContexts()
                                        .stream()
                                        .filter(context -> contextId.equals(context.getContextId()))
                                        .findFirst()
                                        .orElse(null);
        assertNotNull(entry, "the response's dataWithContexts must hold an entry for context '" + contextId + "'");
        assertTrue(entry.getInternalData() != null, "an echoed entry never has null internalData");
        return entry;
    }

    private DataWithContext originatingContext()
    {
        return new DataWithContext(ORIGINATING_CONTEXT_ID, new HashMap<>(Map.of("name", "value")), new HashMap<>(Map.of("originatingKey", "kept")));
    }

    private DataWithContext otherContext()
    {
        return new DataWithContext(OTHER_CONTEXT_ID, new HashMap<>(Map.of("note", "text")), new HashMap<>(Map.of("otherKey", "untouched")));
    }

    private EventBody requestCarrying(List<DataWithContext> allContexts)
    {
        // An anonymous subclass sets the protected list, so the wire type's public surface is not widened for a test.
        return new EventBody(this.target, this.originatingContext()) {
            {
                this.dataWithContexts = allContexts;
            }
        };
    }

    /**
     * A handler that builds on the request's internal data and adds a {@code validationFeedback} entry, the shape
     * {@code ButtonFormElementImpl#onClick(ButtonEventHandlerWithMessaging)} produces.
     * <p>
     * It copies into a FRESH map instead of calling {@code Data#clone()}: {@code DataImpl} shares the map it was
     * given and {@code clone()} is shallow, so a cloning handler writes straight through into the request object's
     * own internal data. A test built on that would see the "handler's" value in the request's copy and could not
     * tell a correct echo from the old one.
     */
    private EventHandlerServiceImpl serviceWithHandlerWritingFeedback()
    {
        EventHandlerServiceImpl service = new EventHandlerServiceImpl() {
            {
                this.rerenderingService = Mockito.mock(RerenderingService.class);
                Mockito.when(this.rerenderingService.rerenderTargetNode(any(), any()))
                       .thenReturn(Optional.of(new TargetNode(Target.empty(), null)));
            }
        };
        service.executeTransactionalAndPublishStagingHandlers(() ->
        {
            service.registerDataEventHandler(this.target, (data, internalData) ->
            {
                Data written = Data.of(internalData.getContextId(), new HashMap<>(internalData.toMap()));
                written.setFieldValue(FEEDBACK_KEY, "written by the handler");
                return DataEventHandler.MappedData.builder()
                                                  .data(data)
                                                  .internalData(written)
                                                  .build();
            });
            return null;
        });
        return service;
    }
}
