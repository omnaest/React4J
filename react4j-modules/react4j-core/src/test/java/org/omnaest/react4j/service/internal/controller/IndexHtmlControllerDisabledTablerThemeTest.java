package org.omnaest.react4j.service.internal.controller;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.omnaest.react4j.EnableReactUI;
import org.omnaest.react4j.data.annotations.EnableReactUIInMemoryRepository;
import org.omnaest.react4j.domain.configuration.ThemeConfiguration.ThemePreset;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

/**
 * plan-277 T2, AC4 (b): TABLER is selected and the theme is then disabled. Disabled means the stock Bootstrap link and nothing else: the preset is
 * ignored, no tabler link, no colour mode attribute. The stylesheet file itself is deliberately NOT requested (see
 * {@link IndexHtmlControllerTablerThemeTest}).
 */
@SpringBootTest(classes = IndexHtmlControllerDisabledTablerThemeTest.TestApplication.class, webEnvironment = WebEnvironment.MOCK)
@AutoConfigureMockMvc
public class IndexHtmlControllerDisabledTablerThemeTest extends AbstractIndexHtmlThemeTest
{
    @SpringBootApplication
    @EnableReactUI
    @EnableReactUIInMemoryRepository
    public static class TestApplication
    {
    }

    @Override
    protected void configureTheme()
    {
        this.reactUIService.configureTheme(theme -> theme.preset(ThemePreset.TABLER)
                                                         .disable());
    }

    @Test
    public void testOnlyTheStockLinkIsEmittedAndItPrecedesTheApplicationStylesheets() throws Exception
    {
        String indexHtml = this.getIndexHtml();

        int stockIndex = indexOfStylesheetLink(indexHtml, STOCK_STYLESHEET);
        int appIndex = indexOfStylesheetLink(indexHtml, APP_STYLESHEET);
        assertTrue(stockIndex < appIndex, "the stock link must precede " + APP_STYLESHEET + ": " + indexHtml);
        assertFalse(indexHtml.contains("react4j-tabler"), indexHtml);
        assertFalse(indexHtml.contains("react4j-modern"), indexHtml);
        assertNoRawPlaceholderSurvives(indexHtml);
    }

    @Test
    public void testNoColorModeAttributeNoScriptAndNoStyleBlockIsEmitted() throws Exception
    {
        String indexHtml = this.getIndexHtml();

        assertFalse(indexHtml.contains("data-bs-theme"), indexHtml);
        assertFalse(indexHtml.contains("<script>"), "no inline colour mode script while disabled: " + indexHtml);
        assertFalse(indexHtml.contains("<style"), indexHtml);
    }
}
