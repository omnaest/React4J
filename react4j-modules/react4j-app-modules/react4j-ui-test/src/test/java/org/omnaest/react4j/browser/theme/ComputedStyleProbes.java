package org.omnaest.react4j.browser.theme;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.regex.Pattern;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

/**
 * plan-274 S0: a reusable, fixed set of computed-style probes over {@link org.omnaest.react4j.ComponentShowcaseUI}.
 * It records how the page LOOKS (the browser's resolved style values), independent of which CSS rules produced them,
 * so "theme disabled == today" can be proven by comparing two dumps.
 *
 * <p>
 * Every probe is located by explicit identity - an exact accessible name, an exact card title, an exact placeholder
 * or an id on a fixture this class injects itself - never by the first match of a Bootstrap class (testing P11). Each
 * locator must resolve to EXACTLY one element, otherwise {@link #capture(Page)} fails loudly instead of silently
 * measuring a coincidental element.
 *
 * <p>
 * <b>Injected fixtures.</b> The showcase page renders no card header, no footer and no Bootstrap {@code .row}
 * outside a card, although {@code custom.css} styles all of them. A tiny block with stock Bootstrap markup and ids
 * {@value #FIXTURE_ID_PREFIX}* is therefore appended to {@code body} by this helper, so those rules are probed
 * against the real stylesheet cascade without changing the application page. The block is removed again afterwards.
 */
public final class ComputedStyleProbes
{
    public static final String                                FIXTURE_ID_PREFIX = "probe-fixture-";

    /** The computed properties recorded per probe (kebab-case CSS names). */
    public static final List<String>                          PROPERTIES        = List.of("background-color",
                                                                                          "color",
                                                                                          "font-family",
                                                                                          "font-size",
                                                                                          "border-radius",
                                                                                          "border-top-color",
                                                                                          "padding",
                                                                                          "margin-top",
                                                                                          "width");

    /** Fixed viewport so that width-dependent values are reproducible. */
    public static final int                                   VIEWPORT_WIDTH    = 1280;
    public static final int                                   VIEWPORT_HEIGHT   = 900;

    private static final String                               FIXTURE_HTML      = "<div id='" + FIXTURE_ID_PREFIX + "root'>"
                                                                                  + "<div id='" + FIXTURE_ID_PREFIX + "card' class='card'>"
                                                                                  + "<div id='" + FIXTURE_ID_PREFIX + "card-header' class='card-header'>Probe header</div>"
                                                                                  + "<div id='" + FIXTURE_ID_PREFIX + "card-footer' class='card-footer'>Probe footer</div>"
                                                                                  + "</div>"
                                                                                  + "<div id='" + FIXTURE_ID_PREFIX + "row' class='row'><div class='col'>Probe column</div></div>"
                                                                                  + "<div id='" + FIXTURE_ID_PREFIX + "toast' class='toast show' role='alert'>"
                                                                                  + "<div class='toast-body'>Probe toast</div></div>"
                                                                                  + "</div>";

    private static final String                               READ_STYLE_JS     = "(el, props) => { const s = getComputedStyle(el); const r = {};"
                                                                                  + " for (const p of props) { r[p] = s.getPropertyValue(p); } return r; }";

    private static final Map<String, Function<Page, Locator>> PROBES            = createProbes();

    private ComputedStyleProbes()
    {
    }

    /** Names of all probes, in capture order. */
    public static List<String> probeNames()
    {
        return List.copyOf(PROBES.keySet());
    }

    /**
     * Waits for the showcase to be rendered, injects the fixtures, reads the computed styles of every probe and
     * removes the fixtures. Result: probe name to (CSS property to computed value), both sorted by key.
     */
    public static Map<String, Map<String, String>> capture(Page page)
    {
        page.setViewportSize(VIEWPORT_WIDTH, VIEWPORT_HEIGHT);
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Open modal")
                                                                   .setExact(true))
            .waitFor(new Locator.WaitForOptions().setTimeout(15000));
        page.waitForLoadState(com.microsoft.playwright.options.LoadState.NETWORKIDLE);
        page.evaluate("html => document.body.insertAdjacentHTML('beforeend', html)", FIXTURE_HTML);
        try
        {
            Map<String, Map<String, String>> result = new TreeMap<>();
            PROBES.forEach((name, locatorFactory) ->
            {
                Locator locator = locatorFactory.apply(page);
                int count = locator.count();
                if (count != 1)
                {
                    throw new IllegalStateException("Probe '" + name + "' must resolve to exactly one element but matched " + count);
                }
                @SuppressWarnings("unchecked")
                Map<String, String> styles = (Map<String, String>) locator.evaluate(READ_STYLE_JS, PROPERTIES);
                result.put(name, new TreeMap<>(styles));
            });
            return result;
        }
        finally
        {
            page.evaluate("id => document.getElementById(id).remove()", FIXTURE_ID_PREFIX + "root");
        }
    }

    private static Map<String, Function<Page, Locator>> createProbes()
    {
        Map<String, Function<Page, Locator>> probes = new LinkedHashMap<>();
        probes.put("body", page -> page.locator("body"));
        probes.put("h2-component-showcase",
                   page -> page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("Component Showcase")
                                                                                       .setExact(true)
                                                                                       .setLevel(2)));
        probes.put("button-open-modal", page -> page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Open modal")
                                                                                                           .setExact(true)));
        probes.put("card-badge", page -> cardTitled(page, "Badge"));
        probes.put("card-badge-body", page -> cardTitled(page, "Badge").locator("> .card-body"));
        probes.put("card-badge-title", page -> cardTitled(page, "Badge").locator("div.card-title"));
        probes.put("toast-notice",
                   page -> cardTitled(page, "Toaster").locator("div.toast")
                                                      .filter(new Locator.FilterOptions().setHasText(Pattern.compile("^Notice"))));
        probes.put("badge-new", page -> cardTitled(page, "Badge").locator(".badge")
                                                                 .filter(new Locator.FilterOptions().setHasText(Pattern.compile("^New$"))));
        probes.put("alert-info", page -> cardTitled(page, "Alert").getByRole(AriaRole.ALERT));
        probes.put("input-showcase-name",
                   page -> page.getByPlaceholder("Enter your name", new Page.GetByPlaceholderOptions().setExact(true)));
        probes.put("fixture-card-header", page -> page.locator("#" + FIXTURE_ID_PREFIX + "card-header"));
        probes.put("fixture-card-footer", page -> page.locator("#" + FIXTURE_ID_PREFIX + "card-footer"));
        probes.put("fixture-row", page -> page.locator("#" + FIXTURE_ID_PREFIX + "row"));
        probes.put("fixture-toast", page -> page.locator("#" + FIXTURE_ID_PREFIX + "toast"));
        // LAST on purpose: the NavigationBar is only mounted once the hamburger toggler is clicked (HomePage.tsx starts
        // collapsed), and opening it shifts the layout below it, so every other probe is read before this one.
        probes.put("nav-link-navigation-target-a", page ->
        {
            Locator link = page.locator("a.nav-link")
                               .filter(new Locator.FilterOptions().setHasText(Pattern.compile("^Navigation Target A$")));
            if (link.count() == 0)
            {
                page.locator(".navbar-toggler")
                    .click();
                link.waitFor(new Locator.WaitForOptions().setTimeout(10000));
            }
            return link;
        });
        // Added during S2: it exposes custom.css' .page-navigation rule, which ties with Bootstrap's .nav padding-left.
        // The ul is the only element carrying that class; it exists once the navigation was opened by the probe above.
        probes.put("nav-pills-page-navigation", page -> page.locator("ul.nav.nav-pills.page-navigation"));
        return probes;
    }

    /** The one {@code .card} whose title heading is exactly {@code title}. */
    private static Locator cardTitled(Page page, String title)
    {
        Locator titleHeading = page.locator("div.card-title")
                                   .filter(new Locator.FilterOptions().setHasText(Pattern.compile("^" + title.replaceAll("[\\\\^$.|?*+()\\[\\]{}]", "\\\\$0") + "$")));
        return page.locator("div.card")
                   .filter(new Locator.FilterOptions().setHas(titleHeading));
    }
}
