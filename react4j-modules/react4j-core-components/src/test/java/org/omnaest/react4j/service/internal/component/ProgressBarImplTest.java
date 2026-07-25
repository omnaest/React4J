package org.omnaest.react4j.service.internal.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
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
import org.omnaest.react4j.service.internal.nodes.ProgressBarNode;
import org.omnaest.react4j.service.internal.nodes.i18n.I18nTextValue;
import org.omnaest.react4j.service.internal.service.LocalizedTextResolverService;

/**
 * Goal-1 contract-fidelity test: asserts the {@link ProgressBarImpl} builder API ({@code withValue}/{@code withMinimum}/
 * {@code withMaximum}/{@code withProgressRatio}/{@code withRatioText}/{@code withText}) maps onto the produced
 * {@link ProgressBarNode}.
 *
 * @see ProgressBarImpl
 * @author omnaest
 */
public class ProgressBarImplTest
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
        ProgressBarImpl progressBar = new ProgressBarImpl(context);

        Node node = progressBar.asRenderer()
                               .render(mock(RenderingProcessor.class), mock(Location.class), Optional.empty());

        ProgressBarNode progressBarNode = (ProgressBarNode) node;
        assertEquals(0.0, progressBarNode.getMin());
        assertEquals(100.0, progressBarNode.getMax());
        assertEquals(0.0, progressBarNode.getValue());
        assertNull(progressBarNode.getText());
    }

    @Test
    public void testWithValueMinimumMaximumMapToNode()
    {
        ComponentContext context = this.newContext();
        ProgressBarImpl progressBar = new ProgressBarImpl(context);

        progressBar.withMinimum(10)
                   .withMaximum(50)
                   .withValue(30);

        Node node = progressBar.asRenderer()
                               .render(mock(RenderingProcessor.class), mock(Location.class), Optional.empty());

        ProgressBarNode progressBarNode = (ProgressBarNode) node;
        assertEquals(10.0, progressBarNode.getMin());
        assertEquals(50.0, progressBarNode.getMax());
        assertEquals(30.0, progressBarNode.getValue());
    }

    @Test
    public void testWithProgressRatioSetsZeroToOneRange()
    {
        ComponentContext context = this.newContext();
        ProgressBarImpl progressBar = new ProgressBarImpl(context);

        progressBar.withProgressRatio(0.25);

        Node node = progressBar.asRenderer()
                               .render(mock(RenderingProcessor.class), mock(Location.class), Optional.empty());

        ProgressBarNode progressBarNode = (ProgressBarNode) node;
        assertEquals(0.0, progressBarNode.getMin());
        assertEquals(1.0, progressBarNode.getMax());
        assertEquals(0.25, progressBarNode.getValue());
    }

    @Test
    public void testWithTextResolvesTranslatableI18nText()
    {
        ComponentContext context = this.newContext();
        LocalizedTextResolverService textResolver = context.getTextResolver();
        Location location = mock(Location.class);
        I18nTextValue resolvedText = new I18nTextValue(Map.of("DEFAULT", "Loading"));
        when(textResolver.apply(any(I18nText.class), eq(location))).thenReturn(resolvedText);

        ProgressBarImpl progressBar = new ProgressBarImpl(context);
        progressBar.withText("Loading");

        Node node = progressBar.asRenderer()
                               .render(mock(RenderingProcessor.class), location, Optional.empty());

        assertSame(resolvedText, ((ProgressBarNode) node).getText());
    }

    @Test
    public void testFieldsSurviveTemplating()
    {
        ComponentContext context = this.newContext();
        ProgressBarImpl progressBar = new ProgressBarImpl(context);
        progressBar.withValue(42)
                   .withMinimum(0)
                   .withMaximum(100);

        ProgressBarImpl templated = (ProgressBarImpl) progressBar.asTemplateProvider()
                                                                 .get();

        Node node = templated.asRenderer()
                             .render(mock(RenderingProcessor.class), mock(Location.class), Optional.empty());

        assertEquals(42.0, ((ProgressBarNode) node).getValue());
    }
}
