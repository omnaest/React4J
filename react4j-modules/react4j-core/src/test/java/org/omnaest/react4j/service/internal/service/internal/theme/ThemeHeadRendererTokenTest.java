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
import org.omnaest.react4j.service.internal.service.RgbColor;
import org.omnaest.react4j.service.internal.service.ThemeColorRole;
import org.omnaest.react4j.service.internal.service.ThemeSettings;
import org.omnaest.react4j.service.internal.service.ThemeTokens;

/**
 * plan-274 S4 AC4.3: the head the renderer produces when design tokens are set. The S3 expectations for settings without tokens stay in
 * {@link ThemeHeadRendererTest} unchanged, this class only adds what the tokens change. Pure function, no mocks.
 */
public class ThemeHeadRendererTokenTest
{
    private static final String      CACHE_BUSTER = "4711";
    private static final String      MODERN_LINK  = "<link rel=\"stylesheet\" href=\"/css/theme/react4j-modern.css?4711\"/>";
    private static final String      STOCK_LINK   = "<link rel=\"stylesheet\" href=\"/css/theme/bootstrap.min.css?4711\"/>";

    private static final ThemeTokens TEAL         = ThemeTokens.none()
                                                               .withColor(ThemeColorRole.PRIMARY, RgbColor.parseHex("#0f766e"));

    @Test
    public void testWithoutTokensTheHeadIsExactlyTheS3DefaultAndHasNoStyleBlock()
    {
        ThemeHead head = ThemeHeadRenderer.render(ThemeSettings.builder()
                                                               .tokens(ThemeTokens.none())
                                                               .build(),
                                                  CACHE_BUSTER);

        assertEquals(MODERN_LINK, head.getHeadMarkup(), "byte identical to the S3 expectation for default settings");
        assertEquals("data-bs-theme=\"light\"", head.getHtmlAttribute());
        assertFalse(head.getHeadMarkup()
                        .contains("<style"));
        assertEquals(ThemeHeadRenderer.render(ThemeSettings.defaults(), CACHE_BUSTER), head);
    }

    @Test
    public void testPrimaryOnlyAddsOneStyleBlockWithTheRootVariablesTheDarkBlockAndTheButtonVariants()
    {
        ThemeHead head = ThemeHeadRenderer.render(ThemeSettings.builder()
                                                               .tokens(TEAL)
                                                               .build(),
                                                  CACHE_BUSTER);

        String markup = head.getHeadMarkup();
        assertTrue(markup.startsWith(MODERN_LINK + "\n<style>:root,[data-bs-theme=light]{"), markup.substring(0, Math.min(200, markup.length())));
        assertTrue(markup.endsWith("</style>"), "the style block ends the head markup when no stylesheet is added");
        assertEquals(1, count(markup, "<style>"));
        assertEquals(1, count(markup, "</style>"));
        assertTrue(markup.contains("--bs-primary:#0f766e"), markup);
        assertTrue(markup.contains("[data-bs-theme=dark]{--bs-primary-text-emphasis:"), "the dark block");
        // the derived values are Bootstrap Sass' own for #0f766e (see theme-sheet-golden.json, primary-teal)
        assertTrue(markup.contains(".btn-primary{--bs-btn-color:#fff;--bs-btn-bg:#0f766e;--bs-btn-border-color:#0f766e;--bs-btn-hover-color:#fff;"
                                   + "--bs-btn-hover-bg:rgb(5%, 39.3333333333%, 36.6666666667%);"),
                   markup);
        assertTrue(markup.contains(".btn-outline-primary{--bs-btn-color:#0f766e;"), markup);
        assertEquals("data-bs-theme=\"light\"", head.getHtmlAttribute());
    }

    @Test
    public void testTheStyleBlockFollowsTheModernLinkAndPrecedesTheAddedStylesheets()
    {
        ThemeHead head = ThemeHeadRenderer.render(ThemeSettings.builder()
                                                               .tokens(TEAL)
                                                               .stylesheet("/css/added.css")
                                                               .build(),
                                                  CACHE_BUSTER);

        String markup = head.getHeadMarkup();
        int modern = markup.indexOf(MODERN_LINK);
        int style = markup.indexOf("<style>");
        int added = markup.indexOf("<link rel=\"stylesheet\" href=\"/css/added.css\"/>");
        assertTrue(modern == 0 && modern < style && style < added, "modern " + modern + ", style " + style + ", added " + added);
    }

    @Test
    public void testTheColourModeScriptStillComesFirstThenTheModernLinkThenTheStyleBlock()
    {
        ThemeHead head = ThemeHeadRenderer.render(ThemeSettings.builder()
                                                               .colorMode(ColorMode.AUTO)
                                                               .tokens(TEAL)
                                                               .build(),
                                                  CACHE_BUSTER);

        String markup = head.getHeadMarkup();
        assertTrue(markup.indexOf("<script>") == 0 && markup.indexOf("<script>") < markup.indexOf(MODERN_LINK) && markup.indexOf(MODERN_LINK) < markup.indexOf("<style>"),
                   markup);
        assertEquals("", head.getHtmlAttribute());
    }

    @Test
    public void testDisabledWithTokensRendersTheStockLinkOnlyAndNoStyleBlock()
    {
        ThemeHead head = ThemeHeadRenderer.render(ThemeSettings.builder()
                                                               .enabled(false)
                                                               .tokens(TEAL.withBorderRadius(org.omnaest.react4j.service.internal.service.CssLength.parse("12px")))
                                                               .colorMode(ColorMode.DARK)
                                                               .stylesheet("/css/added.css")
                                                               .build(),
                                                  CACHE_BUSTER);

        assertEquals(STOCK_LINK, head.getHeadMarkup());
        assertEquals("", head.getHtmlAttribute());
        assertFalse(head.getHeadMarkup()
                        .contains("<style"));
    }

    @Test
    public void testTheStyleBlockRefusesCssThatCouldEndTheElementAsDefenceInDepth()
    {
        assertEquals("<style>a{b:c}</style>", ThemeHeadRenderer.styleBlock("a{b:c}"));

        assertThrows(IllegalStateException.class, () -> ThemeHeadRenderer.styleBlock("a{b:c}</style><script>alert(1)</script>"));
        assertThrows(IllegalStateException.class, () -> ThemeHeadRenderer.styleBlock("<"));
        assertThrows(IllegalStateException.class, () -> ThemeHeadRenderer.styleBlock("a{b:'<'}"));
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
