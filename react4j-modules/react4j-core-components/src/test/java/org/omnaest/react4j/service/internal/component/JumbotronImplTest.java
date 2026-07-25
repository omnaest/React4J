package org.omnaest.react4j.service.internal.component;

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
import org.omnaest.react4j.domain.GridContainer;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.UIComponentFactory;
import org.omnaest.react4j.domain.i18n.I18nText;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.service.internal.nodes.JumbotronNode;
import org.omnaest.react4j.service.internal.nodes.i18n.I18nTextValue;
import org.omnaest.react4j.service.internal.service.LocalizedTextResolverService;

/**
 * Goal-1 contract-fidelity test: asserts the {@link JumbotronImpl} builder API ({@code withTitle}/{@code withSubTitle}/
 * {@code withFullWidth}/{@code withContent}/{@code withGridContent}) maps onto the produced {@link JumbotronNode}.
 *
 * @see JumbotronImpl
 * @author omnaest
 */
public class JumbotronImplTest
{
    private ComponentContext newContext()
    {
        ComponentContext context = mock(ComponentContext.class);
        when(context.getTextResolver()).thenReturn(mock(LocalizedTextResolverService.class));
        return context;
    }

    @Test
    public void testDefaultsHaveFullWidthDisabled()
    {
        ComponentContext context = this.newContext();
        JumbotronImpl jumbotron = new JumbotronImpl(context);
        jumbotron.withContent(mock(UIComponent.class));

        Node node = jumbotron.asRenderer()
                             .render(mock(RenderingProcessor.class), mock(Location.class), Optional.empty());

        assertFalse(((JumbotronNode) node).isFullWidth());
    }

    @Test
    public void testWithTitleAndSubTitleAreResolvedAndMapped()
    {
        ComponentContext context = this.newContext();
        LocalizedTextResolverService textResolver = context.getTextResolver();
        Location location = mock(Location.class);
        I18nTextValue resolvedTitle = new I18nTextValue(Map.of("DEFAULT", "Welcome"));
        I18nTextValue resolvedSubTitle = new I18nTextValue(Map.of("DEFAULT", "Subtitle"));
        when(textResolver.apply(any(I18nText.class), eq(location))).thenAnswer(invocation ->
        {
            I18nText text = invocation.getArgument(0);
            return "Welcome".equals(text.getDefaultText()) ? resolvedTitle : resolvedSubTitle;
        });

        JumbotronImpl jumbotron = new JumbotronImpl(context);
        jumbotron.withTitle("Welcome");
        jumbotron.withSubTitle("Subtitle");
        jumbotron.withContent(mock(UIComponent.class));
        jumbotron.withFullWidth();

        Node node = jumbotron.asRenderer()
                             .render(mock(RenderingProcessor.class), location, Optional.empty());

        JumbotronNode jumbotronNode = (JumbotronNode) node;
        assertSame(resolvedTitle, jumbotronNode.getTitle());
        assertSame(resolvedSubTitle, jumbotronNode.getSubTitle());
        assertTrue(jumbotronNode.isFullWidth());
    }

    @Test
    public void testWithContentIsRendered()
    {
        ComponentContext context = this.newContext();
        UIComponent<?> content = mock(UIComponent.class);
        JumbotronImpl jumbotron = new JumbotronImpl(context);
        jumbotron.withContent(content);

        RenderingProcessor renderingProcessor = mock(RenderingProcessor.class);
        Location location = mock(Location.class);
        Node contentNode = mock(Node.class);
        when(renderingProcessor.process(content, location)).thenReturn(contentNode);

        Node node = jumbotron.asRenderer()
                             .render(renderingProcessor, location, Optional.empty());

        assertSame(contentNode, ((JumbotronNode) node).getContent());
    }

    @Test
    public void testWithGridContentBuildsGridViaUiComponentFactoryAndSetsItAsContent()
    {
        UIComponentFactory factory = mock(UIComponentFactory.class);
        GridContainer grid = mock(GridContainer.class);
        when(factory.newGridContainer()).thenReturn(grid);

        ComponentContext context = this.newContext();
        when(context.getUiComponentFactory()).thenReturn(factory);

        JumbotronImpl jumbotron = new JumbotronImpl(context);
        jumbotron.withGridContent(gridContainer -> gridContainer.addRow(row ->
        {
        }));

        org.mockito.Mockito.verify(grid)
                           .addRow(org.mockito.ArgumentMatchers.any());

        RenderingProcessor renderingProcessor = mock(RenderingProcessor.class);
        Location location = mock(Location.class);
        Node gridNode = mock(Node.class);
        when(renderingProcessor.process(grid, location)).thenReturn(gridNode);

        Node node = jumbotron.asRenderer()
                             .render(renderingProcessor, location, Optional.empty());
        assertSame(gridNode, ((JumbotronNode) node).getContent());
    }

    @Test
    public void testFieldsSurviveTemplating()
    {
        ComponentContext context = this.newContext();
        JumbotronImpl jumbotron = new JumbotronImpl(context);
        jumbotron.withContent(mock(UIComponent.class));
        jumbotron.withFullWidth();

        JumbotronImpl templated = (JumbotronImpl) jumbotron.asTemplateProvider()
                                                           .get();

        Node node = templated.asRenderer()
                             .render(mock(RenderingProcessor.class), mock(Location.class), Optional.empty());

        assertTrue(((JumbotronNode) node).isFullWidth());
    }
}
