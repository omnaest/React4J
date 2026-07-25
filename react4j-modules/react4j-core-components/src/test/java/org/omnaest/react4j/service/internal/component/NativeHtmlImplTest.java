package org.omnaest.react4j.service.internal.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.service.internal.nodes.NativeHtmlNode;
import org.omnaest.react4j.service.internal.service.LocalizedTextResolverService;

/**
 * Goal-1 contract-fidelity test: asserts the {@link NativeHtmlImpl} builder API ({@code withSource}) maps onto the
 * produced {@link NativeHtmlNode}, and that the source is lazily re-evaluated via the {@code Supplier} overload on
 * every render.
 *
 * @see NativeHtmlImpl
 * @author omnaest
 */
public class NativeHtmlImplTest
{
    private ComponentContext newContext()
    {
        ComponentContext context = mock(ComponentContext.class);
        when(context.getTextResolver()).thenReturn(mock(LocalizedTextResolverService.class));
        return context;
    }

    @Test
    public void testDefaultSourceIsNull()
    {
        ComponentContext context = this.newContext();
        NativeHtmlImpl nativeHtml = new NativeHtmlImpl(context);

        Node node = nativeHtml.asRenderer()
                              .render(mock(RenderingProcessor.class), mock(Location.class), Optional.empty());

        assertNull(((NativeHtmlNode) node).getSource());
    }

    @Test
    public void testWithSourceStringMapsToNodeSource()
    {
        ComponentContext context = this.newContext();
        NativeHtmlImpl nativeHtml = new NativeHtmlImpl(context);

        nativeHtml.withSource("<div>hello</div>");

        Node node = nativeHtml.asRenderer()
                              .render(mock(RenderingProcessor.class), mock(Location.class), Optional.empty());

        assertEquals("<div>hello</div>", ((NativeHtmlNode) node).getSource());
    }

    @Test
    public void testWithSourceSupplierIsReEvaluatedOnEveryRender()
    {
        ComponentContext context = this.newContext();
        NativeHtmlImpl nativeHtml = new NativeHtmlImpl(context);

        AtomicInteger counter = new AtomicInteger();
        nativeHtml.withSource(() -> "call-" + counter.incrementAndGet());

        Node firstNode = nativeHtml.asRenderer()
                                   .render(mock(RenderingProcessor.class), mock(Location.class), Optional.empty());
        Node secondNode = nativeHtml.asRenderer()
                                    .render(mock(RenderingProcessor.class), mock(Location.class), Optional.empty());

        assertEquals("call-1", ((NativeHtmlNode) firstNode).getSource());
        assertEquals("call-2", ((NativeHtmlNode) secondNode).getSource());
    }
}
