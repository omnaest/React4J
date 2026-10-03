package org.omnaest.react4j.browser.theme;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.omnaest.react4j.MockApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Route;

/**
 * plan-274 S0: captures and re-checks the computed-style baseline of the stock-Bootstrap look of the showcase page.
 * Both tests are OPT-IN, driven by the system property {@value #MODE_PROPERTY}, so a plain {@code mvn verify
 * "-DexcludedGroups="} stays a no-op for them (they are skipped, never silently green):
 *
 * <pre>
 * # (re)record the fixture and the screenshot - run ONLY against a build whose look is the reference look
 * mvn verify "-DexcludedGroups=" "-Dit.test=ThemeBaselineIT" "-Dreact4j.theme.baseline=capture"
 *
 * # compare the current build against the recorded fixture
 * mvn verify "-DexcludedGroups=" "-Dit.test=ThemeBaselineIT" "-Dreact4j.theme.baseline=compare"
 * </pre>
 *
 * Capture writes {@value #FIXTURE_RESOURCE} below {@code src/test/resources} and {@code baseline-today.png} below
 * {@code target/theme-screenshots} (a copy under a persistent directory can be requested with
 * {@code -Dreact4j.theme.screenshotDir=<dir>}). Compare reads the fixture from the classpath, optionally applies
 * the INTENDED deltas listed in {@value #INTENDED_DELTAS_RESOURCE} (probe, then property, then the new value) and
 * fails on every other difference. An intended delta whose value equals the baseline is itself an error, so the
 * list cannot go stale unnoticed.
 */
@Tag("browser")
@SpringBootTest(classes = MockApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles(DisabledThemeTestConfiguration.PROFILE) // plan-274 S3: the baseline is the STOCK look, so it runs with the theme disabled
public class ThemeBaselineIT
{
    static final String               MODE_PROPERTY                = "react4j.theme.baseline";
    static final String               SCREENSHOT_DIR_PROPERTY      = "react4j.theme.screenshotDir";
    static final String               SIMULATE_FLIP_PROPERTY       = "react4j.theme.simulateFlip";
    static final String               CUSTOM_CSS_OVERRIDE_PROPERTY = "react4j.theme.customCssOverride";
    static final String               FIXTURE_RESOURCE             = "theme/stock-baseline-computed-styles.json";
    static final String               INTENDED_DELTAS_RESOURCE     = "theme/stock-baseline-intended-deltas.json";

    private static final ObjectMapper MAPPER                       = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT)
                                                                                       .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);

    @LocalServerPort
    private int                       port;

    private Playwright                playwright;
    private Browser                   browser;
    private Page                      page;

    @BeforeEach
    public void openBrowser()
    {
        this.playwright = Playwright.create();
        this.browser = this.playwright.chromium()
                                      .launch(new BrowserType.LaunchOptions().setHeadless(true));
        this.page = this.browser.newPage();
    }

    @AfterEach
    public void closeBrowser()
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

    @Test
    public void captureBaseline() throws IOException
    {
        assumeTrue("capture".equals(System.getProperty(MODE_PROPERTY)), "run with -D" + MODE_PROPERTY + "=capture");

        // With a custom.css override the capture re-records "today's look" for a probe added AFTER the original baseline
        // build existed (the override is the pre-clean-up file, served in today's stylesheet order). It never refreshes
        // the persistent screenshot, which stays the one taken from the unmodified build.
        boolean overridden = System.getProperty(CUSTOM_CSS_OVERRIDE_PROPERTY) != null;
        this.applyDiagnosticOverrides();
        this.page.navigate("http://localhost:" + this.port + "/");
        Map<String, Map<String, String>> styles = ComputedStyleProbes.capture(this.page);

        Path fixture = Paths.get(System.getProperty("basedir", System.getProperty("user.dir")), "src", "test", "resources", FIXTURE_RESOURCE);
        Files.createDirectories(fixture.getParent());
        MAPPER.writeValue(fixture.toFile(), styles);

        Path screenshot = Paths.get(System.getProperty("basedir", System.getProperty("user.dir")), "target", "theme-screenshots", "baseline-today.png");
        Files.createDirectories(screenshot.getParent());
        this.page.screenshot(new Page.ScreenshotOptions().setPath(screenshot)
                                                         .setFullPage(true));
        String persistentDir = System.getProperty(SCREENSHOT_DIR_PROPERTY);
        if (!overridden && persistentDir != null && !persistentDir.isBlank())
        {
            Files.createDirectories(Paths.get(persistentDir));
            Files.copy(screenshot, Paths.get(persistentDir, "baseline-today.png"), StandardCopyOption.REPLACE_EXISTING);
        }
        assertTrue(Files.size(fixture) > 0 && Files.size(screenshot) > 0);
    }

    @Test
    public void currentLookEqualsBaselineExceptIntendedDeltas() throws IOException
    {
        assumeTrue("compare".equals(System.getProperty(MODE_PROPERTY)), "run with -D" + MODE_PROPERTY + "=compare");

        Map<String, Map<String, String>> baseline = readResource(FIXTURE_RESOURCE, new TreeMap<>());
        Map<String, Map<String, String>> deltas = readResource(INTENDED_DELTAS_RESOURCE, new TreeMap<>());

        this.applyDiagnosticOverrides();
        this.page.navigate("http://localhost:" + this.port + "/");
        if (Boolean.getBoolean(SIMULATE_FLIP_PROPERTY))
        {
            // Equal-specificity ties between custom.css and Bootstrap are decided by stylesheet ORDER. Re-appending the
            // custom.css <link> after the bundle's link reproduces the order the server will emit once Bootstrap
            // leaves the JS bundle (plan-274 S3), without needing that change yet.
            this.page.evaluate("() => { const l = document.querySelector('link[href^=\"/css/custom.css\"]'); document.head.appendChild(l); }");
        }
        Map<String, Map<String, String>> actual = ComputedStyleProbes.capture(this.page);

        List<String> problems = new ArrayList<>();
        if (!baseline.keySet()
                     .equals(actual.keySet()))
        {
            problems.add("probe set differs: baseline=" + baseline.keySet() + " actual=" + actual.keySet());
        }
        baseline.forEach((probe, expectedProps) -> expectedProps.forEach((property, expected) ->
        {
            String actualValue = actual.getOrDefault(probe, Map.of())
                                       .get(property);
            String intended = deltas.getOrDefault(probe, Map.of())
                                    .get(property);
            if (intended != null)
            {
                if (intended.equals(expected))
                {
                    problems.add("stale intended delta (equals baseline) " + probe + "/" + property + ": " + expected);
                }
                else if (!intended.equals(actualValue))
                {
                    problems.add(probe + "/" + property + ": intended '" + intended + "' but actual '" + actualValue + "'");
                }
            }
            else if (!expected.equals(actualValue))
            {
                problems.add(probe + "/" + property + ": baseline '" + expected + "' but actual '" + actualValue + "'");
            }
        }));
        assertTrue(problems.isEmpty(), "Computed styles differ from the stock baseline:\n" + String.join("\n", problems));
    }

    /**
     * Freshness guard for the compare run (testing P12): the jar under test must carry the plan-274 S2 clean-up. A
     * stale core-ui jar would still serve the dead {@code .card-header} colours and would make the comparison above
     * meaningless, because stale and fresh builds look identical today.
     */
    @Test
    public void servedCustomCssNoLongerCarriesTheDeadCardHeaderColours()
    {
        assumeTrue("compare".equals(System.getProperty(MODE_PROPERTY)), "run with -D" + MODE_PROPERTY + "=compare");

        String customCss = this.page.request()
                                    .get("http://localhost:" + this.port + "/css/custom.css")
                                    .text();

        assertTrue(customCss.contains(".page-navigation"), "custom.css must be the framework file, got: " + customCss.substring(0, Math.min(80, customCss.length())));
        assertFalse(customCss.contains("background-color: blue"), "stale core-ui jar: custom.css still sets the card header to blue");
    }

    /** Optional, compare mode only: serve the file at this path instead of /css/custom.css (to show the probes can go red). */
    private void applyDiagnosticOverrides() throws IOException
    {
        String override = System.getProperty(CUSTOM_CSS_OVERRIDE_PROPERTY);
        if (override != null && !override.isBlank())
        {
            String body = Files.readString(Paths.get(override));
            this.page.route("**/css/custom.css*", route -> route.fulfill(new Route.FulfillOptions().setContentType("text/css")
                                                                                                   .setBody(body)));
        }
    }

    private static Map<String, Map<String, String>> readResource(String resource, Map<String, Map<String, String>> defaultValue) throws IOException
    {
        try (InputStream stream = ThemeBaselineIT.class.getClassLoader()
                                                       .getResourceAsStream(resource))
        {
            if (stream == null)
            {
                return defaultValue;
            }
            return MAPPER.readValue(stream, new TypeReference<Map<String, Map<String, String>>>() {
            });
        }
    }
}
