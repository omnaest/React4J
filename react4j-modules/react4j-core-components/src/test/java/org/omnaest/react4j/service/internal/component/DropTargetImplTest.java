package org.omnaest.react4j.service.internal.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.omnaest.react4j.domain.DropTarget.DropEvent;
import org.omnaest.react4j.domain.DropTarget.DropRelation;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.context.data.Data;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer.EventHandlerRegistrationSupport;
import org.omnaest.react4j.domain.rendering.components.LocationSupport;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.service.internal.handler.domain.DataEventHandler;
import org.omnaest.react4j.service.internal.handler.domain.EventHandler;
import org.omnaest.react4j.service.internal.handler.domain.Target;
import org.omnaest.react4j.service.internal.nodes.DropTargetNode;
import org.omnaest.react4j.service.internal.nodes.ParagraphNode;

/**
 * @see DropTargetImpl
 * @author omnaest
 */
public class DropTargetImplTest
{
    private ComponentContext newContext()
    {
        return mock(ComponentContext.class);
    }

    /**
     * A recording {@link EventHandlerRegistrationSupport} test double, capturing whatever DataEventHandler
     * gets registered (or none).
     */
    private static class RecordingRegistrationSupport implements EventHandlerRegistrationSupport
    {
        private DataEventHandler registeredHandler;

        @Override
        public EventHandlerRegistrationSupport register(EventHandler eventHandler)
        {
            return this;
        }

        @Override
        public EventHandlerRegistrationSupport register(DataEventHandler eventHandler)
        {
            this.registeredHandler = eventHandler;
            return this;
        }

        @Override
        public EventHandlerRegistrationSupport registerAsRerenderingNode()
        {
            return this;
        }
    }

    private Location stubbedLocation(String... segments)
    {
        Location location = mock(Location.class);
        when(location.get()).thenReturn(List.of(segments));
        return location;
    }

    // ---------------------------------------------------------------------------------------------------
    // AC-S1-3: gating - no onDrop -> Target.empty() and null field keys, and nothing registers.
    // ---------------------------------------------------------------------------------------------------

    @Test
    public void testNoOnDropEmitsEmptyTargetAndNullFieldKeys()
    {
        DropTargetImpl dropTarget = new DropTargetImpl(this.newContext());

        UIComponentRenderer renderer = dropTarget.asRenderer();
        Location location = this.stubbedLocation("root", "droptargetimpl");
        Node node = renderer.render(mock(RenderingProcessor.class), location, Optional.empty());

        DropTargetNode dropTargetNode = (DropTargetNode) node;
        assertTrue(dropTargetNode.getDropTarget()
                                 .isEmpty(),
                   "an ungated DropTarget must emit Target.empty(), never null");
        assertNull(dropTargetNode.getDragIdFieldKey());
        assertNull(dropTargetNode.getRelationFieldKey());
    }

    @Test
    public void testNoOnDropRegistersNoHandler()
    {
        DropTargetImpl dropTarget = new DropTargetImpl(this.newContext());
        UIComponentRenderer renderer = dropTarget.asRenderer();
        renderer.getLocation(this.locationSupport(this.stubbedLocation("root")));

        RecordingRegistrationSupport support = new RecordingRegistrationSupport();
        renderer.manageEventHandler(support);

        assertNull(support.registeredHandler, "no onDrop -> no handler registered at all");
    }

    // ---------------------------------------------------------------------------------------------------
    // Gated path - onDrop configured.
    // ---------------------------------------------------------------------------------------------------

    @Test
    public void testOnDropConfiguredEmitsNonEmptyTargetAndLocationDerivedFieldKeys()
    {
        DropTargetImpl dropTarget = new DropTargetImpl(this.newContext());
        dropTarget.onDrop(event ->
        {
        });

        UIComponentRenderer renderer = dropTarget.asRenderer();
        Location location = this.stubbedLocation("root", "droptargetimpl");
        Node node = renderer.render(mock(RenderingProcessor.class), location, Optional.empty());

        DropTargetNode dropTargetNode = (DropTargetNode) node;
        assertFalse(dropTargetNode.getDropTarget()
                                  .isEmpty());
        assertEquals(Target.from(location), dropTargetNode.getDropTarget());
        assertEquals("droptarget.root.droptargetimpl.dragId", dropTargetNode.getDragIdFieldKey());
        assertEquals("droptarget.root.droptargetimpl.relation", dropTargetNode.getRelationFieldKey());
    }

    // ---------------------------------------------------------------------------------------------------
    // AC-C2/AC-C3 (plan-235 S1 corrective round): manageEventHandler must fail loudly rather than silently
    // skip registration when onDrop was configured but getLocation(...) never ran first - the disagreement
    // between a live render() Target and a silently-unregistered handler is the regression class this guards.
    // ---------------------------------------------------------------------------------------------------

    @Test
    public void testManageEventHandlerThrowsWhenOnDropConfiguredButLocationWasNeverResolved()
    {
        DropTargetImpl dropTarget = new DropTargetImpl(this.newContext());
        dropTarget.onDrop(event ->
        {
        });

        UIComponentRenderer renderer = dropTarget.asRenderer();
        // deliberately no renderer.getLocation(...) call here - lastResolvedLocation stays null

        RecordingRegistrationSupport support = new RecordingRegistrationSupport();

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> renderer.manageEventHandler(support),
                                                       "onDrop configured but getLocation(...) never ran -> manageEventHandler must fail loudly, not skip silently");
        assertTrue(exception.getMessage()
                            .contains("DropTargetImpl"),
                   "the exception message must name the class");
        assertNull(support.registeredHandler, "no handler may have been registered when the exception fired");
    }

    @Test
    public void testOnDropConfiguredRegistersAHandler()
    {
        DropTargetImpl dropTarget = new DropTargetImpl(this.newContext());
        dropTarget.onDrop(event ->
        {
        });

        UIComponentRenderer renderer = dropTarget.asRenderer();
        renderer.getLocation(this.locationSupport(this.stubbedLocation("root", "droptargetimpl")));

        RecordingRegistrationSupport support = new RecordingRegistrationSupport();
        renderer.manageEventHandler(support);

        assertTrue(support.registeredHandler != null, "onDrop configured -> a handler must be registered");
    }

    // ---------------------------------------------------------------------------------------------------
    // The registered handler's behaviour - AC-S1-1's server-side half, isolated from the HTTP seam.
    // ---------------------------------------------------------------------------------------------------

    @Test
    public void testRegisteredHandlerInvokesOnDropWithTheDragIdAndRelationReadFromData()
    {
        AtomicReference<DropEvent> captured = new AtomicReference<>();
        DropTargetImpl dropTarget = new DropTargetImpl(this.newContext());
        dropTarget.onDrop(captured::set);

        UIComponentRenderer renderer = dropTarget.asRenderer();
        Location location = this.stubbedLocation("root", "droptargetimpl");
        renderer.getLocation(this.locationSupport(location));

        RecordingRegistrationSupport support = new RecordingRegistrationSupport();
        renderer.manageEventHandler(support);

        Map<String, Object> fields = new HashMap<>();
        fields.put("droptarget.root.droptargetimpl.dragId", "card-42");
        fields.put("droptarget.root.droptargetimpl.relation", "AFTER");
        Data eventData = Data.of("", fields);

        support.registeredHandler.invoke(eventData, Data.newInstance());

        assertEquals("card-42", captured.get()
                                        .getDragId());
        assertEquals(DropRelation.AFTER, captured.get()
                                                 .getRelation());
    }

    @Test
    public void testRegisteredHandlerIgnoresAMalformedRelationDefensively()
    {
        AtomicReference<DropEvent> captured = new AtomicReference<>();
        DropTargetImpl dropTarget = new DropTargetImpl(this.newContext());
        dropTarget.onDrop(captured::set);

        UIComponentRenderer renderer = dropTarget.asRenderer();
        Location location = this.stubbedLocation("root", "droptargetimpl");
        renderer.getLocation(this.locationSupport(location));

        RecordingRegistrationSupport support = new RecordingRegistrationSupport();
        renderer.manageEventHandler(support);

        Map<String, Object> fields = new HashMap<>();
        fields.put("droptarget.root.droptargetimpl.dragId", "card-42");
        fields.put("droptarget.root.droptargetimpl.relation", "sideways");
        Data eventData = Data.of("", fields);

        support.registeredHandler.invoke(eventData, Data.newInstance());

        assertNull(captured.get(), "a malformed relation must never invoke the app's onDrop callback");
    }

    @Test
    public void testRegisteredHandlerDoesNothingWhenNeitherFieldWasSubmitted()
    {
        AtomicReference<DropEvent> captured = new AtomicReference<>();
        DropTargetImpl dropTarget = new DropTargetImpl(this.newContext());
        dropTarget.onDrop(captured::set);

        UIComponentRenderer renderer = dropTarget.asRenderer();
        renderer.getLocation(this.locationSupport(this.stubbedLocation("root", "droptargetimpl")));

        RecordingRegistrationSupport support = new RecordingRegistrationSupport();
        renderer.manageEventHandler(support);

        support.registeredHandler.invoke(Data.newInstance(), Data.newInstance());

        assertNull(captured.get());
    }

    // ---------------------------------------------------------------------------------------------------
    // Content wrapping and the registration walk.
    // ---------------------------------------------------------------------------------------------------

    @Test
    public void testWithContentIsProcessedAndAttachedAsTheRenderedNodesContent()
    {
        DropTargetImpl dropTarget = new DropTargetImpl(this.newContext());
        UIComponent<?> content = mock(UIComponent.class);
        dropTarget.withContent(content);

        RenderingProcessor renderingProcessor = mock(RenderingProcessor.class);
        Location location = this.stubbedLocation("root");
        ParagraphNode contentNode = new ParagraphNode();
        when(renderingProcessor.process(content, location)).thenReturn(contentNode);

        Node node = dropTarget.asRenderer()
                              .render(renderingProcessor, location, Optional.empty());

        assertEquals(contentNode, ((DropTargetNode) node).getContent());
    }

    @Test
    public void testGetSubComponentsReturnsTheContentWhenPresentAndIsEmptyWhenAbsent()
    {
        DropTargetImpl bareDropTarget = new DropTargetImpl(this.newContext());
        assertEquals(0, bareDropTarget.asRenderer()
                                      .getSubComponents(mock(Location.class))
                                      .count());

        DropTargetImpl dropTargetWithContent = new DropTargetImpl(this.newContext());
        UIComponent<?> content = mock(UIComponent.class);
        dropTargetWithContent.withContent(content);
        assertEquals(1, dropTargetWithContent.asRenderer()
                                             .getSubComponents(mock(Location.class))
                                             .count());
    }

    // ---------------------------------------------------------------------------------------------------
    // AC-S1-8: templating - content and the onDrop wiring must both survive asTemplateProvider().
    // ---------------------------------------------------------------------------------------------------

    @Test
    public void testTemplatingSurvivesContentAndOnDropWiring()
    {
        AtomicReference<DropEvent> captured = new AtomicReference<>();
        DropTargetImpl dropTarget = new DropTargetImpl(this.newContext());
        UIComponent<?> content = mock(UIComponent.class);
        dropTarget.withContent(content)
                  .onDrop(captured::set);

        DropTargetImpl templated = (DropTargetImpl) dropTarget.asTemplateProvider()
                                                              .get();

        RenderingProcessor renderingProcessor = mock(RenderingProcessor.class);
        Location location = this.stubbedLocation("root", "droptargetimpl");
        ParagraphNode contentNode = new ParagraphNode();
        when(renderingProcessor.process(content, location)).thenReturn(contentNode);

        UIComponentRenderer renderer = templated.asRenderer();
        renderer.getLocation(this.locationSupport(location));
        Node node = renderer.render(renderingProcessor, location, Optional.empty());

        DropTargetNode dropTargetNode = (DropTargetNode) node;
        assertEquals(contentNode, dropTargetNode.getContent());
        assertFalse(dropTargetNode.getDropTarget()
                                  .isEmpty(),
                    "the templated instance must still have carried the onDrop wiring over");

        RecordingRegistrationSupport support = new RecordingRegistrationSupport();
        renderer.manageEventHandler(support);
        support.registeredHandler.invoke(Data.of("", Map.of("droptarget.root.droptargetimpl.dragId", "x",
                                                            "droptarget.root.droptargetimpl.relation", "INTO")),
                                         Data.newInstance());
        assertEquals("x", captured.get()
                                  .getDragId());
        assertEquals(DropRelation.INTO, captured.get()
                                                .getRelation());
    }

    private LocationSupport locationSupport(Location location)
    {
        return new LocationSupport() {
            @Override
            public Location getParentLocation()
            {
                return location;
            }

            @Override
            public Location createLocation(String id)
            {
                return location;
            }
        };
    }
}
