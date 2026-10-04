package org.omnaest.react4j.service.internal.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import org.junit.jupiter.api.Test;
import org.omnaest.react4j.EnableReactUI;
import org.omnaest.react4j.data.annotations.EnableReactUIInMemoryRepository;
import org.omnaest.react4j.domain.configuration.ThemeConfiguration.ThemePreset;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

/**
 * plan-277 T2, AC4 (a): the seam between {@code ReactUIService#configureTheme(theme -> theme.preset(ThemePreset.TABLER))} and the served index.html,
 * against the REAL core-ui template. T3 adds the seam this class deferred: the stylesheet file itself, served from the installed core-ui artifact.
 */
@SpringBootTest(classes = IndexHtmlControllerTablerThemeTest.TestApplication.class, webEnvironment = WebEnvironment.MOCK)
@AutoConfigureMockMvc
public class IndexHtmlControllerTablerThemeTest extends AbstractIndexHtmlThemeTest
{
    private static final String TABLER_STYLESHEET = "/css/theme/react4j-tabler.css";

    @SpringBootApplication
    @EnableReactUI
    @EnableReactUIInMemoryRepository
    public static class TestApplication
    {
    }

    @Override
    protected void configureTheme()
    {
        this.reactUIService.configureTheme(theme -> theme.preset(ThemePreset.TABLER));
    }

    @Test
    public void testTheTablerLinkIsEmittedBeforeTheApplicationStylesheets() throws Exception
    {
        String indexHtml = this.getIndexHtml();

        int tablerIndex = indexOfStylesheetLink(indexHtml, TABLER_STYLESHEET);
        int appIndex = indexOfStylesheetLink(indexHtml, APP_STYLESHEET);
        assertTrue(tablerIndex < appIndex, "the tabler link must precede " + APP_STYLESHEET + ": " + indexHtml);
        assertTrue(indexHtml.contains("<link rel=\"stylesheet\" href=\"" + TABLER_STYLESHEET + "?"), indexHtml);
        assertFalse(cacheBusterOf(indexHtml, TABLER_STYLESHEET).isEmpty(), "the theme link carries the cache buster");
        assertEquals(cacheBusterOf(indexHtml, APP_STYLESHEET), cacheBusterOf(indexHtml, TABLER_STYLESHEET), "same cache buster as the application links");
        assertNoRawPlaceholderSurvives(indexHtml);
    }

    /**
     * plan-277 T3, AC5: the file the link points at is really served from the core-ui jar, as CSS, and is Tabler (its banner), not another sheet
     */
    @Test
    public void testTheTablerStylesheetIsServedAsCssAndStartsWithTheTablerBanner() throws Exception
    {
        this.assertServedAsCss(TABLER_STYLESHEET);

        String body = this.mockMvc.perform(get(TABLER_STYLESHEET))
                                  .andReturn()
                                  .getResponse()
                                  .getContentAsString();
        assertTrue(body.startsWith("/*!"), "the served sheet must start with the licence banner: " + body.substring(0, 80));
        assertTrue(body.substring(0, 400)
                       .contains("Tabler v"),
                   body.substring(0, 400));
        assertTrue(body.substring(0, 400)
                       .contains("MIT"),
                   body.substring(0, 400));
        assertTrue(body.length() > 500_000, "the full Tabler sheet is expected, got " + body.length() + " characters");
    }

    @Test
    public void testNeitherTheModernNorTheStockStylesheetIsLinked() throws Exception
    {
        String indexHtml = this.getIndexHtml();

        assertFalse(indexHtml.contains("react4j-modern.css"), indexHtml);
        assertFalse(indexHtml.contains("bootstrap.min.css"), indexHtml);
    }

    @Test
    public void testTheDefaultLightColorModeAttributeIsWrittenAndNoStyleBlockIsEmitted() throws Exception
    {
        String indexHtml = this.getIndexHtml();

        assertTrue(htmlTag(indexHtml).contains("data-bs-theme=\"light\""), htmlTag(indexHtml));
        assertFalse(indexHtml.contains("<style"), indexHtml);
    }
}
