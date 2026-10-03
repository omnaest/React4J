package org.omnaest.react4j.browser.theme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.omnaest.react4j.MockApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

import com.microsoft.playwright.Locator;

/**
 * plan-274 S3, AC3.4 (modern half): {@link MockApplication} configures no theme, so the default - modern, light - must be what a real browser renders.
 * The expected values are the compiled theme settings of plan-274 S1: accent {@code #4f46e5} and a radius of 8px.
 */
@Tag("browser")
@SpringBootTest(classes = MockApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
public class ModernThemeIT extends ThemeBrowserSupport
{
    private static final String MODERN_STYLESHEET_PATH = "/css/theme/react4j-modern.css";

    @Test
    public void testServedPageLinksTheModernStylesheetAndCarriesTheLightColorMode()
    {
        String servedHtml = this.fetchServedIndexHtml();

        assertTrue(servedHtml.contains("<link rel=\"stylesheet\" href=\"" + MODERN_STYLESHEET_PATH + "?"),
                   "freshness: the served page must carry the theme link, a stale core jar was probably used. Head: " + head(servedHtml));
        assertTrue(servedHtml.contains("data-bs-theme=\"light\""), head(servedHtml));
        assertFalse(servedHtml.contains("bootstrap.min.css"), head(servedHtml));
    }

    @Test
    public void testModernStylesheetIsActuallyLoadedWithRulesInTheBrowser()
    {
        this.openShowcaseAndLocateOpenModalButton();

        Object loadedRuleCount = this.page.evaluate("() => { const sheets = Array.from(document.styleSheets).filter(s => s.href && s.href.includes('"
                                                    + MODERN_STYLESHEET_PATH + "'));" + " return sheets.length === 1 ? sheets[0].cssRules.length : -sheets.length; }");

        assertTrue(((Number) loadedRuleCount).intValue() > 0,
                   "exactly one document.styleSheets entry for " + MODERN_STYLESHEET_PATH + " with cssRules > 0 expected, got " + loadedRuleCount);
        assertEquals("light", this.page.evaluate("() => document.documentElement.getAttribute('data-bs-theme')"));
    }

    @Test
    public void testOpenModalButtonIsRenderedInTheModernAccentColorAndRadius()
    {
        Locator openModalButton = this.openShowcaseAndLocateOpenModalButton();

        assertEquals("rgb(79, 70, 229)", computedStyle(openModalButton, "background-color"));
        assertEquals("8px", computedStyle(openModalButton, "border-radius"));
    }

    private static String head(String html)
    {
        int end = html.indexOf("</head>");
        return end < 0 ? html : html.substring(0, end);
    }
}
