package org.omnaest.react4j.browser.theme;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/**
 * plan-274 S5: shared vocabulary of the three showcase screenshot tests ({@link ShowcaseScreenshotModernIT}, {@link ShowcaseScreenshotDisabledIT},
 * {@link ShowcaseScreenshotModernDarkIT}). Each subclass owns one Spring context (one theme mode), takes a full-page screenshot of the
 * ComponentShowcaseUI at the fixed {@link ComputedStyleProbes#VIEWPORT_WIDTH} x {@link ComputedStyleProbes#VIEWPORT_HEIGHT} viewport, persists it under
 * {@code docs/theme-screenshots} of the React4J repository (outside {@code target}, so it survives {@code mvn clean}) and asserts one cheap, mode
 * specific computed value so that the test is not screenshot-only.
 */
abstract class ShowcaseScreenshotSupport extends ThemeBrowserSupport
{
    private static final int  MAX_EXTRA_HEIGHT     = 6000;

    /**
     * Relative to the module directory, which is the working directory of the failsafe fork: react4j-ui-test -> react4j-app-modules -> react4j-modules
     * -> React4J
     */
    private static final Path SCREENSHOT_DIRECTORY = Paths.get("..", "..", "..", "docs", "theme-screenshots");

    /**
     * Opens the showcase, waits until the page is settled (every in-flight request answered, fonts loaded), writes the full-page screenshot to
     * {@code <fileName>} and returns the path written.
     */
    protected Path openShowcaseAndPersistScreenshot(String fileName) throws IOException
    {
        this.openShowcaseAndLocateOpenModalButton();
        this.page.waitForFunction("document.querySelector('.App')?.getAttribute('data-inflight-count') === '0'");
        this.page.evaluate("document.fonts.ready.then(() => true)");

        // The showcase scrolls inside an internal container, so a plain full-page capture shows one viewport only. Grow the viewport by the largest
        // hidden overflow (capped) so that the whole showcase is on the image; the width stays fixed.
        int hiddenOverflow = ((Number) this.page.evaluate("Math.max(0, ...Array.from(document.querySelectorAll('*')).map(e => e.scrollHeight - e.clientHeight))")).intValue();
        this.page.setViewportSize(ComputedStyleProbes.VIEWPORT_WIDTH, ComputedStyleProbes.VIEWPORT_HEIGHT + Math.min(hiddenOverflow, MAX_EXTRA_HEIGHT));
        this.page.waitForFunction("document.querySelector('.App')?.getAttribute('data-inflight-count') === '0'");

        Files.createDirectories(SCREENSHOT_DIRECTORY);
        Path target = SCREENSHOT_DIRECTORY.resolve(fileName)
                                          .normalize();
        this.page.screenshot(new Page.ScreenshotOptions().setPath(target)
                                                         .setFullPage(true));
        return target;
    }

    /**
     * The {@code data-bs-theme} attribute of the {@code <html>} element, or {@code null} when it is absent
     */
    protected String htmlColorMode()
    {
        return (String) this.page.evaluate("document.documentElement.getAttribute('data-bs-theme')");
    }

    protected String bodyBackgroundColor()
    {
        return computedStyle(this.page.locator("body"), "background-color");
    }

    /**
     * Sum of the red, green and blue channels of an {@code rgb(r, g, b)} computed colour, as a cheap brightness measure
     */
    protected static int channelSum(String rgb)
    {
        String[] parts = rgb.replaceAll("[^0-9,]", "")
                            .split(",");
        return Integer.parseInt(parts[0]) + Integer.parseInt(parts[1]) + Integer.parseInt(parts[2]);
    }

    protected Locator openModalButton()
    {
        return this.page.getByRole(com.microsoft.playwright.options.AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Open modal")
                                                                                                                .setExact(true));
    }
}
