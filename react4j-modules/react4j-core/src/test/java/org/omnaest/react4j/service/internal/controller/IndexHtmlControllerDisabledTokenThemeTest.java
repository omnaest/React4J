package org.omnaest.react4j.service.internal.controller;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;
import org.omnaest.react4j.EnableReactUI;
import org.omnaest.react4j.data.annotations.EnableReactUIInMemoryRepository;
import org.omnaest.react4j.domain.configuration.ThemeConfiguration.ShadowStrength;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

/**
 * plan-274 S4 AC4.3 at the seam: design tokens are configured and the theme is then disabled. The served page must be exactly the stock page: the
 * stock stylesheet link and no style block at all, because tokens only take effect while the theme is enabled.
 */
@SpringBootTest(classes = IndexHtmlControllerDisabledTokenThemeTest.TestApplication.class, webEnvironment = WebEnvironment.MOCK)
@AutoConfigureMockMvc
public class IndexHtmlControllerDisabledTokenThemeTest extends AbstractIndexHtmlThemeTest
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
                                                         .fontFamily("Inter, sans-serif")
                                                         .borderRadius("12px")
                                                         .shadowStrength(ShadowStrength.STRONG)
                                                         .disable());
    }

    @Test
    public void testDisabledThemeWithTokensServesTheStockLinkAndNoStyleBlock() throws Exception
    {
        String indexHtml = this.getIndexHtml();

        indexOfStylesheetLink(indexHtml, STOCK_STYLESHEET);
        assertFalse(indexHtml.contains(MODERN_STYLESHEET), indexHtml);
        assertFalse(indexHtml.contains("<style"), "a disabled theme must not emit the token block: " + indexHtml);
        assertFalse(indexHtml.contains("--bs-primary"), indexHtml);
        assertFalse(htmlTag(indexHtml).contains("data-bs-theme"), "a disabled theme writes no colour mode attribute: " + htmlTag(indexHtml));
        assertNoRawPlaceholderSurvives(indexHtml);
    }
}
