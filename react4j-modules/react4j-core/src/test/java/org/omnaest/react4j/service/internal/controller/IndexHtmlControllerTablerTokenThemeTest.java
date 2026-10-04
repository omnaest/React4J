package org.omnaest.react4j.service.internal.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.omnaest.react4j.EnableReactUI;
import org.omnaest.react4j.data.annotations.EnableReactUIInMemoryRepository;
import org.omnaest.react4j.domain.configuration.ThemeConfiguration.ShadowStrength;
import org.omnaest.react4j.domain.configuration.ThemeConfiguration.ThemePreset;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

/**
 * plan-277 T3, AC5 (b): the seam between {@code ReactUIService#configureTheme} with the Tabler preset AND design tokens and the served index.html,
 * against the REAL core-ui template. The tokens must put exactly one {@code <style>} block of Tabler base variables into the head, after the Tabler
 * link and before the application stylesheets, and none of the component selectors the modern writer would emit. The S3/S4 modern tests and the T2
 * Tabler tests are untouched.
 */
@SpringBootTest(classes = IndexHtmlControllerTablerTokenThemeTest.TestApplication.class, webEnvironment = WebEnvironment.MOCK)
@AutoConfigureMockMvc
public class IndexHtmlControllerTablerTokenThemeTest extends AbstractIndexHtmlThemeTest
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
        this.reactUIService.configureTheme(theme -> theme.preset(ThemePreset.TABLER)
                                                         .primaryColor("#0f766e")
                                                         .borderRadius("12px")
                                                         .shadowStrength(ShadowStrength.NONE));
    }

    @Test
    public void testTheStyleBlockFollowsTheTablerLinkAndPrecedesTheApplicationStylesheet() throws Exception
    {
        String indexHtml = this.getIndexHtml();

        int tablerIndex = indexOfStylesheetLink(indexHtml, TABLER_STYLESHEET);
        int styleIndex = indexHtml.indexOf("<style>");
        int appIndex = indexOfStylesheetLink(indexHtml, APP_STYLESHEET);
        assertTrue(styleIndex > 0, "no <style> block in: " + indexHtml);
        assertTrue(tablerIndex < styleIndex && styleIndex < appIndex, "expected tabler < style < color.css, got " + tablerIndex + "," + styleIndex + "," + appIndex);
        assertEquals(1, count(indexHtml, "<style>"), "exactly one token style block: " + indexHtml);
        assertFalse(indexHtml.contains("react4j-modern.css"), indexHtml);
        assertNoRawPlaceholderSurvives(indexHtml);
    }

    @Test
    public void testTheBlockCarriesTheTablerBaseVariablesAndNoComponentSelector() throws Exception
    {
        String indexHtml = this.getIndexHtml();

        Matcher matcher = Pattern.compile("<style>(.*?)</style>", Pattern.DOTALL)
                                 .matcher(indexHtml);
        assertTrue(matcher.find(), "no style block in: " + indexHtml);
        String css = matcher.group(1);
        assertTrue(css.startsWith(":root,[data-bs-theme=light],[data-theme=light]{"), css);
        assertTrue(css.contains("--bs-primary:#0f766e;--bs-primary-rgb:15, 118, 110"), css);
        assertTrue(css.contains("--bs-border-radius-md:12px"), css);
        assertTrue(css.contains("--bs-border-radius-sm:calc(12px * 2 / 3)"), css);
        assertTrue(css.contains("--bs-shadow-color:light-dark(rgba(18, 18, 23, 0), rgba(0, 0, 0, 0))"), css);
        assertEquals(1, count(css, "{"), "one rule only: " + css);
        assertFalse(css.contains(".btn"), "no component selector may reach a Tabler page: " + css);
        assertFalse(css.contains("[data-bs-theme=dark]"), css);
        assertFalse(css.contains("<"), "angle bracket inside the style block");
        assertTrue(htmlTag(indexHtml).contains("data-bs-theme=\"light\""), htmlTag(indexHtml));
    }

    private static int count(String text, String part)
    {
        int count = 0;
        for (int index = text.indexOf(part); index >= 0; index = text.indexOf(part, index + part.length()))
        {
            count++;
        }
        return count;
    }
}
