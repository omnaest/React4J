package org.omnaest.react4j.service.internal.controller;

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
 * plan-274 S3: the seam between {@code ReactUIService#configureTheme} and the served index.html for a NON-default but enabled configuration. The
 * theme is disabled first and re-enabled by {@code useDefault()}, which must keep the colour mode and the added stylesheets configured in between.
 */
@SpringBootTest(classes = IndexHtmlControllerConfiguredThemeTest.TestApplication.class, webEnvironment = WebEnvironment.MOCK)
@AutoConfigureMockMvc
public class IndexHtmlControllerConfiguredThemeTest extends AbstractIndexHtmlThemeTest
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
        this.reactUIService.configureTheme(theme -> theme.disable()
                                                         .colorMode(ColorMode.DARK)
                                                         .addStylesheet("/css/first-added.css")
                                                         .addStylesheet("/css/second-added.css?a=1&b=2"))
                           .configureTheme(theme -> theme.useDefault());
    }

    @Test
    public void testDarkColorModeAndTheAddedStylesheetsSurviveReEnablingTheTheme() throws Exception
    {
        String indexHtml = this.getIndexHtml();

        assertTrue(htmlTag(indexHtml).contains("data-bs-theme=\"dark\""), htmlTag(indexHtml));
        assertFalse(indexHtml.contains("bootstrap.min.css"), indexHtml);
        assertNoRawPlaceholderSurvives(indexHtml);
    }

    @Test
    public void testAddedStylesheetsFollowTheModernLinkInInsertionOrderAndPrecedeTheApplicationStylesheets() throws Exception
    {
        String indexHtml = this.getIndexHtml();

        int modernIndex = indexOfStylesheetLink(indexHtml, MODERN_STYLESHEET);
        int firstIndex = indexOfStylesheetLink(indexHtml, "/css/first-added.css");
        int secondIndex = indexOfStylesheetLink(indexHtml, "/css/second-added.css");
        int appIndex = indexOfStylesheetLink(indexHtml, APP_STYLESHEET);
        assertTrue(modernIndex < firstIndex && firstIndex < secondIndex && secondIndex < appIndex,
                   "expected order modern < first < second < app, got " + modernIndex + "," + firstIndex + "," + secondIndex + "," + appIndex + " in: "
                                                                                                   + indexHtml);
        assertTrue(indexHtml.contains("href=\"/css/second-added.css?a=1&amp;b=2\""), "the url must be emitted attribute-escaped: " + indexHtml);
    }
}
