package org.omnaest.react4j.service.internal.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.omnaest.react4j.EnableReactUI;
import org.omnaest.react4j.data.annotations.EnableReactUIInMemoryRepository;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

/**
 * plan-274 S4 AC4.5: the seam between {@code ReactUIService#configureTheme} with design tokens and the served index.html, against the REAL
 * core-ui template. A primary colour plus a border radius must put exactly one {@code <style>} block into the head, after the modern theme link and
 * before the application stylesheets. The S3 default and disabled tests are untouched.
 */
@SpringBootTest(classes = IndexHtmlControllerTokenThemeTest.TestApplication.class, webEnvironment = WebEnvironment.MOCK)
@AutoConfigureMockMvc
public class IndexHtmlControllerTokenThemeTest extends AbstractIndexHtmlThemeTest
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
        this.reactUIService.configureTheme(theme -> theme.primaryColor("#0f766e")
                                                         .borderRadius("12px"));
    }

    @Test
    public void testTheStyleBlockFollowsTheModernLinkAndPrecedesTheApplicationStylesheet() throws Exception
    {
        String indexHtml = this.getIndexHtml();

        int modernIndex = indexOfStylesheetLink(indexHtml, MODERN_STYLESHEET);
        int styleIndex = indexHtml.indexOf("<style>");
        int appIndex = indexOfStylesheetLink(indexHtml, APP_STYLESHEET);
        assertTrue(styleIndex > 0, "no <style> block in: " + indexHtml);
        assertTrue(modernIndex < styleIndex && styleIndex < appIndex, "expected modern < style < color.css, got " + modernIndex + "," + styleIndex + "," + appIndex);
        assertEquals(1, count(indexHtml, "<style>"), "exactly one token style block: " + indexHtml);
        assertNoRawPlaceholderSurvives(indexHtml);
    }

    @Test
    public void testTheBlockCarriesTheDerivedTokensAndNothingThatCouldEndTheElement() throws Exception
    {
        String indexHtml = this.getIndexHtml();

        Matcher matcher = Pattern.compile("<style>(.*?)</style>", Pattern.DOTALL)
                                 .matcher(indexHtml);
        assertTrue(matcher.find(), "no style block in: " + indexHtml);
        String css = matcher.group(1);
        assertTrue(css.contains("--bs-primary:#0f766e"), css);
        assertTrue(css.contains("--bs-border-radius:12px"), css);
        assertTrue(css.contains("--bs-border-radius-sm:9px"), css);
        assertTrue(css.contains(".btn-primary{--bs-btn-color:#fff;--bs-btn-bg:#0f766e;"), css);
        assertTrue(css.contains("[data-bs-theme=dark]{"), css);
        assertFalse(css.contains("<"), "angle bracket inside the style block");
        assertTrue(htmlTag(indexHtml).contains("data-bs-theme=\"light\""), htmlTag(indexHtml));
    }

    @Test
    public void testTheModernStylesheetIsStillServed() throws Exception
    {
        this.assertServedAsCss(MODERN_STYLESHEET);
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
