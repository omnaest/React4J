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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.omnaest.react4j.domain.configuration.ThemeConfiguration.ShadowStrength;
import org.omnaest.react4j.service.internal.service.CssLength;
import org.omnaest.react4j.service.internal.service.FontFamily;
import org.omnaest.react4j.service.internal.service.RgbColor;
import org.omnaest.react4j.service.internal.service.ThemeColorRole;
import org.omnaest.react4j.service.internal.service.ThemeTokens;

/**
 * plan-277 T3, AC1: {@link TablerTokenCss} is an algorithmic leaf (a {@link ThemeTokens} value in, CSS text out, no collaborators), so it is tested
 * at its own boundary without mocks: every token family alone, all together, px and rem radius and all four shadow strengths. What the written names
 * mean to the SHIPPED Tabler sheet is {@link TablerTokenCssSheetGuardTest}'s subject, not this class's.
 */
public class TablerTokenCssTest
{
    private static final String SELECTOR     = ":root,[data-bs-theme=light],[data-theme=light]";
    private static final String DEFAULT_RING = "0px 0px 0px 1px color-mix(in oklab, light-dark(rgba(18, 18, 23, 0.4), #000) 25%, transparent)";

    private static final String TEAL         = "#0f766e";
    private static final String FONT_FAMILY  = "Inter, \"Segoe UI\", system-ui, sans-serif";

    private static ThemeTokens withColor(ThemeColorRole role, String hex)
    {
        return ThemeTokens.none()
                          .withColor(role, RgbColor.parseHex(hex));
    }

    private static String rule(String declarations)
    {
        return SELECTOR + "{" + declarations + "}";
    }

    private static String radiusDeclarations(String length)
    {
        return "--bs-border-radius-md:" + length + ";--bs-border-radius-sm:calc(" + length + " * 2 / 3);--bs-border-radius-lg:calc(" + length
               + " * 4 / 3);--bs-border-radius-xl:calc(" + length + " * 8 / 3);--bs-border-radius-xxl:calc(" + length + " * 16 / 3)";
    }

    // ---- nothing set -------------------------------------------------------------------------------------------------

    @Test
    public void testNoTokensGiveAnEmptyString()
    {
        assertEquals("", TablerTokenCss.render(ThemeTokens.none()));
    }

    // ---- colours ----------------------------------------------------------------------------------------------------

    @Test
    public void testAColourWritesTheHexAndTheIntegerTripletAndNothingElse()
    {
        assertEquals(rule("--bs-primary:#0f766e;--bs-primary-rgb:15, 118, 110"), TablerTokenCss.render(withColor(ThemeColorRole.PRIMARY, TEAL)));
    }

    @Test
    public void testAShortHexColourIsWrittenTheWayTheSassOutputWritesIt()
    {
        assertEquals(rule("--bs-danger:#f00;--bs-danger-rgb:255, 0, 0"), TablerTokenCss.render(withColor(ThemeColorRole.DANGER, "#ff0000")));
    }

    @ParameterizedTest
    @EnumSource(ThemeColorRole.class)
    public void testEveryRoleWritesExactlyItsOwnTwoVariables(ThemeColorRole role)
    {
        String name = "--bs-" + role.getCssName();

        assertEquals(rule(name + ":#0f766e;" + name + "-rgb:15, 118, 110"), TablerTokenCss.render(withColor(role, TEAL)));
    }

    @Test
    public void testSeveralRolesAreWrittenInTheOrderOfTheRoleEnumIndependentOfTheOrderTheyWereSet()
    {
        ThemeTokens tokens = ThemeTokens.none()
                                        .withColor(ThemeColorRole.DANGER, RgbColor.parseHex("#d63939"))
                                        .withColor(ThemeColorRole.PRIMARY, RgbColor.parseHex("#0f766e"));

        assertEquals(rule("--bs-primary:#0f766e;--bs-primary-rgb:15, 118, 110;--bs-danger:#d63939;--bs-danger-rgb:214, 57, 57"), TablerTokenCss.render(tokens));
    }

    @Test
    public void testNeitherTheTextColourNorAnyShadeIsDerived()
    {
        String css = TablerTokenCss.render(withColor(ThemeColorRole.PRIMARY, TEAL));

        for (String derived : new String[] {"-fg", "-lt", "-darken", "-200", "-text-emphasis", "-bg-subtle", "-border-subtle", ".btn"})
        {
            assertFalse(css.contains(derived), derived + " must not be written: " + css);
        }
    }

    // ---- font -------------------------------------------------------------------------------------------------------

    @Test
    public void testTheFontFamilySetsTheBaseVariableTheBodyFamilyFollows()
    {
        ThemeTokens tokens = ThemeTokens.none()
                                        .withFontFamily(FontFamily.parse("Inter,   \"Segoe UI\" ,system-ui, sans-serif"));

        assertEquals(rule("--bs-font-sans-serif:" + FONT_FAMILY), TablerTokenCss.render(tokens));
    }

    @Test
    public void testTheBaseFontSizeInPxAndInRem()
    {
        assertEquals(rule("--bs-body-font-size:16px"), TablerTokenCss.render(ThemeTokens.none()
                                                                                        .withBaseFontSize(CssLength.parse("16px"))));
        assertEquals(rule("--bs-body-font-size:1.125rem"), TablerTokenCss.render(ThemeTokens.none()
                                                                                            .withBaseFontSize(CssLength.parse("1.125rem"))));
    }

    // ---- radius -----------------------------------------------------------------------------------------------------

    @Test
    public void testThePixelRadiusSetsTheBaseSizeAndScalesTheOtherSizesWithTablersRatios()
    {
        assertEquals(rule(radiusDeclarations("12px")), TablerTokenCss.render(ThemeTokens.none()
                                                                                        .withBorderRadius(CssLength.parse("12px"))));
    }

    @Test
    public void testTheRemRadiusIsScaledInTheSameUnitWithoutAPixelConversion()
    {
        assertEquals(rule(radiusDeclarations("0.5rem")), TablerTokenCss.render(ThemeTokens.none()
                                                                                          .withBorderRadius(CssLength.parse("0.5rem"))));
    }

    @Test
    public void testAZeroRadiusKeepsItsUnitSoCalcStaysValid()
    {
        assertEquals(rule(radiusDeclarations("0px")), TablerTokenCss.render(ThemeTokens.none()
                                                                                       .withBorderRadius(CssLength.parse("0px"))));
    }

    @Test
    public void testTheBaseRadiusVariableOfTheBootstrapNameIsLeftToFollowTheBaseSizeAndTheUnreadExtraSmallSizeIsNotWritten()
    {
        String css = TablerTokenCss.render(ThemeTokens.none()
                                                      .withBorderRadius(CssLength.parse("12px")));

        assertFalse(css.contains("--bs-border-radius:"), "derived by the sheet from the base size: " + css);
        assertFalse(css.contains("--bs-border-radius-xs"), "not read by the sheet: " + css);
        assertFalse(css.contains("--bs-border-radius-scale"), "the scale is superseded, not set: " + css);
    }

    // ---- shadow -----------------------------------------------------------------------------------------------------

    @Test
    public void testTheDefaultStrengthAloneIsTablerAsCompiledSoNothingIsWritten()
    {
        assertEquals("", TablerTokenCss.render(ThemeTokens.none()
                                                          .withShadowStrength(ShadowStrength.DEFAULT)));
    }

    @Test
    public void testSubtleHalvesTheAlphaInBothArmsOfTheShadowColour()
    {
        assertEquals(rule("--bs-shadow-color:light-dark(rgba(18, 18, 23, 0.2), rgba(0, 0, 0, 0.5));--bs-shadow-border:" + DEFAULT_RING),
                     TablerTokenCss.render(ThemeTokens.none()
                                                      .withShadowStrength(ShadowStrength.SUBTLE)));
    }

    @Test
    public void testStrongDoublesTheLightAlphaAndCapsTheOpaqueDarkArmAtOne()
    {
        assertEquals(rule("--bs-shadow-color:light-dark(rgba(18, 18, 23, 0.8), rgba(0, 0, 0, 1));--bs-shadow-border:" + DEFAULT_RING),
                     TablerTokenCss.render(ThemeTokens.none()
                                                      .withShadowStrength(ShadowStrength.STRONG)));
    }

    @Test
    public void testNoneMakesTheShadowColourFullyTransparentInBothArms()
    {
        assertEquals(rule("--bs-shadow-color:light-dark(rgba(18, 18, 23, 0), rgba(0, 0, 0, 0));--bs-shadow-border:" + DEFAULT_RING),
                     TablerTokenCss.render(ThemeTokens.none()
                                                      .withShadowStrength(ShadowStrength.NONE)));
    }

    /**
     * The border of mentions and avatars is drawn with box-shadow, so every strength that changes the shadow colour pins that ring to the default
     * colour: a strength must never remove or weaken a border.
     */
    @ParameterizedTest
    @EnumSource(value = ShadowStrength.class, names = "DEFAULT", mode = EnumSource.Mode.EXCLUDE)
    public void testNoStrengthChangesTheBorderRing(ShadowStrength strength)
    {
        String css = TablerTokenCss.render(ThemeTokens.none()
                                                      .withShadowStrength(strength));

        assertTrue(css.contains("--bs-shadow-border:" + DEFAULT_RING), css);
    }

    // ---- all together and the shape of the output --------------------------------------------------------------------

    @Test
    public void testAllTokensTogetherAreOneRuleInTheOrderColoursFontSizeRadiusShadow()
    {
        ThemeTokens tokens = ThemeTokens.none()
                                        .withColor(ThemeColorRole.PRIMARY, RgbColor.parseHex(TEAL))
                                        .withColor(ThemeColorRole.SECONDARY, RgbColor.parseHex("#475569"))
                                        .withFontFamily(FontFamily.parse(FONT_FAMILY))
                                        .withBaseFontSize(CssLength.parse("15px"))
                                        .withBorderRadius(CssLength.parse("12px"))
                                        .withShadowStrength(ShadowStrength.SUBTLE);

        String expected = rule("--bs-primary:#0f766e;--bs-primary-rgb:15, 118, 110;" + "--bs-secondary:#475569;--bs-secondary-rgb:71, 85, 105;"
                               + "--bs-font-sans-serif:" + FONT_FAMILY + ";" + "--bs-body-font-size:15px;" + radiusDeclarations("12px") + ";"
                               + "--bs-shadow-color:light-dark(rgba(18, 18, 23, 0.2), rgba(0, 0, 0, 0.5));--bs-shadow-border:" + DEFAULT_RING);
        assertEquals(expected, TablerTokenCss.render(tokens));
    }

    @Test
    public void testOutputOfEveryTokenIsBaseCustomPropertiesInOneRuleAndNeverAComponentRuleOrAnAngleBracket()
    {
        ThemeTokens tokens = ThemeTokens.none();
        for (ThemeColorRole role : ThemeColorRole.values())
        {
            tokens = tokens.withColor(role, RgbColor.parseHex(TEAL));
        }
        for (ShadowStrength strength : ShadowStrength.values())
        {
            String css = TablerTokenCss.render(tokens.withFontFamily(FontFamily.parse(FONT_FAMILY))
                                                     .withBaseFontSize(CssLength.parse("1rem"))
                                                     .withBorderRadius(CssLength.parse("0.5rem"))
                                                     .withShadowStrength(strength));

            assertTrue(css.startsWith(SELECTOR + "{") && css.endsWith("}"), css);
            assertEquals(1, css.chars()
                               .filter(character -> character == '{')
                               .count(),
                         "exactly one rule: " + css);
            assertFalse(css.contains("<"), css);
            assertFalse(css.contains(".btn"), css);
            assertFalse(css.contains("!important"), css);
            List<String> properties = Arrays.stream(css.substring(SELECTOR.length() + 1, css.length() - 1)
                                                       .split(";"))
                                            .map(declaration -> declaration.substring(0, declaration.indexOf(':')))
                                            .collect(Collectors.toList());
            assertFalse(properties.isEmpty(), css);
            properties.forEach(property -> assertTrue(property.startsWith("--bs-"), property + " in " + css));
        }
    }
}
