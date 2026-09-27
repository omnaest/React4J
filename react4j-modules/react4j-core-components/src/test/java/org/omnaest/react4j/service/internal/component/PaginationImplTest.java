package org.omnaest.react4j.service.internal.component;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.RenderableUIComponent;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer.EventHandlerRegistrationSupport;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.service.internal.handler.domain.EventHandler;
import org.omnaest.react4j.service.internal.nodes.PaginationItemNode;
import org.omnaest.react4j.service.internal.nodes.PaginationNode;
import org.omnaest.react4j.service.internal.nodes.handler.ServerHandler;
import org.omnaest.react4j.service.internal.service.LocalizedTextResolverService;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * @see PaginationImpl
 * @see PaginationItemImpl
 * @author omnaest
 */
public class PaginationImplTest
{
    private ComponentContext newContext()
    {
        ComponentContext context = mock(ComponentContext.class);
        when(context.getTextResolver()).thenReturn(mock(LocalizedTextResolverService.class));
        return context;
    }

    private RenderingProcessor selfRenderingProcessor()
    {
        RenderingProcessor renderingProcessor = mock(RenderingProcessor.class);
        when(renderingProcessor.process(any(UIComponent.class), any(Location.class))).thenAnswer(invocation ->
        {
            UIComponent<?> component = invocation.getArgument(0);
            Location location = invocation.getArgument(1);
            RenderableUIComponent<?> renderable = (RenderableUIComponent<?>) component;
            return renderable.asRenderer()
                             .render(this.selfRenderingProcessor(), location, Optional.empty());
        });
        return renderingProcessor;
    }

    @Test
    public void testEntriesDefaultToEmptyList()
    {
        ComponentContext context = this.newContext();
        PaginationImpl pagination = new PaginationImpl(context);

        UIComponentRenderer renderer = pagination.asRenderer();
        RenderingProcessor renderingProcessor = mock(RenderingProcessor.class);
        Location location = mock(Location.class);

        Node node = renderer.render(renderingProcessor, location, Optional.empty());

        assertTrue(((PaginationNode) node).getEntries()
                                          .isEmpty());
    }

    @Test
    public void testAddItemAppendsRenderedEntryWithActiveAndDisabledState()
    {
        ComponentContext context = this.newContext();
        PaginationImpl pagination = new PaginationImpl(context);

        pagination.addItem(item -> item.withActiveState(true)
                                       .withDisabledState(true));

        UIComponentRenderer renderer = pagination.asRenderer();
        RenderingProcessor renderingProcessor = this.selfRenderingProcessor();
        Location location = mock(Location.class);

        Node node = renderer.render(renderingProcessor, location, Optional.empty());

        List<PaginationItemNode> entries = ((PaginationNode) node).getEntries();
        assertEquals(1, entries.size());
        assertTrue(entries.get(0)
                          .isActive());
        assertTrue(entries.get(0)
                          .isDisabled());
    }

    @Test
    public void testEntriesSurviveTemplating()
    {
        ComponentContext context = this.newContext();
        PaginationImpl pagination = new PaginationImpl(context);
        pagination.addItem(item -> item.withActiveState(true));
        pagination.addItem(item -> item.withDisabledState(true));

        PaginationImpl templated = (PaginationImpl) pagination.asTemplateProvider()
                                                              .get();

        UIComponentRenderer renderer = templated.asRenderer();
        RenderingProcessor renderingProcessor = this.selfRenderingProcessor();
        Location location = mock(Location.class);

        Node node = renderer.render(renderingProcessor, location, Optional.empty());

        List<PaginationItemNode> entries = ((PaginationNode) node).getEntries();
        assertEquals(2, entries.size());
        assertTrue(entries.get(0)
                          .isActive());
        assertTrue(entries.get(1)
                          .isDisabled());
    }

    @Test
    public void testItemWithOnClickRendersNonNullServerHandler()
    {
        ComponentContext context = this.newContext();
        PaginationItemImpl item = new PaginationItemImpl(context, 0);
        item.onClick(mock(EventHandler.class));

        Location location = mock(Location.class);
        when(location.get()).thenReturn(Arrays.asList("root", item.getId()));

        Node node = item.asRenderer()
                        .render(mock(RenderingProcessor.class), location, Optional.empty());

        assertNotNull(((PaginationItemNode) node).getOnClick());
        assertTrue(((PaginationItemNode) node).getOnClick() instanceof ServerHandler);
    }

    @Test
    public void testItemWithoutOnClickHasNullOnClick()
    {
        ComponentContext context = this.newContext();
        PaginationItemImpl item = new PaginationItemImpl(context, 0);

        Location location = mock(Location.class);
        when(location.get()).thenReturn(Arrays.asList("root", item.getId()));

        Node node = item.asRenderer()
                        .render(mock(RenderingProcessor.class), location, Optional.empty());

        assertNull(((PaginationItemNode) node).getOnClick());
    }

    @Test
    public void testManageEventHandlerRegistersOnlyWhenOnClickSet()
    {
        ComponentContext context = this.newContext();

        PaginationItemImpl itemWithHandler = new PaginationItemImpl(context, 0);
        EventHandler handler = mock(EventHandler.class);
        itemWithHandler.onClick(handler);

        EventHandlerRegistrationSupport supportWithHandler = mock(EventHandlerRegistrationSupport.class);
        itemWithHandler.asRenderer()
                       .manageEventHandler(supportWithHandler);
        verify(supportWithHandler, times(1)).register(handler);

        PaginationItemImpl itemWithoutHandler = new PaginationItemImpl(context, 1);
        EventHandlerRegistrationSupport supportWithoutHandler = mock(EventHandlerRegistrationSupport.class);
        itemWithoutHandler.asRenderer()
                          .manageEventHandler(supportWithoutHandler);
        verify(supportWithoutHandler, never()).register(any(EventHandler.class));
    }

    @Test
    public void testTwoItemsWithDistinctHandlersProduceDistinctTargets()
    {
        ComponentContext context = this.newContext();

        PaginationItemImpl item0 = new PaginationItemImpl(context, 0);
        item0.onClick(mock(EventHandler.class));
        PaginationItemImpl item1 = new PaginationItemImpl(context, 1);
        item1.onClick(mock(EventHandler.class));

        assertNotEquals(item0.getId(), item1.getId());

        Location location0 = mock(Location.class);
        when(location0.get()).thenReturn(Arrays.asList("root", item0.getId()));
        Location location1 = mock(Location.class);
        when(location1.get()).thenReturn(Arrays.asList("root", item1.getId()));

        PaginationItemNode node0 = (PaginationItemNode) item0.asRenderer()
                                                             .render(mock(RenderingProcessor.class), location0, Optional.empty());
        PaginationItemNode node1 = (PaginationItemNode) item1.asRenderer()
                                                             .render(mock(RenderingProcessor.class), location1, Optional.empty());

        ServerHandler handler0 = (ServerHandler) node0.getOnClick();
        ServerHandler handler1 = (ServerHandler) node1.getOnClick();

        assertNotEquals(handler0.getTarget(), handler1.getTarget());
    }

    @Test
    public void testItemFieldsSurviveTemplating()
    {
        ComponentContext context = this.newContext();
        PaginationItemImpl item = new PaginationItemImpl(context, 2);
        item.withActiveState(true);
        item.withDisabledState(true);
        item.withAriaLabel("Switch to BACKLOG view");
        item.onClick(mock(EventHandler.class));

        PaginationItemImpl templated = (PaginationItemImpl) item.asTemplateProvider()
                                                                .get();

        Location location = mock(Location.class);
        when(location.get()).thenReturn(Arrays.asList("root", templated.getId()));

        Node node = templated.asRenderer()
                             .render(mock(RenderingProcessor.class), location, Optional.empty());

        assertTrue(((PaginationItemNode) node).isActive());
        assertTrue(((PaginationItemNode) node).isDisabled());
        assertEquals("Switch to BACKLOG view", ((PaginationItemNode) node).getAriaLabel());
        assertNotNull(((PaginationItemNode) node).getOnClick());
        assertEquals(item.getId(), templated.getId());
    }

    /**
     * AC-1/AC-2 (plan-255 S1): {@code withAriaLabel} sets the field into the node unconditionally - null stays
     * null, mirroring {@link org.omnaest.react4j.service.internal.component.ButtonImpl}'s shape.
     */
    @Test
    public void testItemWithoutAriaLabelHasNullAriaLabel()
    {
        ComponentContext context = this.newContext();
        PaginationItemImpl item = new PaginationItemImpl(context, 0);

        Location location = mock(Location.class);
        when(location.get()).thenReturn(Arrays.asList("root", item.getId()));

        Node node = item.asRenderer()
                        .render(mock(RenderingProcessor.class), location, Optional.empty());

        assertNull(((PaginationItemNode) node).getAriaLabel());
    }

    @Test
    public void testItemWithAriaLabelRendersItOnTheNode()
    {
        ComponentContext context = this.newContext();
        PaginationItemImpl item = new PaginationItemImpl(context, 0);
        item.withAriaLabel("Currently viewing BOARD view");

        Location location = mock(Location.class);
        when(location.get()).thenReturn(Arrays.asList("root", item.getId()));

        Node node = item.asRenderer()
                        .render(mock(RenderingProcessor.class), location, Optional.empty());

        assertEquals("Currently viewing BOARD view", ((PaginationItemNode) node).getAriaLabel());
    }

    /**
     * AC-6 (plan-255 S1): mirroring {@code ButtonNode} means the serialized node GAINS an {@code ariaLabel} key
     * with a {@code null} value when unset - not an absent key - exactly as every {@code BUTTON} node already
     * does (verified at source: neither {@code ButtonNode} nor {@code PaginationItemNode} carries a class- or
     * field-level {@code @JsonInclude}).
     * <p>
     * Serializes with a plain {@link ObjectMapper} plus a minimal ad-hoc {@link Optional} serializer rather than
     * {@code JSONHelper.serialize(...)}: this module carries no {@code jackson-datatype-jdk8} module (that is
     * pulled in only by {@code react4j-core}'s Spring auto-configuration), so a bare {@code ObjectMapper} trying
     * to introspect {@code AbstractNode#uiContextData}'s {@code Optional<UIContextDataNode>} as a plain bean
     * recurses into a {@link StackOverflowError} - a pre-existing landmine on every {@code Node} subtype, not
     * specific to this change, and out of this slice's additive-only scope to fix.
     */
    @Test
    public void testUnsetAriaLabelSerializesAsExplicitNullKeyMatchingButtonNode()
    {
        ComponentContext context = this.newContext();
        PaginationItemImpl item = new PaginationItemImpl(context, 0);

        Location location = mock(Location.class);
        when(location.get()).thenReturn(Arrays.asList("root", item.getId()));

        Node node = item.asRenderer()
                        .render(mock(RenderingProcessor.class), location, Optional.empty());

        SimpleModule optionalAsNullableValueModule = new SimpleModule();
        optionalAsNullableValueModule.addSerializer(Optional.class, new JsonSerializer<Optional>() {
            @Override
            public void serialize(Optional value, JsonGenerator gen, SerializerProvider serializers) throws IOException
            {
                if (value.isPresent())
                {
                    serializers.defaultSerializeValue(value.get(), gen);
                }
                else
                {
                    gen.writeNull();
                }
            }
        });
        ObjectMapper objectMapper = new ObjectMapper().registerModule(optionalAsNullableValueModule);

        String json = assertDoesNotThrow(() -> objectMapper.writeValueAsString(node));
        assertTrue(json.contains("\"ariaLabel\":null"), () -> "expected an explicit null ariaLabel key in: " + json);
    }
}
