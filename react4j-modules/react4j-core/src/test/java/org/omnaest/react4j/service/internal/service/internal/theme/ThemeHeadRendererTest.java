package org.omnaest.react4j.service.internal.service.internal.theme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.omnaest.react4j.domain.configuration.ThemeConfiguration.ColorMode;
import org.omnaest.react4j.service.internal.service.ThemeSettings;

/**
 * plan-274 S3, AC3.1: the algorithmic node. Input is a {@link ThemeSettings} snapshot, output is markup; no collaborators, no mocks.
 */
public class ThemeHeadRendererTest
{
    private static final String CACHE_BUSTER = "4711";

    private static final String MODERN_LINK  = "<link rel=\"stylesheet\" href=\"/css/theme/react4j-modern.css?4711\"/>";
    private static final String STOCK_LINK   = "<link rel=\"stylesheet\" href=\"/css/theme/bootstrap.min.css?4711\"/>";

    @Test
    public void testDefaultSettingsRenderTheModernLinkAndTheLightAttribute()
    {
        ThemeHead head = ThemeHeadRenderer.render(ThemeSettings.defaults(), CACHE_BUSTER);

        assertEquals(MODERN_LINK, head.getHeadMarkup());
        assertEquals("data-bs-theme=\"light\"", head.getHtmlAttribute());
    }

    @Test
    public void testDarkColorModeRendersTheDarkAttribute()
    {
        ThemeHead head = ThemeHeadRenderer.render(ThemeSettings.builder()
                                                               .colorMode(ColorMode.DARK)
                                                               .build(),
                                                  CACHE_BUSTER);

        assertEquals(MODERN_LINK, head.getHeadMarkup());
        assertEquals("data-bs-theme=\"dark\"", head.getHtmlAttribute());
    }

    @Test
    public void testAutoColorModeRendersNoAttributeButTheScriptBeforeTheStylesheetLink()
    {
        ThemeHead head = ThemeHeadRenderer.render(ThemeSettings.builder()
                                                               .colorMode(ColorMode.AUTO)
                                                               .build(),
                                                  CACHE_BUSTER);

        assertEquals("", head.getHtmlAttribute());
        String markup = head.getHeadMarkup();
        int scriptIndex = markup.indexOf("<script>");
        int linkIndex = markup.indexOf(MODERN_LINK);
        assertTrue(scriptIndex >= 0, "the colour mode script must be present: " + markup);
        assertTrue(markup.contains("prefers-color-scheme: dark"), markup);
        assertTrue(markup.contains("document.documentElement.setAttribute(\"data-bs-theme\""), markup);
        assertTrue(scriptIndex < linkIndex, "the script must come before the stylesheet link: " + markup);
    }

    @Test
    public void testDisabledThemeRendersExactlyTheStockLinkEvenWithDarkAndAnAddedStylesheetConfigured()
    {
        ThemeHead head = ThemeHeadRenderer.render(ThemeSettings.builder()
                                                               .enabled(false)
                                                               .colorMode(ColorMode.DARK)
                                                               .stylesheet("/css/added.css")
                                                               .build(),
                                                  CACHE_BUSTER);

        assertEquals(STOCK_LINK, head.getHeadMarkup());
        assertEquals("", head.getHtmlAttribute());
    }

    @Test
    public void testDisabledThemeRendersNoScriptForAutoColorMode()
    {
        ThemeHead head = ThemeHeadRenderer.render(ThemeSettings.builder()
                                                               .enabled(false)
                                                               .colorMode(ColorMode.AUTO)
                                                               .build(),
                                                  CACHE_BUSTER);

        assertEquals(STOCK_LINK, head.getHeadMarkup());
        assertFalse(head.getHeadMarkup()
                        .contains("<script"));
    }

    @Test
    public void testAddedStylesheetsFollowTheModernLinkInInsertionOrderAndAreAttributeEscaped()
    {
        ThemeHead head = ThemeHeadRenderer.render(ThemeSettings.builder()
                                                               .stylesheet("/css/z-added-first.css")
                                                               .stylesheet("/css/a-added-second.css?x=\"1\"&y=<2>'3'")
                                                               .build(),
                                                  CACHE_BUSTER);

        assertEquals(MODERN_LINK + "\n" + "<link rel=\"stylesheet\" href=\"/css/z-added-first.css\"/>" + "\n"
                     + "<link rel=\"stylesheet\" href=\"/css/a-added-second.css?x=&quot;1&quot;&amp;y=&lt;2&gt;&#39;3&#39;\"/>", head.getHeadMarkup());
        assertEquals("data-bs-theme=\"light\"", head.getHtmlAttribute());
    }
}
