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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.omnaest.react4j.domain.configuration.ThemeConfiguration.ShadowStrength;
import org.omnaest.react4j.service.internal.service.CssLength;
import org.omnaest.react4j.service.internal.service.FontFamily;
import org.omnaest.react4j.service.internal.service.RgbColor;
import org.omnaest.react4j.service.internal.service.ThemeColorRole;
import org.omnaest.react4j.service.internal.service.ThemeTokens;
import org.omnaest.react4j.service.internal.service.internal.theme.TablerSheet.Declaration;
import org.omnaest.react4j.service.internal.service.internal.theme.TablerTokenCss.RadiusStep;

/**
 * plan-277 T3, AC3 and AC4 (seam s6, decision D7): the two guards that tie {@link TablerTokenCss} to the Tabler stylesheet that is ACTUALLY SHIPPED
 * ({@code /public/css/theme/react4j-tabler.css} of the react4j-core-ui artifact, so core-ui must be installed first), so that a Tabler update cannot
 * silently detach a design token.
 * <ol>
 * <li><b>Detachment guard.</b> Every custom property the writer can emit must be READ in the sheet, i.e. appear as {@code var(--name)} or
 * {@code var(--name, ...)}. A name that is only declared is not enough: Tabler declares {@code --bs-<role>-lt-rgb} for every role and reads none of
 * them, so writing one would change nothing and no other test would notice. The names are collected by rendering every token combination, and are
 * pinned to an explicit list as well, so an emptied or shrunk writer cannot make the guard pass vacuously.</li>
 * <li><b>Selector guard.</b> Every declaration of an emitted name in the sheet must sit in a selector the writer's own rule ties in specificity
 * (the sheet's {@code :root}, {@code :root,:host} and the light scope list), because only then does the later rule win. A Tabler update that declares
 * one of them in a more specific selector, or in the dark block, would detach it just as silently.</li>
 * <li><b>Constant drift guard.</b> The Tabler defaults the writer mirrors - the radius ratios against the base size, the default shadow colour with
 * its alpha and the shape of {@code --bs-shadow-border} - are compared with the sheet.</li>
 * </ol>
 * Each guard carries a negative control run against a doctored sheet or a name known to be only declared, so none can be satisfied by accident.
 */
public class TablerTokenCssSheetGuardTest
{
    private static final Pattern     PROPERTY_NAME          = Pattern.compile("^(--[A-Za-z0-9-]+):");

    /**
     * The members of a rule selector the sheet declares the emitted names in. Each has the specificity of one pseudo-class or attribute, exactly like
     * every member of {@link TablerTokenCss#SELECTOR}.
     */
    private static final Set<String> TYING_SELECTOR_MEMBERS = Set.of(":root", ":host", "[data-bs-theme=light]", "[data-theme=light]");

    private static final BigDecimal  ROOT_FONT_SIZE_PX      = new BigDecimal("16");

    private static final TablerSheet SHIPPED                = TablerSheet.shipped();

    /**
     * Every custom property the writer emits for any token, written out once so that the collected set below is compared with a statement of intent
     */
    private static final Set<String> EXPECTED_EMITTED_NAMES = emittedNameList();

    private static Set<String> emittedNameList()
    {
        Set<String> names = new TreeSet<>();
        for (ThemeColorRole role : ThemeColorRole.values())
        {
            names.add("--bs-" + role.getCssName());
            names.add("--bs-" + role.getCssName() + "-rgb");
        }
        names.addAll(Arrays.asList("--bs-font-sans-serif", "--bs-body-font-size", "--bs-border-radius-md", "--bs-border-radius-sm", "--bs-border-radius-lg",
                                   "--bs-border-radius-xl", "--bs-border-radius-xxl", "--bs-shadow-color", "--bs-shadow-border"));
        return names;
    }

    // ---- what the writer can emit -------------------------------------------------------------------------------------

    /**
     * @return every custom property name the writer emits over all token families, all six roles, px and rem lengths and all four shadow strengths
     */
    private static Set<String> collectEmittableNames()
    {
        ThemeTokens allRoles = ThemeTokens.none();
        for (ThemeColorRole role : ThemeColorRole.values())
        {
            allRoles = allRoles.withColor(role, RgbColor.parseHex("#0f766e"));
        }
        List<ThemeTokens> renderings = new ArrayList<>();
        for (ShadowStrength strength : ShadowStrength.values())
        {
            renderings.add(allRoles.withFontFamily(FontFamily.parse("Inter, sans-serif"))
                                   .withBaseFontSize(CssLength.parse("16px"))
                                   .withBorderRadius(CssLength.parse("12px"))
                                   .withShadowStrength(strength));
            renderings.add(allRoles.withBaseFontSize(CssLength.parse("1rem"))
                                   .withBorderRadius(CssLength.parse("0.5rem"))
                                   .withShadowStrength(strength));
        }

        Set<String> names = new TreeSet<>();
        for (ThemeTokens tokens : renderings)
        {
            String css = TablerTokenCss.render(tokens);
            String body = css.substring(css.indexOf('{') + 1, css.lastIndexOf('}'));
            for (String declaration : body.split(";"))
            {
                Matcher matcher = PROPERTY_NAME.matcher(declaration);
                assertTrue(matcher.find(), "not a custom property declaration: " + declaration);
                names.add(matcher.group(1));
            }
        }
        return names;
    }

    // ---- the checks, each a function of a sheet so that a doctored sheet can be fed to it --------------------------------

    private static List<String> detachedNames(TablerSheet sheet, Collection<String> names)
    {
        List<String> detached = new ArrayList<>();
        for (String name : names)
        {
            if (!sheet.isRead(name))
            {
                detached.add(name + (sheet.declarations(name)
                                          .isEmpty() ? " (not in the sheet at all)" : " (declared, but never read through var())"));
            }
        }
        return detached;
    }

    private static List<String> outOfTieSelectors(TablerSheet sheet, Collection<String> names)
    {
        List<String> problems = new ArrayList<>();
        for (String name : names)
        {
            List<Declaration> declarations = sheet.declarations(name);
            if (declarations.isEmpty())
            {
                problems.add(name + " is not declared at all");
            }
            for (Declaration declaration : declarations)
            {
                for (String member : declaration.getSelector()
                                                .split(","))
                {
                    if (!TYING_SELECTOR_MEMBERS.contains(member.trim()))
                    {
                        problems.add(name + " is declared in '" + declaration.getSelector() + "' (member '" + member.trim() + "')");
                    }
                }
            }
        }
        return problems;
    }

    private static List<String> radiusDrifts(TablerSheet sheet)
    {
        List<String> drifts = new ArrayList<>();
        BigDecimal base = radiusInPixels(sheet, TablerTokenCss.RADIUS_BASE_NAME);
        for (RadiusStep step : RadiusStep.values())
        {
            BigDecimal compiled = radiusInPixels(sheet, step.getName());
            // compiled / base == numerator / denominator, compared without division so it is exact
            boolean sameRatio = compiled.multiply(BigDecimal.valueOf(step.getDenominator()))
                                        .compareTo(base.multiply(BigDecimal.valueOf(step.getNumerator()))) == 0;
            if (!sameRatio)
            {
                drifts.add(step.getName() + " is " + compiled + "px in the sheet against a base of " + base + "px, but the writer assumes " + step.getNumerator() + "/"
                           + step.getDenominator() + " of the base");
            }
        }
        String baseFollowing = sheet.lastValue("--bs-border-radius");
        if (!("var(" + TablerTokenCss.RADIUS_BASE_NAME + ")").equals(baseFollowing))
        {
            drifts.add("--bs-border-radius is '" + baseFollowing + "', the writer relies on it following " + TablerTokenCss.RADIUS_BASE_NAME);
        }
        String bodyFamily = sheet.lastValue("--bs-body-font-family");
        if (!"var(--bs-font-sans-serif)".equals(bodyFamily))
        {
            drifts.add("--bs-body-font-family is '" + bodyFamily + "', the writer relies on it following --bs-font-sans-serif");
        }
        return drifts;
    }

    /**
     * @return the size of a radius variable in pixels, from its compiled form {@code calc(<n><px|rem> * var(--bs-border-radius-scale, 1))}
     */
    private static BigDecimal radiusInPixels(TablerSheet sheet, String name)
    {
        String value = sheet.lastValue(name);
        Matcher matcher = Pattern.compile("calc\\((\\d+(?:\\.\\d+)?)(px|rem) \\* var\\(--bs-border-radius-scale, 1\\)\\)")
                                 .matcher(value);
        assertTrue(matcher.matches(), name + " has a form this guard does not know: " + value);
        BigDecimal number = new BigDecimal(matcher.group(1));
        return "rem".equals(matcher.group(2)) ? number.multiply(ROOT_FONT_SIZE_PX) : number;
    }

    private static List<String> shadowDrifts(TablerSheet sheet)
    {
        List<String> drifts = new ArrayList<>();
        String color = sheet.lastValue("--bs-shadow-color");
        if (!TablerTokenCss.DEFAULT_SHADOW_COLOR.equals(color))
        {
            drifts.add("--bs-shadow-color is '" + color + "', the writer assumes '" + TablerTokenCss.DEFAULT_SHADOW_COLOR + "'");
        }
        Matcher matcher = Pattern.compile("light-dark\\(rgba\\((\\d+, \\d+, \\d+), ([0-9.]+)\\), (#[0-9a-fA-F]+)\\)")
                                 .matcher(color);
        if (!matcher.matches())
        {
            drifts.add("--bs-shadow-color has a form the scaling does not know: " + color);
        }
        else
        {
            if (!TablerTokenCss.SHADOW_LIGHT_CHANNELS.equals(matcher.group(1)))
            {
                drifts.add("light shadow channels are " + matcher.group(1) + ", the writer assumes " + TablerTokenCss.SHADOW_LIGHT_CHANNELS);
            }
            if (new BigDecimal(matcher.group(2)).compareTo(TablerTokenCss.SHADOW_LIGHT_ALPHA) != 0)
            {
                drifts.add("light shadow alpha is " + matcher.group(2) + ", the writer assumes " + TablerTokenCss.SHADOW_LIGHT_ALPHA);
            }
            // the dark arm is only a hex colour, so it is pinned as the opaque black the writer scales from
            if (!"#000".equalsIgnoreCase(matcher.group(3)) || !"0, 0, 0".equals(TablerTokenCss.SHADOW_DARK_CHANNELS) || BigDecimal.ONE.compareTo(TablerTokenCss.SHADOW_DARK_ALPHA) != 0)
            {
                drifts.add("dark shadow colour is " + matcher.group(3) + ", the writer assumes opaque black");
            }
        }
        String ring = sheet.lastValue("--bs-shadow-border");
        String expectedRing = String.format(TablerTokenCss.SHADOW_BORDER_TEMPLATE, TablerTokenCss.SHADOW_COLOR_VARIABLE);
        if (!expectedRing.equals(ring))
        {
            drifts.add("--bs-shadow-border is '" + ring + "', the writer assumes '" + expectedRing + "'");
        }
        return drifts;
    }

    // ---- AC3: the detachment guard ------------------------------------------------------------------------------------

    @Test
    public void testTheWriterEmitsExactlyTheExpectedSetOfNames()
    {
        assertEquals(EXPECTED_EMITTED_NAMES, collectEmittableNames(), "a name was added to or dropped from the writer: update the expectation deliberately");
    }

    @Test
    public void testEveryCustomPropertyTheWriterCanEmitIsReadInTheShippedSheet()
    {
        Set<String> emittable = collectEmittableNames();
        assertFalse(emittable.isEmpty(), "nothing emitted would make the guard vacuous");

        List<String> detached = detachedNames(SHIPPED, emittable);

        assertTrue(detached.isEmpty(), () -> "design tokens detached from Tabler, written but never read: " + detached);
    }

    /**
     * Negative control with a name no sheet knows
     */
    @Test
    public void testTheDetachmentGuardRejectsAnAbsentName()
    {
        List<String> detached = detachedNames(SHIPPED, List.of("--bs-primary", "--bs-bogus-name"));

        assertEquals(1, detached.size(), detached.toString());
        assertTrue(detached.get(0)
                           .startsWith("--bs-bogus-name (not in the sheet at all)"),
                   detached.toString());
    }

    /**
     * Negative control with the real case the guard exists for: Tabler declares {@code --bs-primary-lt-rgb} and never reads it (measured at T1, and
     * asserted here so that the control is still the case it claims to be)
     */
    @Test
    public void testTheDetachmentGuardRejectsAVariableThatIsOnlyDeclared()
    {
        String onlyDeclared = "--bs-primary-lt-rgb";
        assertFalse(SHIPPED.declarations(onlyDeclared)
                           .isEmpty(),
                    "the control no longer exists in the sheet, pick another declared-only variable");

        List<String> detached = detachedNames(SHIPPED, List.of(onlyDeclared));

        assertEquals(List.of(onlyDeclared + " (declared, but never read through var())"), detached);
    }

    // ---- the selector guard -----------------------------------------------------------------------------------------

    @Test
    public void testEveryDeclarationOfAnEmittedNameSitsInASelectorTheWritersRuleTies()
    {
        List<String> problems = outOfTieSelectors(SHIPPED, collectEmittableNames());

        assertTrue(problems.isEmpty(), () -> "a later rule of the writer would not win for: " + problems);
    }

    @Test
    public void testTheWritersOwnSelectorMembersAreExactlyTheTyingMembers()
    {
        Set<String> writersMembers = new TreeSet<>(Arrays.asList(TablerTokenCss.SELECTOR.split(",")));

        assertTrue(TYING_SELECTOR_MEMBERS.containsAll(writersMembers), writersMembers.toString());
    }

    /**
     * Negative control: a doctored sheet that declares a primary colour in the dark block, which would beat the writer in dark mode
     */
    @Test
    public void testTheSelectorGuardRejectsADeclarationInTheDarkBlock()
    {
        TablerSheet doctored = TablerSheet.of(SHIPPED.getCss() + "[data-bs-theme=dark]{--bs-primary:#000}");

        List<String> problems = outOfTieSelectors(doctored, List.of("--bs-primary"));

        assertEquals(1, problems.size(), problems.toString());
        assertTrue(problems.get(0)
                           .contains("[data-bs-theme=dark]"),
                   problems.toString());
    }

    // ---- AC4: the constant drift guard ----------------------------------------------------------------------------------

    @Test
    public void testTheRadiusRatiosTheWriterMirrorsEqualTheShippedSheet()
    {
        List<String> drifts = radiusDrifts(SHIPPED);

        assertTrue(drifts.isEmpty(), () -> "Tabler's radius scale changed, update RadiusStep: " + drifts);
    }

    @Test
    public void testTheDefaultShadowColourAndRingTheWriterMirrorsEqualTheShippedSheet()
    {
        List<String> drifts = shadowDrifts(SHIPPED);

        assertTrue(drifts.isEmpty(), () -> "Tabler's shadow settings changed, update the shadow constants of TablerTokenCss: " + drifts);
    }

    /**
     * Negative control: a Tabler update that moves the large radius from 8px to 9px must turn the guard red
     */
    @Test
    public void testTheRadiusGuardRejectsAChangedRatio()
    {
        String largeRadius = SHIPPED.lastValue("--bs-border-radius-lg");
        assertEquals("calc(8px * var(--bs-border-radius-scale, 1))", largeRadius, "the control expects Tabler's compiled form");
        TablerSheet doctored = TablerSheet.of(SHIPPED.getCss() + ":root{--bs-border-radius-lg:calc(9px * var(--bs-border-radius-scale, 1))}");

        List<String> drifts = radiusDrifts(doctored);

        assertEquals(1, drifts.size(), drifts.toString());
        assertTrue(drifts.get(0)
                         .startsWith("--bs-border-radius-lg is 9"),
                   drifts.toString());
    }

    /**
     * Negative control: a Tabler update that changes the default shadow colour alpha must turn the guard red
     */
    @Test
    public void testTheShadowGuardRejectsAChangedDefaultShadowColour()
    {
        TablerSheet doctored = TablerSheet.of(SHIPPED.getCss() + ":root{--bs-shadow-color:light-dark(rgba(18, 18, 23, 0.5), #000)}");

        List<String> drifts = shadowDrifts(doctored);

        assertFalse(drifts.isEmpty());
        assertTrue(drifts.stream()
                         .anyMatch(drift -> drift.contains("light shadow alpha is 0.5")),
                   drifts.toString());
    }
}
