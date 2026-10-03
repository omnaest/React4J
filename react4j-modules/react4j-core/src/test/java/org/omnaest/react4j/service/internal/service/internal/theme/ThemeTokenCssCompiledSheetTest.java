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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.omnaest.react4j.domain.configuration.ThemeConfiguration.ShadowStrength;
import org.omnaest.react4j.service.internal.service.CssLength;
import org.omnaest.react4j.service.internal.service.FontFamily;
import org.omnaest.react4j.service.internal.service.RgbColor;
import org.omnaest.react4j.service.internal.service.ThemeColorRole;
import org.omnaest.react4j.service.internal.service.ThemeTokens;

/**
 * plan-274 S4 AC4.4: the two guards that tie the Java mirror to the stylesheet that is ACTUALLY SHIPPED ({@code /public/css/theme/react4j-modern.css}
 * of the react4j-core-ui artifact on the classpath), not to a copy.
 * <ol>
 * <li><b>Equality (drift guard).</b> Rendering the theme's own default values must reproduce, declaration for declaration, what the shipped sheet
 * compiled. The Java side mirrors settings of {@code react4j-modern.scss} (focus ring, body background, shadow shapes, radius ratios, button amounts);
 * if the scss changes and the Java constants do not, this fails.</li>
 * <li><b>Coverage (pre-mortem 11, guard rung ii).</b> Every rule of the shipped sheet that carries the default accent, or any value the deriver
 * derives from it, in hex, rgb or triplet form, must be a rule the renderer emits for that colour, or be listed in an explicit allowlist with a reason.
 * A new Bootstrap rule that paints with the accent therefore cannot silently escape the token override.</li>
 * </ol>
 */
public class ThemeTokenCssCompiledSheetTest
{
    private static final String                                   LIGHT_ROOT   = "|:root,[data-bs-theme=light]|";
    private static final Pattern                                  TRIPLET      = Pattern.compile("(?<![\\d.])(\\d{1,3}), (\\d{1,3}), (\\d{1,3})(?![\\d.])");
    private static final double                                   MATCH        = 1e-4;

    private final List<CssSheet.Declaration>                      compiledList = GoldenFixtures.compiledTheme();
    private final Map<String, CssSheet.Declaration>               compiled     = ThemeTokenCssSheetGoldenTest.lastWins(this.compiledList);

    /**
     * The explicit allowlist of the coverage guard: a rule of the shipped sheet that matches a colour derived from the default accent but is NOT
     * re-emitted by the token block, with the reason. Keys are {@code <at-rules><selector>}. An entry that is not matched by the guard, or that the
     * renderer does cover, fails the test, so the list cannot rot.
     */
    private static final Map<ThemeColorRole, Map<String, String>> ALLOWLIST    = new LinkedHashMap<>();
    static
    {
        for (ThemeColorRole role : ThemeColorRole.values())
        {
            ALLOWLIST.put(role, new LinkedHashMap<>());
        }
        // The default secondary colour IS Bootstrap's gray-600 (#6c757d). These rules paint with the neutral gray palette ($gray-600), not with
        // $secondary, so overriding the secondary colour must not touch them: they match by coincidence of the palette.
        String grayReason = "gray-600 (#6c757d) is the same literal as the default secondary by coincidence of Bootstrap's palette; this rule uses $gray-600, not $secondary";
        ALLOWLIST.get(ThemeColorRole.SECONDARY)
                 .put(".blockquote-footer", grayReason + " ($blockquote-footer-color)");
        ALLOWLIST.get(ThemeColorRole.SECONDARY)
                 .put(".btn-link", grayReason + " ($btn-link-disabled-color)");
        ALLOWLIST.get(ThemeColorRole.SECONDARY)
                 .put(".dropdown-menu", grayReason + " ($dropdown-header-color)");
        ALLOWLIST.get(ThemeColorRole.SECONDARY)
                 .put(".form-floating>:disabled~label,.form-floating>.form-control:disabled~label", grayReason + " ($form-floating-label-disabled-color)");
    }

    // ---- equality ----------------------------------------------------------------------------------------------------

    @Test
    public void testTheDerivedButtonVariablesForTheCompiledDefaultAccentEqualTheBtnPrimaryAndBtnOutlinePrimaryRulesOfTheShippedSheet()
    {
        Map<String, CssSheet.Declaration> emitted = emit(this.tokensForCompiledDefaults(ThemeColorRole.PRIMARY));

        int compared = 0;
        for (String selector : new String[] {".btn-primary", ".btn-outline-primary"})
        {
            List<CssSheet.Declaration> rule = new ArrayList<>();
            for (CssSheet.Declaration declaration : emitted.values())
            {
                if (declaration.getSelector()
                               .equals(selector))
                {
                    rule.add(declaration);
                }
            }
            assertTrue(rule.size() >= 13, selector + " must carry every --bs-btn-* variable of Bootstrap's mixin, got " + rule.size());
            for (CssSheet.Declaration declaration : rule)
            {
                CssSheet.Declaration shipped = this.compiled.get(declaration.key());
                assertNotNull(shipped, "the shipped sheet has no " + declaration.key());
                assertEquals(null, CssValues.describeDifference(shipped.getValue(), declaration.getValue()), declaration.key());
                compared++;
            }
        }
        assertTrue(compared >= 27, "compared declarations: " + compared);
    }

    @TestFactory
    public List<DynamicTest> testEverythingRenderedForTheCompiledDefaultsOfEachColourEqualsTheShippedSheet()
    {
        List<DynamicTest> tests = new ArrayList<>();
        for (ThemeColorRole role : ThemeColorRole.values())
        {
            tests.add(DynamicTest.dynamicTest("default " + role.getCssName(), () -> this.assertEqualsShippedSheet(this.tokensForCompiledDefaults(role))));
        }
        return tests;
    }

    @Test
    public void testTheCompiledDefaultsOfRadiusFontShadowAndBaseFontSizeEqualTheShippedSheet()
    {
        ThemeTokens tokens = ThemeTokens.none()
                                        .withBorderRadius(CssLength.parse(this.compiledRootValue("--bs-border-radius")))
                                        .withFontFamily(FontFamily.parse(this.compiledRootValue("--bs-body-font-family")))
                                        .withBaseFontSize(CssLength.parse(this.compiledRootValue("--bs-body-font-size")))
                                        .withShadowStrength(ShadowStrength.DEFAULT);
        this.assertEqualsShippedSheet(tokens);
        Map<String, CssSheet.Declaration> emitted = emit(tokens);
        assertTrue(emitted.containsKey(LIGHT_ROOT + "--bs-box-shadow-lg"), "the shadow scale must be emitted");
        assertTrue(emitted.containsKey("|.card|--bs-card-box-shadow"), "the card shadow literal must be emitted");
        assertTrue(emitted.containsKey("|kbd|border-radius") && emitted.containsKey("|.btn-close|border-radius"), "the two literal radii must be emitted");
    }

    @Test
    public void testTheDarkBlockRestatesEveryVariableOfTheLightBlockThatTheShippedDarkBlockOverrides()
    {
        // the light block also matches the root element in dark mode and comes after the shipped dark block, so a variable it sets would defeat the
        // shipped dark value unless the emitted dark block restates it
        ThemeTokens all = ThemeTokens.none();
        for (ThemeColorRole role : ThemeColorRole.values())
        {
            all = all.withColor(role, RgbColor.parseHex("#336699"));
        }
        Map<String, CssSheet.Declaration> emitted = emit(all);
        int checked = 0;
        for (CssSheet.Declaration declaration : emitted.values())
        {
            if (!declaration.getSelector()
                            .equals(":root,[data-bs-theme=light]"))
            {
                continue;
            }
            boolean shippedDarkOverrides = this.compiled.containsKey("|[data-bs-theme=dark]|" + declaration.getProperty());
            if (shippedDarkOverrides)
            {
                checked++;
                assertTrue(emitted.containsKey("|[data-bs-theme=dark]|" + declaration.getProperty()),
                           declaration.getProperty() + " is set in the light block but not restated in the dark block, which the shipped sheet overrides in dark mode");
            }
        }
        assertTrue(checked >= 20, "variables checked: " + checked);
    }

    // ---- coverage ----------------------------------------------------------------------------------------------------

    @TestFactory
    public List<DynamicTest> testEveryShippedRuleCarryingTheDefaultColourOrAValueDerivedFromItIsCoveredByTheRendererOrAllowlisted()
    {
        List<DynamicTest> tests = new ArrayList<>();
        for (ThemeColorRole role : ThemeColorRole.values())
        {
            tests.add(DynamicTest.dynamicTest("coverage " + role.getCssName(), () -> this.assertCoverage(role)));
        }
        return tests;
    }

    private void assertCoverage(ThemeColorRole role)
    {
        Map<String, CssSheet.Declaration> emitted = emit(this.tokensForCompiledDefaults(role));

        Set<String> emittedRules = new TreeSet<>();
        List<double[]> derived = new ArrayList<>();
        for (CssSheet.Declaration declaration : emitted.values())
        {
            emittedRules.add(ruleKey(declaration));
            for (double[] color : colorsIn(declaration.getValue()))
            {
                if (!isNeutral(color) && !contains(derived, color))
                {
                    derived.add(color);
                }
            }
        }
        assertTrue(derived.size() >= 10, "derived values for " + role + ": " + derived.size());

        Set<String> carrying = new LinkedHashSet<>();
        for (CssSheet.Declaration declaration : this.compiledList)
        {
            for (double[] color : colorsIn(declaration.getValue()))
            {
                if (contains(derived, color))
                {
                    carrying.add(ruleKey(declaration));
                }
            }
        }

        Map<String, String> allowlist = ALLOWLIST.get(role);
        Set<String> uncovered = new TreeSet<>();
        for (String rule : carrying)
        {
            if (!emittedRules.contains(rule) && !allowlist.containsKey(rule))
            {
                uncovered.add(rule);
            }
        }
        Set<String> staleAllowlist = new TreeSet<>();
        for (String rule : allowlist.keySet())
        {
            if (!carrying.contains(rule) || emittedRules.contains(rule))
            {
                staleAllowlist.add(rule);
            }
        }
        assertTrue(uncovered.isEmpty(),
                   "rules of the shipped sheet carry a " + role.getCssName() + "-derived value but are neither emitted nor allowlisted: " + uncovered);
        assertTrue(staleAllowlist.isEmpty(), "allowlist entries that are not matched by the guard or are covered by the renderer: " + staleAllowlist);
        assertTrue(carrying.size() >= (role == ThemeColorRole.PRIMARY ? 25 : 5), role + " must carry at least some rules, got " + carrying.size());
    }

    // ---- helpers -----------------------------------------------------------------------------------------------------

    private void assertEqualsShippedSheet(ThemeTokens tokens)
    {
        Map<String, CssSheet.Declaration> emitted = emit(tokens);
        List<String> problems = new ArrayList<>();
        for (CssSheet.Declaration declaration : emitted.values())
        {
            CssSheet.Declaration shipped = this.compiled.get(declaration.key());
            if (shipped == null)
            {
                problems.add("not in the shipped sheet: " + declaration.key());
                continue;
            }
            String difference = CssValues.describeDifference(shipped.getValue(), declaration.getValue());
            if (difference != null)
            {
                problems.add(declaration.key() + ": " + difference);
            }
            if (shipped.isImportant() != declaration.isImportant())
            {
                problems.add(declaration.key() + ": important flag differs");
            }
        }
        assertTrue(problems.isEmpty(), () -> problems.size() + " drifts between the Java mirror and react4j-modern.css, first: " + problems.subList(0, Math.min(4, problems.size())));
        assertTrue(emitted.size() >= 10, "suspiciously little emitted: " + emitted.size());
    }

    private ThemeTokens tokensForCompiledDefaults(ThemeColorRole role)
    {
        return ThemeTokens.none()
                          .withColor(role, RgbColor.parseHex(this.compiledRootValue("--bs-" + role.getCssName())));
    }

    private String compiledRootValue(String property)
    {
        CssSheet.Declaration declaration = this.compiled.get(LIGHT_ROOT + property);
        assertNotNull(declaration, "the shipped sheet has no " + property + " in its light root block");
        return declaration.getValue();
    }

    private static Map<String, CssSheet.Declaration> emit(ThemeTokens tokens)
    {
        return ThemeTokenCssSheetGoldenTest.lastWins(CssSheet.parse(ThemeTokenCss.render(tokens)));
    }

    private static String ruleKey(CssSheet.Declaration declaration)
    {
        return declaration.getAtRules() + declaration.getSelector();
    }

    private static boolean isNeutral(double[] color)
    {
        return color[0] == color[1] && color[1] == color[2] && (color[0] == 0 || color[0] == 255);
    }

    private static boolean contains(List<double[]> colors, double[] candidate)
    {
        for (double[] color : colors)
        {
            if (Math.abs(color[0] - candidate[0]) < MATCH && Math.abs(color[1] - candidate[1]) < MATCH && Math.abs(color[2] - candidate[2]) < MATCH)
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Every colour a value carries, as three channels: hex, rgb()/rgba() with numbers or percentages (also inside a data URI), and the bare integer
     * triplets of Bootstrap's {@code -rgb} variables and {@code RGBA(r, g, b, ...)} helpers
     */
    private static List<double[]> colorsIn(String value)
    {
        List<double[]> colors = new ArrayList<>(CssValues.parse(value).colors);
        Matcher matcher = TRIPLET.matcher(value);
        while (matcher.find())
        {
            colors.add(new double[] {Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)), Integer.parseInt(matcher.group(3))});
        }
        return colors;
    }
}
