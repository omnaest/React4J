package org.omnaest.react4j.service.internal.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.omnaest.react4j.domain.Icon.StandardIcon;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.service.internal.nodes.IconNode;
import org.omnaest.react4j.service.internal.service.LocalizedTextResolverService;

/**
 * Goal-1 contract-fidelity test: asserts the {@link IconImpl} builder API ({@code from(StandardIcon)}) maps onto the
 * produced {@link IconNode}.
 *
 * @see IconImpl
 * @author omnaest
 */
public class IconImplTest
{
    private ComponentContext newContext()
    {
        ComponentContext context = mock(ComponentContext.class);
        when(context.getTextResolver()).thenReturn(mock(LocalizedTextResolverService.class));
        return context;
    }

    @Test
    public void testDefaultHasNullIcon()
    {
        ComponentContext context = this.newContext();
        IconImpl icon = new IconImpl(context);

        Node node = icon.asRenderer()
                        .render(mock(RenderingProcessor.class), mock(Location.class), Optional.empty());

        assertNull(((IconNode) node).getIcon());
    }

    @Test
    public void testFromStandardIconMapsToItsIdentifier()
    {
        ComponentContext context = this.newContext();
        IconImpl icon = new IconImpl(context);

        icon.from(StandardIcon.DNA);

        Node node = icon.asRenderer()
                        .render(mock(RenderingProcessor.class), mock(Location.class), Optional.empty());

        assertEquals(StandardIcon.DNA.get(), ((IconNode) node).getIcon());
    }

    @Test
    public void testFieldsSurviveTemplating()
    {
        ComponentContext context = this.newContext();
        IconImpl icon = new IconImpl(context);
        icon.from(StandardIcon.DNA);

        IconImpl templated = (IconImpl) icon.asTemplateProvider()
                                            .get();

        Node node = templated.asRenderer()
                             .render(mock(RenderingProcessor.class), mock(Location.class), Optional.empty());

        assertEquals(StandardIcon.DNA.get(), ((IconNode) node).getIcon());
    }
}
