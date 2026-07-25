package org.omnaest.react4j.service.internal.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.i18n.I18nText;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.service.internal.nodes.IFrameContainerNode;
import org.omnaest.react4j.service.internal.nodes.i18n.I18nTextValue;
import org.omnaest.react4j.service.internal.service.LocalizedTextResolverService;

/**
 * Goal-1 contract-fidelity test: asserts the {@link IFrameImpl} builder API ({@code withTitle}/
 * {@code withNonTranslatedTitle}/{@code withSourceLink}/{@code allowFullScreen}) maps onto the produced
 * {@link IFrameContainerNode}.
 *
 * @see IFrameImpl
 * @author omnaest
 */
public class IFrameImplTest
{
    private ComponentContext newContext()
    {
        ComponentContext context = mock(ComponentContext.class);
        when(context.getTextResolver()).thenReturn(mock(LocalizedTextResolverService.class));
        return context;
    }

    @Test
    public void testDefaultsMapToNode()
    {
        ComponentContext context = this.newContext();
        IFrameImpl iframe = new IFrameImpl(context);

        Node node = iframe.asRenderer()
                          .render(mock(RenderingProcessor.class), mock(Location.class), Optional.empty());

        IFrameContainerNode iframeNode = (IFrameContainerNode) node;
        assertFalse(iframeNode.isAllowFullScreen());
    }

    @Test
    public void testWithTitleAndSourceLinkAndFullScreenMapToNode()
    {
        ComponentContext context = this.newContext();
        LocalizedTextResolverService textResolver = context.getTextResolver();
        Location location = mock(Location.class);
        I18nTextValue resolvedTitle = new I18nTextValue(Map.of("DEFAULT", "Embedded page"));
        when(textResolver.apply(any(I18nText.class), eq(location))).thenReturn(resolvedTitle);

        IFrameImpl iframe = new IFrameImpl(context);
        iframe.withTitle("Embedded page");
        iframe.withSourceLink("https://example.org");
        iframe.allowFullScreen();

        Node node = iframe.asRenderer()
                          .render(mock(RenderingProcessor.class), location, Optional.empty());

        IFrameContainerNode iframeNode = (IFrameContainerNode) node;
        assertSame(resolvedTitle, iframeNode.getTitle());
        assertEquals("https://example.org", iframeNode.getSourceLink());
        assertTrue(iframeNode.isAllowFullScreen());
    }

    @Test
    public void testWithNonTranslatedTitleMarksI18nTextAsNonTranslatable()
    {
        ComponentContext context = this.newContext();
        LocalizedTextResolverService textResolver = context.getTextResolver();
        Location location = mock(Location.class);

        IFrameImpl iframe = new IFrameImpl(context);
        iframe.withNonTranslatedTitle("Raw title");

        iframe.asRenderer()
              .render(mock(RenderingProcessor.class), location, Optional.empty());

        org.mockito.ArgumentCaptor<I18nText> captor = org.mockito.ArgumentCaptor.forClass(I18nText.class);
        org.mockito.Mockito.verify(textResolver)
                           .apply(captor.capture(), eq(location));
        assertTrue(captor.getValue()
                         .isNonTranslatable());
    }

    @Test
    public void testAllowFullScreenBooleanOverloadTogglesExplicitly()
    {
        ComponentContext context = this.newContext();
        IFrameImpl iframe = new IFrameImpl(context);

        iframe.allowFullScreen(true);
        Node enabledNode = iframe.asRenderer()
                                 .render(mock(RenderingProcessor.class), mock(Location.class), Optional.empty());
        assertTrue(((IFrameContainerNode) enabledNode).isAllowFullScreen());

        iframe.allowFullScreen(false);
        Node disabledNode = iframe.asRenderer()
                                  .render(mock(RenderingProcessor.class), mock(Location.class), Optional.empty());
        assertFalse(((IFrameContainerNode) disabledNode).isAllowFullScreen());
    }

    @Test
    public void testFieldsSurviveTemplating()
    {
        ComponentContext context = this.newContext();
        IFrameImpl iframe = new IFrameImpl(context);
        iframe.withSourceLink("https://example.org");
        iframe.allowFullScreen();

        IFrameImpl templated = (IFrameImpl) iframe.asTemplateProvider()
                                                  .get();

        Node node = templated.asRenderer()
                             .render(mock(RenderingProcessor.class), mock(Location.class), Optional.empty());

        IFrameContainerNode iframeNode = (IFrameContainerNode) node;
        assertEquals("https://example.org", iframeNode.getSourceLink());
        assertTrue(iframeNode.isAllowFullScreen());
    }
}
