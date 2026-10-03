package org.omnaest.react4j.browser.theme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.omnaest.react4j.MockApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;

import com.microsoft.playwright.Locator;

/**
 * plan-274 S3, AC3.4 (disabled half): the same application with the theme disabled through the
 * {@link DisabledThemeTestConfiguration#PROFILE} profile must look EXACTLY like the stock Bootstrap look recorded before the theme existed
 * ({@value ThemeBrowserSupport#STOCK_BASELINE_RESOURCE}, plan-274 S0): zero computed-style deltas over the whole probe set.
 */
@Tag("browser")
@SpringBootTest(classes = MockApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles(DisabledThemeTestConfiguration.PROFILE)
public class DisabledThemeIT extends ThemeBrowserSupport
{
    @Test
    public void testServedPageLinksOnlyTheStockStylesheet()
    {
        String servedHtml = this.fetchServedIndexHtml();
        String head = servedHtml.substring(0, servedHtml.indexOf("</head>"));

        assertTrue(head.contains("<link rel=\"stylesheet\" href=\"/css/theme/bootstrap.min.css?"),
                   "freshness: the served page must carry the stock theme link, a stale core jar was probably used. Head: " + head);
        assertFalse(head.contains("react4j-modern"), head);
        assertFalse(head.contains("data-bs-theme"), head);
        assertFalse(head.contains("<script>"), head);
        assertFalse(head.contains("<style"), head);
        assertFalse(head.contains(DisabledThemeTestConfiguration.SUPPRESSED_STYLESHEET), head);
    }

    @Test
    public void testOpenModalButtonIsRenderedInStockBootstrapPrimaryColorAndRadius() throws IOException
    {
        String stockRadius = readStockBaseline().get("button-open-modal")
                                                .get("border-radius");
        Locator openModalButton = this.openShowcaseAndLocateOpenModalButton();

        assertEquals("rgb(13, 110, 253)", computedStyle(openModalButton, "background-color"));
        assertEquals(stockRadius, computedStyle(openModalButton, "border-radius"));
    }

    @Test
    public void testWholeProbeSetEqualsTheStockBaselineWithZeroDeltas() throws IOException
    {
        Map<String, Map<String, String>> baseline = readStockBaseline();

        Map<String, Map<String, String>> actual;
        this.page.navigate(this.baseUrl() + "/");
        actual = ComputedStyleProbes.capture(this.page);

        List<String> deltas = new ArrayList<>();
        if (!baseline.keySet()
                     .equals(actual.keySet()))
        {
            deltas.add("probe set differs: baseline=" + baseline.keySet() + " actual=" + actual.keySet());
        }
        baseline.forEach((probe, expectedProperties) -> expectedProperties.forEach((property, expected) ->
        {
            String actualValue = actual.getOrDefault(probe, Map.of())
                                       .get(property);
            if (!expected.equals(actualValue))
            {
                deltas.add(probe + "/" + property + ": baseline '" + expected + "' but actual '" + actualValue + "'");
            }
        }));
        assertTrue(deltas.isEmpty(), "Disabled theme must equal the stock baseline, deltas:\n" + String.join("\n", deltas));
    }
}
