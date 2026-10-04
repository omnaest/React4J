package org.omnaest.react4j.browser.theme;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import com.microsoft.playwright.Page;

/**
 * plan-277: colour reading for the Tabler theme browser tests. Tabler writes its colours as {@code oklch()}, {@code color-mix()} and
 * {@code color()} values, which {@code getComputedStyle} serialises in their own colour space instead of as {@code rgb()}; the one comparable form is
 * the sRGB bytes the browser's own canvas makes of the colour text.
 */
final class BrowserColors
{
    private BrowserColors()
    {
        // static helper
    }

    /**
     * The sRGB bytes (red, green, blue) the browser's canvas makes of any CSS colour text, including {@code oklch()}, {@code color-mix()} and
     * {@code color()} results. Fails when the browser cannot parse the text, so a typo can never read as the canvas's default colour.
     */
    static int[] srgbBytes(Page page, String cssColor)
    {
        Object result = page.evaluate("(css) => { const canvas = document.createElement('canvas'); canvas.width = 1; canvas.height = 1;"
                                      + " const context = canvas.getContext('2d'); context.fillStyle = '#010203'; context.fillStyle = css;"
                                      + " if (context.fillStyle === '#010203') { return null; }"
                                      + " context.clearRect(0, 0, 1, 1); context.fillRect(0, 0, 1, 1);"
                                      + " return Array.from(context.getImageData(0, 0, 1, 1).data); }",
                                      cssColor);
        assertTrue(result instanceof List, "the browser could not parse the colour '" + cssColor + "'");
        List<?> data = (List<?>) result;
        return new int[] {((Number) data.get(0)).intValue(), ((Number) data.get(1)).intValue(), ((Number) data.get(2)).intValue()};
    }

    /**
     * Largest absolute difference of any of the three channels
     */
    static int maxChannelDistance(int[] first, int[] second)
    {
        int distance = 0;
        for (int index = 0; index < 3; index++)
        {
            distance = Math.max(distance, Math.abs(first[index] - second[index]));
        }
        return distance;
    }

    /**
     * WCAG relative luminance of sRGB bytes
     */
    static double luminance(int[] rgb)
    {
        return 0.2126 * linear(rgb[0]) + 0.7152 * linear(rgb[1]) + 0.0722 * linear(rgb[2]);
    }

    private static double linear(int channel)
    {
        double value = channel / 255.0;
        return value <= 0.03928 ? value / 12.92 : Math.pow((value + 0.055) / 1.055, 2.4);
    }
}
