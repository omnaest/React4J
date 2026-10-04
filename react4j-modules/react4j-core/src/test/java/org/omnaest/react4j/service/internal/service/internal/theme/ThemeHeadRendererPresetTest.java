package org.omnaest.react4j.service.internal.service.internal.theme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.omnaest.react4j.domain.configuration.ThemeConfiguration.ColorMode;
import org.omnaest.react4j.domain.configuration.ThemeConfiguration.ThemePreset;
import org.omnaest.react4j.service.internal.service.CssLength;
import org.omnaest.react4j.service.internal.service.RgbColor;
import org.omnaest.react4j.service.internal.service.ThemeColorRole;
import org.omnaest.react4j.service.internal.service.ThemeSettings;
import org.omnaest.react4j.service.internal.service.ThemeTokens;

/**
 * plan-277 T2, AC1 and AC2: the theme preset of the head renderer. The algorithmic node: a {@link ThemeSettings} snapshot in, markup out, no
 * collaborators, no mocks. The pre-existing preset-less expectations stay in {@link ThemeHeadRendererTest} and {@link ThemeHeadRendererTokenTest}
 * untouched; this class adds the full preset x enabled x colour mode matrix and what the Tabler preset changes.
 */
public class ThemeHeadRendererPresetTest
{
    private static final String CACHE_BUSTER = "4711";

    private static final String MODERN_LINK  = "<link rel=\"stylesheet\" href=\"/css/theme/react4j-modern.css?4711\"/>";
    private static final String TABLER_LINK  = "<link rel=\"stylesheet\" href=\"/css/theme/react4j-tabler.css?4711\"/>";
    private static final String STOCK_LINK   = "<link rel=\"stylesheet\" href=\"/css/theme/bootstrap.min.css?4711\"/>";

    private static final String TABLER_PATH  = "/css/theme/react4j-tabler.css";
    private static final String MODERN_PATH  = "/css/theme/react4j-modern.css";
    private static final String STOCK_PATH   = "/css/theme/bootstrap.min.css";

    static Stream<Arguments> presetEnabledColorModeMatrix()
    {
        List<Arguments> combinations = new ArrayList<>();
        for (ThemePreset preset : ThemePreset.values())
        {
            for (boolean enabled : new boolean[] {true, false})
            {
                for (ColorMode colorMode : ColorMode.values())
                {
                    combinations.add(Arguments.of(preset, enabled, colorMode));
                }
            }
        }
        return combinations.stream();
    }

    /**
     * The matrix is 2 presets x 2 enabled states x 3 colour modes, written out here so a new enum constant cannot silently pass unchecked
     */
    @Test
    public void testTheMatrixCoversEveryCombination()
    {
        assertEquals(2, ThemePreset.values().length, "a new preset needs its own expectations in the matrix below");
        assertEquals(3, ColorMode.values().length, "a new colour mode needs its own expectations in the matrix below");
        assertEquals(2 * 2 * 3, presetEnabledColorModeMatrix().count());
    }

    @ParameterizedTest(name = "{0} enabled={1} {2}")
    @MethodSource("presetEnabledColorModeMatrix")
    public void testEveryPresetEnabledColorModeCombinationRendersTheExpectedHead(ThemePreset preset, boolean enabled, ColorMode colorMode)
    {
        ThemeHead head = ThemeHeadRenderer.render(ThemeSettings.builder()
                                                               .preset(preset)
                                                               .enabled(enabled)
                                                               .colorMode(colorMode)
                                                               .build(),
                                                  CACHE_BUSTER);

        String markup = head.getHeadMarkup();
        if (!enabled)
        {
            assertEquals(STOCK_LINK, markup, "disabled output is the stock link only, whatever the preset and colour mode");
            assertEquals("", head.getHtmlAttribute());
            return;
        }

        String expectedLink = preset == ThemePreset.TABLER ? TABLER_LINK : MODERN_LINK;
        String otherPresetPath = preset == ThemePreset.TABLER ? MODERN_PATH : TABLER_PATH;
        assertEquals(1, count(markup, "<link"), "exactly one stylesheet link: " + markup);
        assertTrue(markup.contains(expectedLink), markup);
        assertFalse(markup.contains(otherPresetPath), markup);
        assertFalse(markup.contains(STOCK_PATH), markup);
        assertFalse(markup.contains("<style"), markup);
        switch (colorMode)
        {
            case LIGHT :
                assertEquals(expectedLink, markup);
                assertEquals("data-bs-theme=\"light\"", head.getHtmlAttribute());
                break;
            case DARK :
                assertEquals(expectedLink, markup);
                assertEquals("data-bs-theme=\"dark\"", head.getHtmlAttribute());
                break;
            case AUTO :
                assertEquals("", head.getHtmlAttribute());
                assertEquals(1, count(markup, "<script>"), markup);
                assertTrue(markup.contains("prefers-color-scheme: dark"), markup);
                assertTrue(markup.indexOf("<script>") < markup.indexOf(expectedLink), "the script must come before the stylesheet link: " + markup);
                assertTrue(markup.endsWith(expectedLink), "the link ends the head when nothing else is configured: " + markup);
                break;
            default :
                throw new IllegalStateException("unexpected colour mode " + colorMode);
        }
    }

    @Test
    public void testAnExplicitModernPresetIsByteIdenticalToTheDefaultSettings()
    {
        ThemeHead explicit = ThemeHeadRenderer.render(ThemeSettings.builder()
                                                                   .preset(ThemePreset.MODERN)
                                                                   .build(),
                                                      CACHE_BUSTER);

        assertEquals(ThemeHeadRenderer.render(ThemeSettings.defaults(), CACHE_BUSTER), explicit);
        assertEquals(MODERN_LINK, explicit.getHeadMarkup());
    }

    @Test
    public void testTablerAddedStylesheetsFollowTheTablerLinkInInsertionOrder()
    {
        ThemeHead head = ThemeHeadRenderer.render(ThemeSettings.builder()
                                                               .preset(ThemePreset.TABLER)
                                                               .stylesheet("/css/z-added-first.css")
                                                               .stylesheet("/css/a-added-second.css")
                                                               .build(),
                                                  CACHE_BUSTER);

        assertEquals(TABLER_LINK + "\n" + "<link rel=\"stylesheet\" href=\"/css/z-added-first.css\"/>" + "\n"
                     + "<link rel=\"stylesheet\" href=\"/css/a-added-second.css\"/>", head.getHeadMarkup());
        assertEquals("data-bs-theme=\"light\"", head.getHtmlAttribute());
    }

    @Test
    public void testTablerWithAutoColorModeKeepsTheScriptBeforeTheTablerLinkAndTheAddedStylesheetsAfterIt()
    {
        ThemeHead head = ThemeHeadRenderer.render(ThemeSettings.builder()
                                                               .preset(ThemePreset.TABLER)
                                                               .colorMode(ColorMode.AUTO)
                                                               .stylesheet("/css/added.css")
                                                               .build(),
                                                  CACHE_BUSTER);

        String markup = head.getHeadMarkup();
        assertTrue(markup.indexOf("<script>") < markup.indexOf(TABLER_LINK), markup);
        assertTrue(markup.indexOf(TABLER_LINK) < markup.indexOf("/css/added.css"), markup);
        assertEquals("", head.getHtmlAttribute());
    }

    /**
     * What a TABLER head does with design tokens is {@link ThemeHeadRendererTablerTokenTest}'s subject (plan-277 T3). This is the control that the very
     * same tokens DO produce a style block for MODERN, so the Tabler expectations there are the preset's doing and not an artefact of the fixture.
     */
    @Test
    public void testTheSameDesignTokensDoProduceAStyleBlockForModern()
    {
        ThemeTokens tokens = ThemeTokens.none()
                                        .withColor(ThemeColorRole.PRIMARY, RgbColor.parseHex("#0f766e"))
                                        .withBorderRadius(CssLength.parse("12px"));

        ThemeHead head = ThemeHeadRenderer.render(ThemeSettings.builder()
                                                               .preset(ThemePreset.MODERN)
                                                               .tokens(tokens)
                                                               .build(),
                                                  CACHE_BUSTER);

        assertTrue(head.getHeadMarkup()
                       .contains("<style>"),
                   head.getHeadMarkup());
    }

    @Test
    public void testDisabledTablerWithTokensAndAddedStylesheetsRendersTheStockLinkOnly()
    {
        ThemeTokens tokens = ThemeTokens.none()
                                        .withColor(ThemeColorRole.PRIMARY, RgbColor.parseHex("#0f766e"));

        ThemeHead head = ThemeHeadRenderer.render(ThemeSettings.builder()
                                                               .preset(ThemePreset.TABLER)
                                                               .enabled(false)
                                                               .tokens(tokens)
                                                               .stylesheet("/css/added.css")
                                                               .build(),
                                                  CACHE_BUSTER);

        assertEquals(STOCK_LINK, head.getHeadMarkup());
        assertEquals("", head.getHtmlAttribute());
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
