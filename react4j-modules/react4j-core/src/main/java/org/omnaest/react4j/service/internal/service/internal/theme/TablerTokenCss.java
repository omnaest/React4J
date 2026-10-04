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

import java.math.BigDecimal;
import java.util.Map;
import java.util.StringJoiner;

import org.omnaest.react4j.domain.configuration.ThemeConfiguration.ShadowStrength;
import org.omnaest.react4j.service.internal.service.CssLength;
import org.omnaest.react4j.service.internal.service.RgbColor;
import org.omnaest.react4j.service.internal.service.ThemeColorRole;
import org.omnaest.react4j.service.internal.service.ThemeTokens;

/**
 * Renders {@link ThemeTokens} into the CSS text of the design token {@code <style>} block of the {@code TABLER} preset (without the element itself).
 * Pure function.
 * <br>
 * <br>
 * In contrast to {@link ThemeTokenCss} this writes <b>base custom properties only</b>, in one rule, and never a component rule such as
 * {@code .btn-primary}: Tabler derives the hover, active and subtle shades, the focus rings and the translucent variants in the browser from its base
 * variables ({@code color-mix()}, {@code oklch()}, {@code light-dark()}), so changing the base variable recolours every component, in light and dark
 * mode alike. Nothing here is free text: colours, lengths, the font list and the shadow strength arrive as validated value types, and the remaining
 * text is constant.
 * <br>
 * <br>
 * <b>What is written per token</b> (every name is one the compiled {@code react4j-tabler.css} READS, which {@code TablerTokenCssSheetGuardTest}
 * proves against the shipped sheet, so a Tabler update cannot silently detach a token):
 * <ul>
 * <li>a colour role: {@code --bs-<role>} (hex) and {@code --bs-<role>-rgb} (the integer triplet), nothing else. {@code --bs-<role>-fg} (the text on
 * top of the colour, Tabler's {@code var(--bs-light)}) is deliberately NOT derived, so choose role colours dark enough for light text.</li>
 * <li>the font family: {@code --bs-font-sans-serif}, which {@code --bs-body-font-family} follows</li>
 * <li>the base font size: {@code --bs-body-font-size}</li>
 * <li>the radius: {@code --bs-border-radius-md} (Tabler's base radius, which {@code --bs-border-radius} follows) equal to the configured value and
 * the other sizes Tabler reads scaled with its own ratios, see {@link RadiusStep}</li>
 * <li>the shadow strength: the alpha of {@code --bs-shadow-color} in both arms of its {@code light-dark()}, which every elevation shadow derives from.
 * {@code --bs-shadow-border}, a border drawn with {@code box-shadow}, is pinned to its default so a strength never changes a border.</li>
 * </ul>
 * <b>Settings of Tabler that this class mirrors</b> (guarded by {@code TablerTokenCssSheetGuardTest}, which compares each one with the shipped sheet):
 * the radius ratios of {@link RadiusStep}, the default shadow colour with its alpha and the shape of {@code --bs-shadow-border}.
 *
 * @author omnaest
 */
final class TablerTokenCss
{
    /**
     * The rule selector. The role colours, font, radius and shadow base variables are declared by the sheet in {@code :root}; {@code --bs-secondary}
     * is declared once more in {@code :root,:host,[data-bs-theme=light],[data-theme=light]}. All of these have the specificity of one pseudo-class or
     * attribute, so a rule with exactly these selectors, placed after the sheet, wins in every colour mode and inside a nested light scope. The sheet's
     * dark block redefines none of these variables.
     */
    static final String             SELECTOR               = ":root,[data-bs-theme=light],[data-theme=light]";

    /**
     * Tabler's {@code --bs-shadow-color} as compiled: {@code light-dark(rgba(18, 18, 23, 0.4), #000)}. Only a fixed alpha is scaled below, so the colour
     * channels are mirrored separately; both are compared with the sheet by the guard.
     */
    static final String             DEFAULT_SHADOW_COLOR   = "light-dark(rgba(18, 18, 23, 0.4), #000)";
    static final String             SHADOW_LIGHT_CHANNELS  = "18, 18, 23";
    static final BigDecimal         SHADOW_LIGHT_ALPHA     = new BigDecimal("0.4");
    /**
     * The dark arm of the default shadow colour is {@code #000}, i.e. opaque black: it cannot be strengthened, only weakened
     */
    static final BigDecimal         SHADOW_DARK_ALPHA      = BigDecimal.ONE;
    static final String             SHADOW_DARK_CHANNELS   = "0, 0, 0";

    /**
     * The compiled {@code --bs-shadow-border}, a 1px ring of the shadow colour; the placeholder takes the colour. It is the outline of mentions and
     * avatars, i.e. a border drawn with {@code box-shadow}.
     */
    static final String             SHADOW_BORDER_TEMPLATE = "0px 0px 0px 1px color-mix(in oklab, %s 25%%, transparent)";
    static final String             SHADOW_COLOR_VARIABLE  = "var(--bs-shadow-color)";

    /**
     * Tabler's base radius, which {@code --bs-border-radius} follows ({@code var(--bs-border-radius-md)})
     */
    static final String             RADIUS_BASE_NAME       = "--bs-border-radius-md";

    private static final String     COLOR_PREFIX           = "--bs-";
    private static final String     RGB_SUFFIX             = "-rgb";

    private static final BigDecimal SHADOW_NONE            = BigDecimal.ZERO;
    private static final BigDecimal SHADOW_SUBTLE          = new BigDecimal("0.5");
    private static final BigDecimal SHADOW_STRONG          = new BigDecimal("2");

    /**
     * The radius sizes Tabler reads besides its base size, as a ratio of the base ({@code --bs-border-radius-md}, 6px as compiled). Tabler compiles
     * them as {@code calc(<n> * var(--bs-border-radius-scale, 1))} with 4px, 8px, 1rem and 2rem, i.e. 2/3, 4/3, 8/3 and 16/3 of 6px. The ratio is
     * written as numerator and denominator inside {@code calc()}, so it is exact and works for {@code px} and {@code rem} alike. The extra small size
     * ({@code --bs-border-radius-xs}) is not read by the sheet and therefore not written.
     */
    enum RadiusStep
    {
        SMALL("--bs-border-radius-sm", 2, 3), LARGE("--bs-border-radius-lg", 4, 3), EXTRA_LARGE("--bs-border-radius-xl", 8, 3), EXTRA_EXTRA_LARGE("--bs-border-radius-xxl", 16, 3);

        private final String name;
        private final int    numerator;
        private final int    denominator;

        RadiusStep(String name, int numerator, int denominator)
        {
            this.name = name;
            this.numerator = numerator;
            this.denominator = denominator;
        }

        String getName()
        {
            return this.name;
        }

        int getNumerator()
        {
            return this.numerator;
        }

        int getDenominator()
        {
            return this.denominator;
        }

        private String valueOf(CssLength base)
        {
            return "calc(" + base.toCss() + " * " + this.numerator + " / " + this.denominator + ")";
        }
    }

    private TablerTokenCss()
    {
    }

    /**
     * @return the CSS text, or an empty string if no token is set or the only token is the default shadow strength, which is Tabler as compiled
     */
    static String render(ThemeTokens tokens)
    {
        StringJoiner declarations = new StringJoiner(";");

        addColors(tokens.getColors(), declarations);
        if (tokens.getFontFamily() != null)
        {
            declarations.add("--bs-font-sans-serif:" + tokens.getFontFamily()
                                                             .getCss());
        }
        if (tokens.getBaseFontSize() != null)
        {
            declarations.add("--bs-body-font-size:" + tokens.getBaseFontSize()
                                                            .toCss());
        }
        addRadius(tokens.getBorderRadius(), declarations);
        addShadow(tokens.getShadowStrength(), declarations);

        return declarations.length() == 0 ? "" : SELECTOR + "{" + declarations + "}";
    }

    private static void addColors(Map<ThemeColorRole, RgbColor> colors, StringJoiner declarations)
    {
        for (ThemeColorRole role : ThemeColorRole.values())
        {
            RgbColor color = colors.get(role);
            if (color != null)
            {
                String name = COLOR_PREFIX + role.getCssName();
                declarations.add(name + ":" + CssText.color(color));
                declarations.add(name + RGB_SUFFIX + ":" + CssText.triplet(new int[] {color.roundedRed(), color.roundedGreen(), color.roundedBlue()}));
            }
        }
    }

    private static void addRadius(CssLength radius, StringJoiner declarations)
    {
        if (radius != null)
        {
            declarations.add(RADIUS_BASE_NAME + ":" + radius.toCss());
            for (RadiusStep step : RadiusStep.values())
            {
                declarations.add(step.getName() + ":" + step.valueOf(radius));
            }
        }
    }

    private static void addShadow(ShadowStrength strength, StringJoiner declarations)
    {
        if (strength == null || strength == ShadowStrength.DEFAULT)
        {
            return;
        }
        declarations.add("--bs-shadow-color:" + shadowColor(shadowFactor(strength)));
        declarations.add("--bs-shadow-border:" + String.format(SHADOW_BORDER_TEMPLATE, DEFAULT_SHADOW_COLOR));
    }

    private static BigDecimal shadowFactor(ShadowStrength strength)
    {
        switch (strength)
        {
            case NONE :
                return SHADOW_NONE;
            case SUBTLE :
                return SHADOW_SUBTLE;
            case STRONG :
                return SHADOW_STRONG;
            default :
                return BigDecimal.ONE;
        }
    }

    /**
     * The {@code light-dark()} shadow colour of Tabler with both alphas multiplied by the factor, the dark one capped at 1 (opaque)
     */
    static String shadowColor(BigDecimal factor)
    {
        BigDecimal light = SHADOW_LIGHT_ALPHA.multiply(factor);
        BigDecimal dark = SHADOW_DARK_ALPHA.multiply(factor)
                                           .min(BigDecimal.ONE);
        return "light-dark(rgba(" + SHADOW_LIGHT_CHANNELS + ", " + alpha(light) + "), rgba(" + SHADOW_DARK_CHANNELS + ", " + alpha(dark) + "))";
    }

    private static String alpha(BigDecimal value)
    {
        BigDecimal stripped = value.stripTrailingZeros();
        return stripped.signum() == 0 ? "0" : stripped.toPlainString();
    }
}
