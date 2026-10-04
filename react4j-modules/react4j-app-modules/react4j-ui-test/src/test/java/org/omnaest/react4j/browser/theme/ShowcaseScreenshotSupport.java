package org.omnaest.react4j.browser.theme;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.ReducedMotion;
import com.microsoft.playwright.options.ScreenshotAnimations;
import com.microsoft.playwright.options.ScreenshotCaret;

/**
 * plan-274 S5 / plan-277 S2b: shared vocabulary of the showcase screenshot tests ({@link ShowcaseScreenshotModernIT},
 * {@link ShowcaseScreenshotDisabledIT}, {@link ShowcaseScreenshotModernDarkIT}, {@link ShowcaseScreenshotTablerDarkIT} and
 * {@link ShowcaseScreenshotTablerComparisonIT}). Each subclass owns one Spring context (one theme mode), takes a full-page screenshot of the
 * ComponentShowcaseUI at the fixed {@link ComputedStyleProbes#VIEWPORT_WIDTH} viewport width, persists it under {@code docs/theme-screenshots} of the
 * React4J repository (outside {@code target}, so it survives {@code mvn clean}) and asserts one cheap, mode specific computed value so that the test is
 * not screenshot-only.
 * <p>
 * The image is <b>complete</b> (the viewport is grown to the full height of the showcase, with no cap, and the capture refuses to run while any element
 * still hides vertical overflow, see {@link #assertNoHiddenVerticalOverflow(Page)}) and <b>deterministic</b>:
 * <ul>
 * <li>server state: every subclass pins {@link #ISOLATED_CONTEXT_PROFILE} next to its theme profile, a marker no configuration reacts to, so the class
 * gets a Spring context of its own and no other test's clicks (the "Bump Interval Counter" button, the drag-and-drop demo) can reach the image;</li>
 * <li>motion: reduced motion is emulated, an injected style switches off every CSS animation, transition and the caret, and Playwright is asked to
 * finish or cancel animations before the capture;</li>
 * <li>time: timers of one second or more are never armed ({@link #FREEZE_LONG_TIMERS_SCRIPT}), so the carousel (5 s) does not advance and the
 * IntervalRerenderingContainer (2 s) does not re-render while the page is being captured; the one remaining time dependent text, the server's
 * "Server time: HH:mm:ss" paragraph, is rendered by the server from its wall clock and is therefore covered by a screenshot mask.</li>
 * </ul>
 */
abstract class ShowcaseScreenshotSupport extends ThemeBrowserSupport
{
    /**
     * Marker profile that gives a screenshot test its own cached Spring context (the context cache key includes the active profiles). Nothing reacts to it.
     */
    static final String         ISOLATED_CONTEXT_PROFILE  = "showcase-screenshot-isolated";

    /**
     * Arms no timer of one second or more. Test only: the screenshot is a still image, so the carousel's auto-advance (5 s) and the
     * IntervalRerenderingContainer refresh (2 s) are exactly the moving parts it must not contain. Idempotent, because one page can capture two servers.
     */
    private static final String FREEZE_LONG_TIMERS_SCRIPT = "(() => { if (window.__showcaseTimersFrozen) { return; } window.__showcaseTimersFrozen = true;"
                                                            + " const arm = (native) => function (handler, delay, ...args) {"
                                                            + " if (Number(delay) >= 1000) { return -1; } return native.call(window, handler, delay, ...args); };"
                                                            + " window.setTimeout = arm(window.setTimeout); window.setInterval = arm(window.setInterval); })();";

    private static final String NO_MOTION_STYLE           = "*, *::before, *::after { animation: none !important; transition: none !important;"
                                                            + " caret-color: transparent !important; scroll-behavior: auto !important; }";

    /**
     * Describes every element between the lowest card of the page and {@code <html>} that clips or scrolls away vertical content (the scroll chain
     * that decides whether the showcase is on the image), plus a pseudo entry when the lowest card ends below the document. One pixel of tolerance covers
     * sub-pixel rounding of scrollHeight. Clipping inside a component that is not an ancestor of page content (the diagram viewer crops its SVG by
     * design, screen reader only spans are clipped to one pixel) is none of this guard's business.
     */
    private static final String HIDDEN_OVERFLOW_JS        = "(() => { const describe = (e) => e.tagName.toLowerCase() + (e.id ? '#' + e.id : '') + (typeof e.className === 'string' && e.className.trim() ? '.' + e.className.trim().split(/\\s+/).join('.') : '')"
                                                            + " + ' (scrollHeight ' + e.scrollHeight + ' > clientHeight ' + e.clientHeight + ')';"
                                                            + " const cards = Array.from(document.querySelectorAll('.card')); if (cards.length === 0) { return ['no .card on the page']; }"
                                                            + " const bottomOf = (c) => c.getBoundingClientRect().bottom + window.scrollY;"
                                                            + " const last = cards.reduce((a, c) => bottomOf(c) > bottomOf(a) ? c : a); const problems = []; let overflow = 0;"
                                                            + " if (bottomOf(last) > document.documentElement.scrollHeight) { overflow = Math.max(overflow, Math.ceil(bottomOf(last) - document.documentElement.scrollHeight)); problems.push('lowest card ends at ' + bottomOf(last) + ', below the document height ' + document.documentElement.scrollHeight); }"
                                                            + " for (let e = last.parentElement; e && e !== document.documentElement; e = e.parentElement)"
                                                            + " { if (getComputedStyle(e).overflowY !== 'visible' && e.scrollHeight - e.clientHeight > 1) { overflow = Math.max(overflow, e.scrollHeight - e.clientHeight); problems.push(describe(e)); } }"
                                                            + " return { problems, overflow }; })()";

    /**
     * Chromium cannot render more than 16384 pixels of height into one screenshot, and the showcase is taller than that, so it is captured in sections
     * of at most this height and stitched
     */
    private static final int    SECTION_HEIGHT            = 8000;
    private static final String SETTLED_JS                = "document.querySelector('.App')?.getAttribute('data-inflight-count') === '0'";

    /**
     * Relative to the module directory, which is the working directory of the failsafe fork: react4j-ui-test -> react4j-app-modules -> react4j-modules
     * -> React4J
     */
    private static final Path   SCREENSHOT_DIRECTORY      = Paths.get("..", "..", "..", "docs", "theme-screenshots");

    /**
     * Opens the showcase, waits until the page is settled (every in-flight request answered, fonts loaded), writes the full-page screenshot to
     * {@code <fileName>} and returns the path written.
     */
    protected Path openShowcaseAndPersistScreenshot(String fileName) throws IOException
    {
        return persistShowcaseScreenshot(this.page, this.baseUrl(), fileName);
    }

    /**
     * Same as {@link #openShowcaseAndPersistScreenshot(String)} for an arbitrary page and server, so that one test can capture two servers (two
     * themes) in one browser session. {@code fileName} may name a sub directory of {@code docs/theme-screenshots}.
     */
    protected static Path persistShowcaseScreenshot(Page page, String baseUrl, String fileName) throws IOException
    {
        page.emulateMedia(new Page.EmulateMediaOptions().setReducedMotion(ReducedMotion.REDUCE));
        page.addInitScript(FREEZE_LONG_TIMERS_SCRIPT);
        page.setViewportSize(ComputedStyleProbes.VIEWPORT_WIDTH, ComputedStyleProbes.VIEWPORT_HEIGHT);
        page.navigate(baseUrl + "/");
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Open modal")
                                                                   .setExact(true))
            .waitFor(new Locator.WaitForOptions().setTimeout(15000));
        page.waitForFunction(SETTLED_JS);
        page.addStyleTag(new Page.AddStyleTagOptions().setContent(NO_MOTION_STYLE));
        page.evaluate("document.fonts.ready.then(() => true)");

        // The showcase scrolls inside an internal container (.body-full), so a plain full-page capture shows one viewport only. Grow the viewport by the
        // hidden overflow of the scroll chain above the lowest card, with no cap, so that the whole showcase is on the image; the width stays fixed. The
        // container is a flex child of a viewport high column, so growing the viewport grows the container, which is what makes the overflow disappear
        // (it is measured again after each growth, because a taller viewport can change the layout).
        for (int attempt = 0; attempt < 4 && scrollChain(page).overflow() > 0; attempt++)
        {
            page.setViewportSize(ComputedStyleProbes.VIEWPORT_WIDTH, page.viewportSize().height + scrollChain(page).overflow());
            page.waitForFunction(SETTLED_JS);
            page.evaluate("new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(resolve)))");
        }
        assertNoHiddenVerticalOverflow(page);
        int totalHeight = ((Number) page.evaluate("Math.ceil(document.documentElement.scrollHeight)")).intValue();
        List<BufferedImage> sections = new ArrayList<>();
        for (int top = 0; top < totalHeight; top += SECTION_HEIGHT)
        {
            int height = Math.min(SECTION_HEIGHT, totalHeight - top);
            byte[] png = page.screenshot(new Page.ScreenshotOptions().setFullPage(true)
                                                                     .setClip(0, top, ComputedStyleProbes.VIEWPORT_WIDTH, height)
                                                                     .setAnimations(ScreenshotAnimations.DISABLED)
                                                                     .setCaret(ScreenshotCaret.HIDE)
                                                                     .setMask(List.of(serverTimeParagraph(page))));
            BufferedImage section = ImageIO.read(new ByteArrayInputStream(png));
            if (section.getWidth() != ComputedStyleProbes.VIEWPORT_WIDTH || section.getHeight() != height)
            {
                throw new AssertionError("the section at y=" + top + " is " + section.getWidth() + "x" + section.getHeight() + " instead of "
                                         + ComputedStyleProbes.VIEWPORT_WIDTH + "x" + height + ", the browser cut it (viewport " + page.viewportSize().height + ")");
            }
            sections.add(section);
        }

        BufferedImage whole = new BufferedImage(ComputedStyleProbes.VIEWPORT_WIDTH, totalHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = whole.createGraphics();
        int top = 0;
        for (BufferedImage section : sections)
        {
            graphics.drawImage(section, 0, top, null);
            top += section.getHeight();
        }
        graphics.dispose();
        return write(whole, fileName);
    }

    private static Path write(BufferedImage image, String fileName) throws IOException
    {
        Path target = SCREENSHOT_DIRECTORY.resolve(fileName)
                                          .normalize();
        Files.createDirectories(target.getParent());
        if (!ImageIO.write(image, "png", target.toFile()))
        {
            throw new IOException("no PNG writer for " + target);
        }
        return target;
    }

    /**
     * Puts two persisted screenshots next to each other, top aligned, each under a caption, on a grey background, and persists the result as
     * {@code fileName}. Done on the pixels rather than in a browser page, because the images are taller than a browser can render into one screenshot.
     */
    protected static Path persistSideBySide(Path left, String leftCaption, Path right, String rightCaption, String fileName) throws IOException
    {
        int padding = 12;
        int gap = 24;
        int captionHeight = 28;
        BufferedImage leftImage = ImageIO.read(left.toFile());
        BufferedImage rightImage = ImageIO.read(right.toFile());
        int width = padding + leftImage.getWidth() + gap + rightImage.getWidth() + padding;
        int height = padding + captionHeight + Math.max(leftImage.getHeight(), rightImage.getHeight()) + padding;

        BufferedImage composite = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = composite.createGraphics();
        graphics.setColor(new Color(0x9aa0a6));
        graphics.fillRect(0, 0, width, height);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
        graphics.setColor(Color.WHITE);
        int rightX = padding + leftImage.getWidth() + gap;
        graphics.drawString(leftCaption, padding, padding + 20);
        graphics.drawString(rightCaption, rightX, padding + 20);
        graphics.drawImage(leftImage, padding, padding + captionHeight, null);
        graphics.drawImage(rightImage, rightX, padding + captionHeight, null);
        graphics.dispose();
        return write(composite, fileName);
    }
    /**
     * The "Server time: HH:mm:ss" paragraph of the IntervalRerenderingContainer card. The server renders its wall clock into it, which no client side
     * setting can freeze, so the screenshot masks it (a solid box over the paragraph's bounding box, whose size does not depend on the digits).
     */
    private static Locator serverTimeParagraph(Page page)
    {
        return page.locator(".card", new Page.LocatorOptions().setHasText("IntervalRerenderingContainer"))
                   .last()
                   .locator(".card-inner-body p");
    }

    private record ScrollChain(List<String> problems, int overflow) {
    }

    @SuppressWarnings("unchecked")
    private static ScrollChain scrollChain(Page page)
    {
        java.util.Map<String, Object> result = (java.util.Map<String, Object>) page.evaluate(HIDDEN_OVERFLOW_JS);
        return new ScrollChain((List<String>) result.get("problems"), ((Number) result.get("overflow")).intValue());
    }

    /**
     * Fails when any element above the lowest card still clips or scrolls away vertical content, so that a cut showcase can never be written silently
     */
    protected static void assertNoHiddenVerticalOverflow(Page page)
    {
        List<String> problems = scrollChain(page).problems();
        if (!problems.isEmpty())
        {
            throw new AssertionError("the screenshot would not contain the whole showcase, elements still hide vertical overflow (viewport height "
                                     + page.viewportSize().height + ", window.innerHeight " + page.evaluate("window.innerHeight") + "): " + problems);
        }
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
        return this.page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Open modal")
                                                                               .setExact(true));
    }
}
