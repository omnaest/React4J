package org.omnaest.react4j.service.internal.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.omnaest.react4j.EnableReactUI;
import org.omnaest.react4j.data.annotations.EnableReactUIInMemoryRepository;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

/**
 * plan-274 S3, AC3.2 (a) and (c): nothing is configured, so the default - modern theme, light - must be served, from the real classpath template
 * of the react4j-core-ui artifact.
 */
@SpringBootTest(classes = IndexHtmlControllerDefaultThemeTest.TestApplication.class, webEnvironment = WebEnvironment.MOCK)
@AutoConfigureMockMvc
public class IndexHtmlControllerDefaultThemeTest extends AbstractIndexHtmlThemeTest
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
        // deliberately nothing: this context proves the defaults
    }

    @Test
    public void testModernStylesheetIsLinkedBeforeTheApplicationStylesheetsAndNoStockBootstrapIsLinked() throws Exception
    {
        String indexHtml = this.getIndexHtml();

        int modernIndex = indexOfStylesheetLink(indexHtml, MODERN_STYLESHEET);
        int appIndex = indexOfStylesheetLink(indexHtml, APP_STYLESHEET);
        assertTrue(modernIndex < appIndex, "the theme link must precede " + APP_STYLESHEET + ": " + indexHtml);
        assertFalse(indexHtml.contains("bootstrap.min.css"), indexHtml);
    }

    @Test
    public void testHtmlElementCarriesTheLightColorModeAttribute() throws Exception
    {
        String htmlTag = htmlTag(this.getIndexHtml());

        assertTrue(htmlTag.contains("data-bs-theme=\"light\""), htmlTag);
    }

    @Test
    public void testThemeLinkCarriesTheSameCacheBusterAsTheApplicationLinks() throws Exception
    {
        String indexHtml = this.getIndexHtml();

        String cacheBuster = cacheBusterOf(indexHtml, MODERN_STYLESHEET);
        assertTrue(cacheBuster.matches("\\d+"), cacheBuster);
        assertEquals(cacheBusterOf(indexHtml, APP_STYLESHEET), cacheBuster);
    }

    @Test
    public void testNoRawPlaceholderSurvivesAndNoScriptOrStyleBlockIsEmitted() throws Exception
    {
        String indexHtml = this.getIndexHtml();

        assertNoRawPlaceholderSurvives(indexHtml);
        assertFalse(indexHtml.contains("prefers-color-scheme"), "AUTO script must not be emitted for LIGHT: " + indexHtml);
        assertFalse(indexHtml.contains("<style"), indexHtml);
    }

    @Test
    public void testBothThemeStylesheetsAreServedAsCssFromTheCoreUiArtifact() throws Exception
    {
        this.assertServedAsCss(MODERN_STYLESHEET);
        this.assertServedAsCss(STOCK_STYLESHEET);
    }
}
