package org.omnaest.react4j.service.internal.controller;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.omnaest.react4j.EnableReactUI;
import org.omnaest.react4j.data.annotations.EnableReactUIInMemoryRepository;
import org.omnaest.react4j.domain.configuration.ThemeConfiguration.ColorMode;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

/**
 * plan-274 S3, AC3.2 (b) and (d): the theme is disabled through {@code ReactUIService#configureTheme}. A colour mode, an added stylesheet and AUTO
 * are configured BEFORE the disable to prove that all of it is suppressed: disabled means the stock Bootstrap link and nothing else.
 */
@SpringBootTest(classes = IndexHtmlControllerDisabledThemeTest.TestApplication.class, webEnvironment = WebEnvironment.MOCK)
@AutoConfigureMockMvc
public class IndexHtmlControllerDisabledThemeTest extends AbstractIndexHtmlThemeTest
{
    private static final String ADDED_STYLESHEET = "/css/suppressed-added-sheet.css";

    @SpringBootApplication
    @EnableReactUI
    @EnableReactUIInMemoryRepository
    public static class TestApplication
    {
    }

    @Override
    protected void configureTheme()
    {
        this.reactUIService.configureTheme(theme -> theme.colorMode(ColorMode.AUTO)
                                                         .addStylesheet(ADDED_STYLESHEET)
                                                         .disable());
    }

    @Test
    public void testOnlyTheStockLinkIsEmittedAndItPrecedesTheApplicationStylesheets() throws Exception
    {
        String indexHtml = this.getIndexHtml();

        int stockIndex = indexOfStylesheetLink(indexHtml, STOCK_STYLESHEET);
        int appIndex = indexOfStylesheetLink(indexHtml, APP_STYLESHEET);
        assertTrue(stockIndex < appIndex, "the stock link must precede " + APP_STYLESHEET + ": " + indexHtml);
        assertFalse(indexHtml.contains("react4j-modern"), indexHtml);
        assertNoRawPlaceholderSurvives(indexHtml);
    }

    @Test
    public void testNoColorModeAttributeNoScriptNoStyleBlockAndNoAddedStylesheetIsEmitted() throws Exception
    {
        String indexHtml = this.getIndexHtml();

        assertFalse(indexHtml.contains("data-bs-theme"), indexHtml);
        assertFalse(indexHtml.contains("<script>"), "no inline colour mode script while disabled: " + indexHtml);
        assertFalse(indexHtml.contains("<style"), indexHtml);
        assertFalse(indexHtml.contains(ADDED_STYLESHEET), indexHtml);
    }

    @Test
    public void testConfigureThemeToleratesANullConsumer()
    {
        assertDoesNotThrow(() -> this.reactUIService.configureTheme(null));
    }

    @Test
    public void testBothThemeStylesheetsAreServedAsCssFromTheCoreUiArtifact() throws Exception
    {
        this.assertServedAsCss(MODERN_STYLESHEET);
        this.assertServedAsCss(STOCK_STYLESHEET);
    }
}
