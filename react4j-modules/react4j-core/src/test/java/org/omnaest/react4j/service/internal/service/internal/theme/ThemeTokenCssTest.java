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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.Test;
import org.omnaest.react4j.domain.configuration.ThemeConfiguration.ShadowStrength;
import org.omnaest.react4j.service.internal.service.CssLength;
import org.omnaest.react4j.service.internal.service.FontFamily;
import org.omnaest.react4j.service.internal.service.RgbColor;
import org.omnaest.react4j.service.internal.service.ThemeColorRole;
import org.omnaest.react4j.service.internal.service.ThemeTokens;

/**
 * plan-274 S4: the CSS text rendered from design tokens, as an algorithmic unit (tokens in, text out, no mocks). The values themselves are proven
 * against Bootstrap's Sass by {@link ThemeTokenCssSheetGoldenTest}; this class covers the shape of the output, the typography, shape and shadow
 * tokens, the cascade order and the property that no input can ever put an angle bracket into the block.
 */
public class ThemeTokenCssTest
{
    private static final String LIGHT = "|:root,[data-bs-theme=light]|";
    private static final String DARK  = "|[data-bs-theme=dark]|";

    @Test
    public void testNoTokenRendersNothing()
    {
        assertEquals("", ThemeTokenCss.render(ThemeTokens.none()));
    }

    @Test
    public void testPrimaryOnlyRendersTheRootVariablesTheDarkBlockAndTheButtonVariantsWithDerivedValues()
    {
        Map<String, CssSheet.Declaration> css = render(ThemeTokens.none()
                                                                  .withColor(ThemeColorRole.PRIMARY, RgbColor.parseHex("#0f766e")));

        assertEquals("#0f766e", css.get(LIGHT + "--bs-primary")
                                   .getValue());
        assertEquals("15, 118, 110", css.get(LIGHT + "--bs-primary-rgb")
                                        .getValue());
        assertEquals("#0f766e", css.get(LIGHT + "--bs-link-color")
                                   .getValue());
        assertEquals("rgba(15, 118, 110, 0.4)", css.get(LIGHT + "--bs-focus-ring-color")
                                                   .getValue());
        assertTrue(css.containsKey(DARK + "--bs-primary-text-emphasis"), "the dark block must carry the dark variants");
        assertTrue(css.containsKey(DARK + "--bs-link-color"), "the dark block must carry the dark link colour");

        // values from Bootstrap's Sass for #0f766e (theme-sheet-golden.json, case primary-teal): shade-color(primary, 15%) etc.
        assertEquals("rgb(5%, 39.3333333333%, 36.6666666667%)", css.get("|.btn-primary|--bs-btn-hover-bg")
                                                                   .getValue());
        assertEquals("rgb(4.7058823529%, 37.0196078431%, 34.5098039216%)", css.get("|.btn-primary|--bs-btn-active-bg")
                                                                              .getValue());
        assertEquals("51, 139, 132", css.get("|.btn-primary|--bs-btn-focus-shadow-rgb")
                                        .getValue());
        assertEquals("#fff", css.get("|.btn-primary|--bs-btn-color")
                                .getValue());
        assertEquals("#0f766e", css.get("|.btn-outline-primary|--bs-btn-color")
                                   .getValue());
        assertEquals("#0f766e", css.get("|.btn-outline-primary|--bs-btn-hover-bg")
                                   .getValue());
        assertEquals("transparent", css.get("|.btn-outline-primary|--bs-btn-disabled-bg")
                                       .getValue());
    }

    @Test
    public void testAColourThatTakesBlackTextFlipsTheTextColourOfEveryDerivedState()
    {
        Map<String, CssSheet.Declaration> css = render(ThemeTokens.none()
                                                                  .withColor(ThemeColorRole.PRIMARY, RgbColor.parseHex("#ffe066")));

        assertEquals("#000", css.get("|.btn-primary|--bs-btn-color")
                                .getValue());
        assertEquals("#000", css.get("|.btn-primary|--bs-btn-hover-color")
                                .getValue());
        assertEquals("#000", css.get("|.btn-outline-primary|--bs-btn-hover-color")
                                .getValue());
        assertEquals("#000", css.get("|.text-bg-primary|color")
                                .getValue());
        assertTrue(css.get("|.text-bg-primary|color")
                      .isImportant(),
                   "Bootstrap's .text-bg-* colour is !important, so must be the override");
    }

    @Test
    public void testOnlyTheConfiguredColoursRenderRules()
    {
        Map<String, CssSheet.Declaration> css = render(ThemeTokens.none()
                                                                  .withColor(ThemeColorRole.SUCCESS, RgbColor.parseHex("#0e9f6e")));

        assertTrue(css.containsKey("|.btn-success|--bs-btn-bg"));
        assertTrue(css.containsKey(LIGHT + "--bs-form-valid-color"));
        assertTrue(css.containsKey(DARK + "--bs-form-valid-color"), "the dark block must restate the dark validation colour");
        assertTrue(css.containsKey("|.was-validated .form-control:valid,.form-control.is-valid|background-image"));
        assertFalse(css.containsKey("|.btn-primary|--bs-btn-bg"));
        assertFalse(css.containsKey("|.btn-danger|--bs-btn-bg"));
        assertFalse(css.containsKey(LIGHT + "--bs-primary"));
        assertFalse(css.containsKey(LIGHT + "--bs-link-color"), "the link colour follows the primary colour only");
    }

    @Test
    public void testTheFontFamilyAndTheBaseFontSizeAreWrittenToTheRootAndTheLiteralPlaces()
    {
        Map<String, CssSheet.Declaration> css = render(ThemeTokens.none()
                                                                  .withFontFamily(FontFamily.parse("Inter ,  \"Segoe UI\",sans-serif"))
                                                                  .withBaseFontSize(CssLength.parse(".875rem")));

        assertEquals("Inter, \"Segoe UI\", sans-serif", css.get(LIGHT + "--bs-body-font-family")
                                                           .getValue());
        assertEquals("0.875rem", css.get(LIGHT + "--bs-body-font-size")
                                    .getValue());
        assertEquals("Inter, \"Segoe UI\", sans-serif", css.get("|.tooltip|font-family")
                                                           .getValue());
        assertEquals("Inter, \"Segoe UI\", sans-serif", css.get("|.popover|font-family")
                                                           .getValue());
        assertFalse(css.containsKey(DARK + "--bs-primary-text-emphasis"));
    }

    @Test
    public void testTheRadiusScaleFollowsTheRatiosOfTheModernThemeInPixelsAndRem()
    {
        Map<String, CssSheet.Declaration> px = render(ThemeTokens.none()
                                                                 .withBorderRadius(CssLength.parse("20px")));
        assertEquals("20px", px.get(LIGHT + "--bs-border-radius")
                               .getValue());
        assertEquals("15px", px.get(LIGHT + "--bs-border-radius-sm")
                               .getValue());
        assertEquals("30px", px.get(LIGHT + "--bs-border-radius-lg")
                               .getValue());
        assertEquals("40px", px.get(LIGHT + "--bs-border-radius-xl")
                               .getValue());
        assertEquals("15px", px.get("|kbd|border-radius")
                               .getValue());
        assertEquals("20px", px.get("|.btn-close|border-radius")
                               .getValue());

        Map<String, CssSheet.Declaration> rem = render(ThemeTokens.none()
                                                                  .withBorderRadius(CssLength.parse("0.5rem")));
        assertEquals("0.375rem", rem.get(LIGHT + "--bs-border-radius-sm")
                                    .getValue());
        assertEquals("0.75rem", rem.get(LIGHT + "--bs-border-radius-lg")
                                   .getValue());
        assertEquals("1rem", rem.get(LIGHT + "--bs-border-radius-xl")
                                .getValue());

        assertEquals("0px", render(ThemeTokens.none()
                                              .withBorderRadius(CssLength.parse("0px"))).get(LIGHT + "--bs-border-radius")
                                                                                        .getValue());
    }

    @Test
    public void testTheShadowStrengthsRenderTheDocumentedShadows()
    {
        Map<String, CssSheet.Declaration> none = render(ThemeTokens.none()
                                                                   .withShadowStrength(ShadowStrength.NONE));
        for (String property : new String[] {"--bs-box-shadow", "--bs-box-shadow-sm", "--bs-box-shadow-lg"})
        {
            assertEquals("none", none.get(LIGHT + property)
                                     .getValue(),
                         property);
        }
        assertEquals("none", none.get("|.card|--bs-card-box-shadow")
                                 .getValue());

        Map<String, CssSheet.Declaration> strong = render(ThemeTokens.none()
                                                                     .withShadowStrength(ShadowStrength.STRONG));
        assertEquals("0 1px 2px rgba(16, 24, 40, 0.12)", strong.get(LIGHT + "--bs-box-shadow-sm")
                                                               .getValue());
        assertEquals("0 4px 12px rgba(16, 24, 40, 0.16)", strong.get(LIGHT + "--bs-box-shadow")
                                                                .getValue());
        assertEquals("0 12px 32px rgba(16, 24, 40, 0.28)", strong.get(LIGHT + "--bs-box-shadow-lg")
                                                                 .getValue());
        assertEquals("0 1px 2px rgba(16, 24, 40, 0.12)", strong.get("|.card|--bs-card-box-shadow")
                                                               .getValue());

        Map<String, CssSheet.Declaration> subtle = render(ThemeTokens.none()
                                                                     .withShadowStrength(ShadowStrength.SUBTLE));
        assertEquals("0 1px 2px rgba(16, 24, 40, 0.03)", subtle.get(LIGHT + "--bs-box-shadow-sm")
                                                               .getValue());
    }

    @Test
    public void testTheBlockIsOrderedRootThenDarkThenComponentRulesAndTheSwitchKnobKeepsBootstrapsFocusBeforeCheckedOrder()
    {
        String css = ThemeTokenCss.render(ThemeTokens.none()
                                                     .withColor(ThemeColorRole.PRIMARY, RgbColor.parseHex("#0f766e")));

        int root = css.indexOf(":root,[data-bs-theme=light]{");
        int dark = css.indexOf("[data-bs-theme=dark]{");
        int firstComponent = css.indexOf(".btn-primary{");
        assertTrue(root == 0 && root < dark && dark < firstComponent, "root " + root + ", dark " + dark + ", first component rule " + firstComponent);

        int switchFocus = css.indexOf(".form-switch .form-check-input:focus{");
        int switchChecked = css.indexOf(".form-switch .form-check-input:checked{");
        assertTrue(switchFocus > 0 && switchFocus < switchChecked, "a focused checked switch must keep the white knob: the checked rule must follow the focus rule");
        assertTrue(css.indexOf(".form-check-input:focus{") < css.indexOf(".form-check-input:checked{"), "Bootstrap's focus rule precedes its checked rule");
    }

    @Test
    public void testNoTokenSetEverRendersAnAngleBracketOrAnUnbalancedBlock()
    {
        ThemeTokens everything = ThemeTokens.none()
                                            .withFontFamily(FontFamily.parse("Inter, \"Segoe UI\", sans-serif"))
                                            .withBaseFontSize(CssLength.parse("1rem"))
                                            .withBorderRadius(CssLength.parse("12px"))
                                            .withShadowStrength(ShadowStrength.STRONG);
        for (ThemeColorRole role : ThemeColorRole.values())
        {
            everything = everything.withColor(role, RgbColor.parseHex("#336699"));
        }
        String css = ThemeTokenCss.render(everything);

        assertFalse(css.contains("<"), "angle bracket in the token css");
        assertFalse(css.contains(">") && css.contains("</"), "closing tag in the token css");
        assertEquals(css.chars()
                        .filter(c -> c == '{')
                        .count(),
                     css.chars()
                        .filter(c -> c == '}')
                        .count(),
                     "braces must balance");
        assertTrue(css.length() > 5000, "the full token block is expected to be sizeable, got " + css.length());
    }

    /**
     * The property test of AC4.2: whatever strings are fed to the validators, everything they accept renders without an angle bracket, and the
     * rendered CSS parses back into rules whose every declaration lives in a rule the renderer itself wrote (nothing smuggled in as extra text).
     */
    @Test
    public void testNoInputThatTheValidatorsAcceptCanPutAnAngleBracketIntoTheRenderedBlock()
    {
        Random random = new Random(274);
        String hostile = "<>/{};\"'()#%\\ abcXYZ019-,.:*!\n\t&=@";
        String valid = "abcdefABCDEF0123456789 -,\"'#.pxrem";
        int accepted = 0;
        for (int i = 0; i < 20000; i++)
        {
            String candidate = randomString(random, i % 2 == 0 ? hostile : valid, random.nextInt(24));
            ThemeTokens tokens = ThemeTokens.none();
            boolean any = false;
            try
            {
                tokens = tokens.withColor(ThemeColorRole.values()[random.nextInt(6)], RgbColor.parseHex(candidate));
                any = true;
            }
            catch (IllegalArgumentException expected)
            {
                // rejected: nothing to render
            }
            try
            {
                tokens = tokens.withFontFamily(FontFamily.parse(candidate));
                any = true;
            }
            catch (IllegalArgumentException expected)
            {
                // rejected
            }
            try
            {
                tokens = tokens.withBorderRadius(CssLength.parse(candidate));
                any = true;
            }
            catch (IllegalArgumentException expected)
            {
                // rejected
            }
            if (any)
            {
                accepted++;
                String css = ThemeTokenCss.render(tokens);
                assertFalse(css.contains("<"), "angle bracket for accepted input '" + candidate + "'");
                String block = ThemeHeadRenderer.styleBlock(css);
                String inner = block.substring("<style>".length(), block.length() - "</style>".length());
                assertFalse(inner.contains("<"), "angle bracket inside the style element for accepted input '" + candidate + "'");
            }
        }
        // also drive the accepted path with generated VALID values, so the test cannot pass because everything was rejected
        for (int i = 0; i < 2000; i++)
        {
            ThemeTokens tokens = ThemeTokens.none()
                                            .withColor(ThemeColorRole.values()[random.nextInt(6)], RgbColor.of(random.nextInt(256), random.nextInt(256), random.nextInt(256)))
                                            .withFontFamily(FontFamily.parse(List.of("Inter", "\"Segoe UI\"", "'Helvetica Neue'", "-apple-system", "sans-serif")
                                                                                 .get(random.nextInt(5))))
                                            .withBorderRadius(CssLength.parse(random.nextInt(40) + "." + random.nextInt(10) + (random.nextBoolean() ? "px" : "rem")));
            assertFalse(ThemeTokenCss.render(tokens)
                                     .contains("<"));
            accepted++;
        }
        assertTrue(accepted > 2000, "the property test must exercise accepted inputs, got " + accepted);
    }

    private static String randomString(Random random, String alphabet, int length)
    {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < length; i++)
        {
            builder.append(alphabet.charAt(random.nextInt(alphabet.length())));
        }
        return builder.toString();
    }

    private static Map<String, CssSheet.Declaration> render(ThemeTokens tokens)
    {
        return ThemeTokenCssSheetGoldenTest.lastWins(CssSheet.parse(ThemeTokenCss.render(tokens)));
    }
}
