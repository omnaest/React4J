package org.omnaest.react4j.browser.theme;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.web.server.LocalServerPort;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.AriaRole;

/**
 * Shared Playwright lifecycle and vocabulary of the plan-274 S3 theme browser tests. The subclass supplies the Spring context (default or
 * {@link DisabledThemeTestConfiguration#PROFILE disabled}); this class never decides which theme mode runs.
 */
abstract class ThemeBrowserSupport
{
    static final String         STOCK_BASELINE_RESOURCE = "theme/stock-baseline-computed-styles.json";

    private static final String READ_STYLE_JS           = "(el, props) => { const s = getComputedStyle(el); const r = {};"
                                                          + " for (const p of props) { r[p] = s.getPropertyValue(p); } return r; }";

    @LocalServerPort
    private int                 port;

    private Playwright          playwright;
    private Browser             browser;
    protected Page              page;

    @BeforeEach
    void openBrowser()
    {
        this.playwright = Playwright.create();
        this.browser = this.playwright.chromium()
                                      .launch(new BrowserType.LaunchOptions().setHeadless(true));
        this.page = this.browser.newPage();
    }

    @AfterEach
    void closeBrowser()
    {
        if (this.browser != null)
        {
            this.browser.close();
        }
        if (this.playwright != null)
        {
            this.playwright.close();
        }
    }

    protected String baseUrl()
    {
        return "http://localhost:" + this.port;
    }

    /**
     * The index.html exactly as the server serves it (no browser processing). Freshness guard of every theme test (testing P12): a stale core-ui
     * or core jar serves a page without the theme slot replaced, which must fail loudly here instead of making the computed-style assertions
     * meaningless.
     */
    protected String fetchServedIndexHtml()
    {
        return this.page.request()
                        .get(this.baseUrl() + "/")
                        .text();
    }

    /**
     * Opens the showcase and returns the one "Open modal" button, located by its exact accessible name (testing P11)
     */
    protected Locator openShowcaseAndLocateOpenModalButton()
    {
        this.page.setViewportSize(ComputedStyleProbes.VIEWPORT_WIDTH, ComputedStyleProbes.VIEWPORT_HEIGHT);
        this.page.navigate(this.baseUrl() + "/");
        Locator button = this.page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Open modal")
                                                                                         .setExact(true));
        button.waitFor(new Locator.WaitForOptions().setTimeout(15000));
        return button;
    }

    protected static String computedStyle(Locator element, String cssProperty)
    {
        @SuppressWarnings("unchecked")
        Map<String, String> styles = (Map<String, String>) element.evaluate(READ_STYLE_JS, java.util.List.of(cssProperty));
        return styles.get(cssProperty);
    }

    protected static Map<String, Map<String, String>> readStockBaseline() throws IOException
    {
        try (InputStream stream = ThemeBrowserSupport.class.getClassLoader()
                                                           .getResourceAsStream(STOCK_BASELINE_RESOURCE))
        {
            if (stream == null)
            {
                throw new IllegalStateException("Missing test resource " + STOCK_BASELINE_RESOURCE);
            }
            return new ObjectMapper().readValue(stream, new TypeReference<Map<String, Map<String, String>>>() {
            });
        }
    }
}
