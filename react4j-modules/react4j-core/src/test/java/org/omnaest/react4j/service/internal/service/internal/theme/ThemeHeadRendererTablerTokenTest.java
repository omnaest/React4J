/*******************************************************************************
 * Copyright 2021 Danny Kunz
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package org.omnaest.react4j.service.internal.service.internal.theme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.omnaest.react4j.domain.configuration.ThemeConfiguration.ColorMode;
import org.omnaest.react4j.domain.configuration.ThemeConfiguration.ShadowStrength;
import org.omnaest.react4j.domain.configuration.ThemeConfiguration.ThemePreset;
import org.omnaest.react4j.service.internal.service.CssLength;
import org.omnaest.react4j.service.internal.service.RgbColor;
import org.omnaest.react4j.service.internal.service.ThemeColorRole;
import org.omnaest.react4j.service.internal.service.ThemeSettings;
import org.omnaest.react4j.service.internal.service.ThemeTokens;

/**
 * plan-277 T3, AC2: what a {@link ThemeHeadRenderer} does with design tokens on the {@link ThemePreset#TABLER} preset: the block of
 * {@link TablerTokenCss}, in the same place as the modern one (one {@code <style>} directly after the theme link, before added stylesheets, only when
 * a token is set), never the {@link ThemeTokenCss} component overrides. Algorithmic node, no mocks.
 */
public class ThemeHeadRendererTablerTokenTest
{
    private static final String      CACHE_BUSTER = "4711";

    private static final String      TABLER_LINK  = "<link rel=\"stylesheet\" href=\"/css/theme/react4j-tabler.css?4711\"/>";
    private static final String      ADDED_LINK   = "<link rel=\"stylesheet\" href=\"/css/added.css\"/>";
    private static final String      STOCK_LINK   = "<link rel=\"stylesheet\" href=\"/css/theme/bootstrap.min.css?4711\"/>";

    private static final ThemeTokens TOKENS       = ThemeTokens.none()
                                                               .withColor(ThemeColorRole.PRIMARY, RgbColor.parseHex("#0f766e"))
                                                               .withBorderRadius(CssLength.parse("12px"));

    private static ThemeSettings.ThemeSettingsBuilder tabler()
    {
        return ThemeSettings.builder()
                            .preset(ThemePreset.TABLER);
    }

    @Test
    public void testTablerWithTokensEmitsOneStyleBlockAfterTheTablerLinkAndBeforeTheAddedStylesheets()
    {
        ThemeHead head = ThemeHeadRenderer.render(tabler().tokens(TOKENS)
                                                          .stylesheet("/css/added.css")
                                                          .build(),
                                                  CACHE_BUSTER);

        assertEquals(TABLER_LINK + "\n<style>" + TablerTokenCss.render(TOKENS) + "</style>\n" + ADDED_LINK, head.getHeadMarkup());
        assertEquals("data-bs-theme=\"light\"", head.getHtmlAttribute());
    }

    @Test
    public void testTheStyleBlockIsTheTablerWritersAndNeverTheModernComponentOverrides()
    {
        String markup = ThemeHeadRenderer.render(tabler().tokens(TOKENS)
                                                         .build(),
                                                 CACHE_BUSTER)
                                         .getHeadMarkup();

        assertTrue(markup.contains("--bs-primary:#0f766e;--bs-primary-rgb:15, 118, 110"), markup);
        assertTrue(markup.contains("--bs-border-radius-md:12px"), markup);
        assertEquals(1, markup.split("<style>", -1).length - 1, "exactly one style block: " + markup);
        assertFalse(markup.contains(".btn-"), "no component selector must reach a Tabler page: " + markup);
        assertFalse(markup.contains(ThemeTokenCss.render(TOKENS)), "the modern writer's output must not appear on a Tabler page: " + markup);
        assertFalse(markup.contains("--bs-btn-"), markup);
    }

    @Test
    public void testTablerWithoutTokensEmitsNoStyleBlock()
    {
        ThemeHead head = ThemeHeadRenderer.render(tabler().stylesheet("/css/added.css")
                                                          .build(),
                                                  CACHE_BUSTER);

        assertEquals(TABLER_LINK + "\n" + ADDED_LINK, head.getHeadMarkup());
        assertFalse(head.getHeadMarkup()
                        .contains("<style"));
    }

    /**
     * The default shadow strength is Tabler as compiled, so on its own it writes nothing and then no empty style element is emitted either
     */
    @Test
    public void testTablerWithOnlyTheDefaultShadowStrengthEmitsNoStyleBlock()
    {
        ThemeHead head = ThemeHeadRenderer.render(tabler().tokens(ThemeTokens.none()
                                                                             .withShadowStrength(ShadowStrength.DEFAULT))
                                                          .build(),
                                                  CACHE_BUSTER);

        assertEquals(TABLER_LINK, head.getHeadMarkup());
    }

    @Test
    public void testDisabledTablerWithTokensRendersTheStockLinkOnly()
    {
        ThemeHead head = ThemeHeadRenderer.render(tabler().enabled(false)
                                                          .tokens(TOKENS)
                                                          .stylesheet("/css/added.css")
                                                          .build(),
                                                  CACHE_BUSTER);

        assertEquals(STOCK_LINK, head.getHeadMarkup());
        assertEquals("", head.getHtmlAttribute());
    }

    @Test
    public void testTheAutoColourModeScriptStillComesBeforeTheTablerLinkAndTheStyleBlockFollowsTheLink()
    {
        String markup = ThemeHeadRenderer.render(tabler().colorMode(ColorMode.AUTO)
                                                         .tokens(TOKENS)
                                                         .build(),
                                                 CACHE_BUSTER)
                                         .getHeadMarkup();

        assertTrue(markup.indexOf("<script>") < markup.indexOf(TABLER_LINK), markup);
        assertTrue(markup.indexOf(TABLER_LINK) < markup.indexOf("<style>"), markup);
    }

    /**
     * The defence in depth of {@code styleBlock} applies to the Tabler block exactly as to the modern one
     */
    @Test
    public void testTheAngleBracketDefenceStillGuardsTheBlock()
    {
        assertThrows(IllegalStateException.class, () -> ThemeHeadRenderer.styleBlock("a{b:c}</style><script>"));
    }
}
