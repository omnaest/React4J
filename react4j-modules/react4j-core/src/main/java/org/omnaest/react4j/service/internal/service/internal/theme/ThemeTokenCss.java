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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.omnaest.react4j.domain.configuration.ThemeConfiguration.ShadowStrength;
import org.omnaest.react4j.service.internal.service.CssLength;
import org.omnaest.react4j.service.internal.service.RgbColor;
import org.omnaest.react4j.service.internal.service.ThemeColorRole;
import org.omnaest.react4j.service.internal.service.ThemeTokens;
import org.omnaest.react4j.service.internal.service.internal.theme.BootstrapColorDeriver.ButtonVariant;
import org.omnaest.react4j.service.internal.service.internal.theme.BootstrapColorDeriver.OutlineButtonVariant;
import org.omnaest.react4j.service.internal.service.internal.theme.BootstrapColorDeriver.TableVariant;

/**
 * Renders {@link ThemeTokens} into the CSS text of the design token {@code <style>} block (without the element itself). Pure function.
 * <br>
 * <br>
 * Bootstrap compiles its component variables as literals (a {@code .btn-primary} carries {@code --bs-btn-bg: #4f46e5} itself), so setting
 * {@code --bs-primary} on {@code :root} does not recolour a button. The output is therefore two parts: the {@code :root}/dark variable blocks, and one
 * rule per compiled rule that carries the colour or a value derived from it, with exactly the selector Bootstrap compiled it under. Every colour is
 * derived by {@link BootstrapColorDeriver}. Nothing here is free text: colours, lengths, the font list and the shadow strength arrive as validated
 * value types, and the remaining text is constant.
 * <br>
 * <br>
 * The primary colour carries the most rules, because Bootstrap paints form focus, checked states, active navigation, pagination, progress and list
 * items with it. Which rules those are is fixed by the compiled-sheet coverage guard test, which fails if the shipped stylesheet contains a rule
 * carrying a primary-derived value that this class does not cover.
 * <br>
 * <br>
 * <b>Settings of the react4j-modern theme that this class mirrors</b> (guarded by the compiled-sheet equality test): the focus ring opacity 0.4 and
 * width 0.25rem, the body background {@code #f7f8fa}, the shadow shapes of the modern theme, and the radius ratios 0.75 / 1.5 / 2 of the radius scale.
 *
 * @author omnaest
 */
final class ThemeTokenCss
{
    private static final String   LIGHT_SELECTOR           = ":root,[data-bs-theme=light]";
    private static final String   DARK_SELECTOR            = "[data-bs-theme=dark]";

    /**
     * {@code $focus-ring-opacity} and {@code $input-btn-focus-color-opacity} of the react4j-modern theme
     */
    private static final String   FOCUS_RING_ALPHA         = "0.4";
    /**
     * {@code $focus-ring-width} of the react4j-modern theme
     */
    private static final String   FOCUS_RING_WIDTH         = "0.25rem";
    /**
     * {@code $body-bg} of the react4j-modern theme
     */
    private static final String   BODY_BACKGROUND          = "#f7f8fa";
    /**
     * {@code $btn-active-box-shadow}: Bootstrap's default
     */
    private static final String   BUTTON_ACTIVE_SHADOW     = "inset 0 3px 5px rgba(0, 0, 0, 0.125)";

    /**
     * {@code $green-300} and {@code $red-300}: Bootstrap's dark mode form validation colours, {@code tint-color($green, 40%)} and
     * {@code tint-color($red, 40%)} of Bootstrap's own palette, which are NOT derived from the configured success and danger colours
     */
    private static final RgbColor GREEN_300                = BootstrapColorFunctions.tintColor(RgbColor.parseHex("#198754"), 40);
    private static final RgbColor RED_300                  = BootstrapColorFunctions.tintColor(RgbColor.parseHex("#dc3545"), 40);

    private static final String   RADIUS_SMALL_RATIO       = "0.75";
    private static final String   RADIUS_LARGE_RATIO       = "1.5";
    private static final String   RADIUS_EXTRA_LARGE_RATIO = "2";

    private static final String   VALID_CONTROL            = ".was-validated .form-control:valid,.form-control.is-valid";
    private static final String   VALID_SELECT             = ".was-validated .form-select:valid:not([multiple]):not([size]),.was-validated .form-select:valid:not([multiple])[size=\"1\"],.form-select.is-valid:not([multiple]):not([size]),.form-select.is-valid:not([multiple])[size=\"1\"]";
    private static final String   INVALID_CONTROL          = ".was-validated .form-control:invalid,.form-control.is-invalid";
    private static final String   INVALID_SELECT           = ".was-validated .form-select:invalid:not([multiple]):not([size]),.was-validated .form-select:invalid:not([multiple])[size=\"1\"],.form-select.is-invalid:not([multiple]):not([size]),.form-select.is-invalid:not([multiple])[size=\"1\"]";

    private ThemeTokenCss()
    {
    }

    /**
     * @return the CSS text, or an empty string if no token is set
     */
    static String render(ThemeTokens tokens)
    {
        if (tokens.isEmpty())
        {
            return "";
        }

        List<Rule> rules = new ArrayList<>();
        Rule light = new Rule(LIGHT_SELECTOR);
        Rule dark = new Rule(DARK_SELECTOR);
        rules.add(light);
        rules.add(dark);

        Map<ThemeColorRole, RgbColor> colors = tokens.getColors();
        for (ThemeColorRole role : ThemeColorRole.values())
        {
            RgbColor color = colors.get(role);
            if (color != null)
            {
                addColorVariables(role, color, light, dark);
                addColorRules(role, color, rules);
            }
        }

        addTypography(tokens, light, rules);
        addShape(tokens, light, rules);
        addShadow(tokens.getShadowStrength(), light, rules);

        return rules.stream()
                    .filter(rule -> !rule.isEmpty())
                    .map(Rule::toCss)
                    .collect(Collectors.joining("\n"));
    }

    // ---- colour variables ------------------------------------------------------------------------------------------

    private static void addColorVariables(ThemeColorRole role, RgbColor color, Rule light, Rule dark)
    {
        String name = role.getCssName();
        light.add("--bs-" + name, CssText.color(color));
        light.add("--bs-" + name + "-rgb", CssText.triplet(BootstrapColorFunctions.toRgb(color)));
        light.add("--bs-" + name + "-text-emphasis", CssText.color(BootstrapColorDeriver.textEmphasis(color, false)));
        light.add("--bs-" + name + "-bg-subtle", CssText.color(BootstrapColorDeriver.backgroundSubtle(color, false)));
        light.add("--bs-" + name + "-border-subtle", CssText.color(BootstrapColorDeriver.borderSubtle(color, false)));
        dark.add("--bs-" + name + "-text-emphasis", CssText.color(BootstrapColorDeriver.textEmphasis(color, true)));
        dark.add("--bs-" + name + "-bg-subtle", CssText.color(BootstrapColorDeriver.backgroundSubtle(color, true)));
        dark.add("--bs-" + name + "-border-subtle", CssText.color(BootstrapColorDeriver.borderSubtle(color, true)));

        if (role == ThemeColorRole.PRIMARY)
        {
            addLinkVariables(light, color, false);
            addLinkVariables(dark, color, true);
            light.add("--bs-focus-ring-color", CssText.rgba(color, FOCUS_RING_ALPHA));
        }
        else if (role == ThemeColorRole.SUCCESS)
        {
            light.add("--bs-form-valid-color", CssText.color(color));
            light.add("--bs-form-valid-border-color", CssText.color(color));
            // the light block also matches the root element in dark mode and comes after the compiled dark block, so the dark constants are restated
            dark.add("--bs-form-valid-color", CssText.color(GREEN_300));
            dark.add("--bs-form-valid-border-color", CssText.color(GREEN_300));
        }
        else if (role == ThemeColorRole.DANGER)
        {
            light.add("--bs-form-invalid-color", CssText.color(color));
            light.add("--bs-form-invalid-border-color", CssText.color(color));
            dark.add("--bs-form-invalid-color", CssText.color(RED_300));
            dark.add("--bs-form-invalid-border-color", CssText.color(RED_300));
        }
    }

    private static void addLinkVariables(Rule target, RgbColor primary, boolean dark)
    {
        RgbColor link = BootstrapColorDeriver.linkColor(primary, dark);
        RgbColor hover = BootstrapColorDeriver.linkHoverColor(link, dark);
        target.add("--bs-link-color", CssText.color(link));
        target.add("--bs-link-color-rgb", CssText.triplet(BootstrapColorFunctions.toRgb(link)));
        target.add("--bs-link-hover-color", CssText.color(hover));
        target.add("--bs-link-hover-color-rgb", CssText.triplet(BootstrapColorFunctions.toRgb(hover)));
    }

    // ---- per colour component rules --------------------------------------------------------------------------------

    private static void addColorRules(ThemeColorRole role, RgbColor color, List<Rule> rules)
    {
        String name = role.getCssName();
        rules.add(buttonRule(".btn-" + name, BootstrapColorDeriver.buttonVariant(color)));
        rules.add(outlineButtonRule(".btn-outline-" + name, BootstrapColorDeriver.buttonOutlineVariant(color)));
        rules.add(new Rule(".text-bg-" + name).important("color", CssText.color(BootstrapColorDeriver.textOn(color))));
        rules.add(linkHelperRule(name, color));
        rules.add(tableRule(".table-" + name, BootstrapColorDeriver.tableVariant(color)));

        if (role == ThemeColorRole.PRIMARY)
        {
            addPrimaryRules(color, rules);
        }
        else if (role == ThemeColorRole.SUCCESS)
        {
            String icon = validIcon(color);
            rules.add(new Rule(VALID_CONTROL).add("background-image", icon));
            rules.add(new Rule(VALID_SELECT).add("--bs-form-select-bg-icon", icon));
        }
        else if (role == ThemeColorRole.DANGER)
        {
            String icon = invalidIcon(color);
            rules.add(new Rule(INVALID_CONTROL).add("background-image", icon));
            rules.add(new Rule(INVALID_SELECT).add("--bs-form-select-bg-icon", icon));
        }
    }

    private static Rule buttonRule(String selector, ButtonVariant variant)
    {
        return new Rule(selector).add("--bs-btn-color", CssText.color(variant.getColor()))
                                 .add("--bs-btn-bg", CssText.color(variant.getBackground()))
                                 .add("--bs-btn-border-color", CssText.color(variant.getBorder()))
                                 .add("--bs-btn-hover-color", CssText.color(variant.getHoverColor()))
                                 .add("--bs-btn-hover-bg", CssText.color(variant.getHoverBackground()))
                                 .add("--bs-btn-hover-border-color", CssText.color(variant.getHoverBorder()))
                                 .add("--bs-btn-focus-shadow-rgb", CssText.triplet(variant.getFocusShadowRgb()))
                                 .add("--bs-btn-active-color", CssText.color(variant.getActiveColor()))
                                 .add("--bs-btn-active-bg", CssText.color(variant.getActiveBackground()))
                                 .add("--bs-btn-active-border-color", CssText.color(variant.getActiveBorder()))
                                 .add("--bs-btn-active-shadow", BUTTON_ACTIVE_SHADOW)
                                 .add("--bs-btn-disabled-color", CssText.color(variant.getDisabledColor()))
                                 .add("--bs-btn-disabled-bg", CssText.color(variant.getDisabledBackground()))
                                 .add("--bs-btn-disabled-border-color", CssText.color(variant.getDisabledBorder()));
    }

    private static Rule outlineButtonRule(String selector, OutlineButtonVariant variant)
    {
        return new Rule(selector).add("--bs-btn-color", CssText.color(variant.getColor()))
                                 .add("--bs-btn-border-color", CssText.color(variant.getBorder()))
                                 .add("--bs-btn-hover-color", CssText.color(variant.getHoverColor()))
                                 .add("--bs-btn-hover-bg", CssText.color(variant.getHoverBackground()))
                                 .add("--bs-btn-hover-border-color", CssText.color(variant.getHoverBorder()))
                                 .add("--bs-btn-focus-shadow-rgb", CssText.triplet(variant.getFocusShadowRgb()))
                                 .add("--bs-btn-active-color", CssText.color(variant.getActiveColor()))
                                 .add("--bs-btn-active-bg", CssText.color(variant.getActiveBackground()))
                                 .add("--bs-btn-active-border-color", CssText.color(variant.getActiveBorder()))
                                 .add("--bs-btn-active-shadow", BUTTON_ACTIVE_SHADOW)
                                 .add("--bs-btn-disabled-color", CssText.color(variant.getDisabledColor()))
                                 .add("--bs-btn-disabled-bg", "transparent")
                                 .add("--bs-btn-disabled-border-color", CssText.color(variant.getDisabledBorder()))
                                 .add("--bs-gradient", "none");
    }

    private static Rule linkHelperRule(String name, RgbColor color)
    {
        String hover = CssText.triplet(BootstrapColorFunctions.toRgb(BootstrapColorDeriver.linkHelperHoverColor(color)));
        return new Rule(".link-" + name + ":hover,.link-" + name + ":focus").important("color", "RGBA(" + hover + ", var(--bs-link-opacity, 1))")
                                                                            .important("text-decoration-color",
                                                                                       "RGBA(" + hover + ", var(--bs-link-underline-opacity, 1))");
    }

    private static Rule tableRule(String selector, TableVariant variant)
    {
        return new Rule(selector).add("--bs-table-color", CssText.color(variant.getColor()))
                                 .add("--bs-table-bg", CssText.color(variant.getBackground()))
                                 .add("--bs-table-border-color", CssText.color(variant.getBorderColor()))
                                 .add("--bs-table-striped-bg", CssText.color(variant.getStripedBackground()))
                                 .add("--bs-table-striped-color", CssText.color(variant.getStripedColor()))
                                 .add("--bs-table-active-bg", CssText.color(variant.getActiveBackground()))
                                 .add("--bs-table-active-color", CssText.color(variant.getActiveColor()))
                                 .add("--bs-table-hover-bg", CssText.color(variant.getHoverBackground()))
                                 .add("--bs-table-hover-color", CssText.color(variant.getHoverColor()));
    }

    // ---- rules only the primary colour carries (the component active colour of Bootstrap is $primary) ----------------

    private static void addPrimaryRules(RgbColor primary, List<Rule> rules)
    {
        String focusBorder = CssText.color(BootstrapColorDeriver.focusBorder(primary));
        String focusShadow = focusRingShadow(primary);
        String solid = CssText.color(primary);

        for (String control : new String[] {".form-control:focus", ".form-select:focus", ".form-check-input:focus"})
        {
            rules.add(new Rule(control).add("border-color", focusBorder)
                                       .add("box-shadow", focusShadow));
        }
        rules.add(new Rule(".form-check-input:checked").add("background-color", solid)
                                                       .add("border-color", solid));
        rules.add(new Rule(".form-check-input[type=checkbox]:indeterminate").add("background-color", solid)
                                                                            .add("border-color", solid));
        rules.add(new Rule(".form-switch .form-check-input:focus").add("--bs-form-switch-bg", switchKnob(BootstrapColorDeriver.focusBorder(primary))));
        // restated to keep Bootstrap's order (focus first, checked after): this block comes after the whole stylesheet, so without it a focused
        // checked switch would show the tinted focus knob instead of the white checked one
        rules.add(new Rule(".form-switch .form-check-input:checked").add("--bs-form-switch-bg", switchKnob(RgbColor.WHITE)));

        String thumbFocusShadow = "0 0 0 1px " + BODY_BACKGROUND + ", " + focusShadow;
        String thumbActive = CssText.color(BootstrapColorDeriver.rangeThumbActive(primary));
        for (String thumb : new String[] {"::-webkit-slider-thumb", "::-moz-range-thumb"})
        {
            rules.add(new Rule(".form-range:focus" + thumb).add("box-shadow", thumbFocusShadow));
            rules.add(new Rule(".form-range" + thumb).add("background-color", solid));
            rules.add(new Rule(".form-range" + thumb + ":active").add("background-color", thumbActive));
        }

        rules.add(new Rule(".btn-link").add("--bs-btn-focus-shadow-rgb", CssText.triplet(BootstrapColorDeriver.linkButtonFocusShadowRgb(primary))));
        rules.add(new Rule(".dropdown-menu").add("--bs-dropdown-link-active-bg", solid));
        rules.add(new Rule(".dropdown-menu-dark").add("--bs-dropdown-link-active-bg", solid));
        rules.add(new Rule(".nav-link:focus-visible").add("box-shadow", focusShadow));
        rules.add(new Rule(".nav-pills").add("--bs-nav-pills-link-active-bg", solid));

        String accordionActiveIcon = accordionChevron(BootstrapColorDeriver.textEmphasis(primary, false));
        rules.add(new Rule(".accordion").add("--bs-accordion-btn-active-icon", accordionActiveIcon)
                                        .add("--bs-accordion-btn-focus-border-color", focusBorder)
                                        .add("--bs-accordion-btn-focus-box-shadow", focusShadow));
        String accordionDarkIcon = accordionChevron(BootstrapColorDeriver.textEmphasis(primary, true));
        rules.add(new Rule("[data-bs-theme=dark] .accordion-button::after").add("--bs-accordion-btn-icon", accordionDarkIcon)
                                                                           .add("--bs-accordion-btn-active-icon", accordionDarkIcon));

        rules.add(new Rule(".pagination").add("--bs-pagination-focus-box-shadow", focusShadow)
                                         .add("--bs-pagination-active-bg", solid)
                                         .add("--bs-pagination-active-border-color", solid));
        rules.add(new Rule(".progress,.progress-stacked").add("--bs-progress-bar-bg", solid));
        rules.add(new Rule(".list-group").add("--bs-list-group-active-bg", solid)
                                         .add("--bs-list-group-active-border-color", solid));
        rules.add(new Rule(".btn-close").add("--bs-btn-close-focus-shadow", focusShadow));
    }

    private static String focusRingShadow(RgbColor primary)
    {
        return "0 0 0 " + FOCUS_RING_WIDTH + " " + CssText.rgba(primary, FOCUS_RING_ALPHA);
    }

    // ---- inline SVG images of Bootstrap, with the colour written the way its escape-svg leaves it -----------------------

    private static String switchKnob(RgbColor fill)
    {
        return svg("%3csvg xmlns='http://www.w3.org/2000/svg' viewBox='-4 -4 8 8'%3e%3ccircle r='3' fill='" + CssText.svgPaint(fill) + "'/%3e%3c/svg%3e");
    }

    private static String accordionChevron(RgbColor fill)
    {
        return svg("%3csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 16 16' fill='" + CssText.svgPaint(fill)
                   + "'%3e%3cpath fill-rule='evenodd' d='M1.646 4.646a.5.5 0 0 1 .708 0L8 10.293l5.646-5.647a.5.5 0 0 1 .708.708l-6 6a.5.5 0 0 1-.708 0l-6-6a.5.5 0 0 1 0-.708z'/%3e%3c/svg%3e");
    }

    private static String validIcon(RgbColor fill)
    {
        return svg("%3csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 8 8'%3e%3cpath fill='" + CssText.svgPaint(fill)
                   + "' d='M2.3 6.73.6 4.53c-.4-1.04.46-1.4 1.1-.8l1.1 1.4 3.4-3.8c.6-.63 1.6-.27 1.2.7l-4 4.6c-.43.5-.8.4-1.1.1z'/%3e%3c/svg%3e");
    }

    private static String invalidIcon(RgbColor fill)
    {
        String paint = CssText.svgPaint(fill);
        return svg("%3csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 12 12' width='12' height='12' fill='none' stroke='" + paint
                   + "'%3e%3ccircle cx='6' cy='6' r='4.5'/%3e%3cpath stroke-linejoin='round' d='M5.8 3.6h.4L6 6.5z'/%3e%3ccircle cx='6' cy='8.2' r='.6' fill='" + paint
                   + "' stroke='none'/%3e%3c/svg%3e");
    }

    private static String svg(String encodedMarkup)
    {
        return "url(\"data:image/svg+xml," + encodedMarkup + "\")";
    }

    // ---- typography, shape, shadow ----------------------------------------------------------------------------------

    private static void addTypography(ThemeTokens tokens, Rule light, List<Rule> rules)
    {
        if (tokens.getFontFamily() != null)
        {
            String family = tokens.getFontFamily()
                                  .getCss();
            light.add("--bs-body-font-family", family);
            // the two places where Bootstrap compiled the base font family as a literal instead of a variable reference
            rules.add(new Rule(".tooltip").add("font-family", family));
            rules.add(new Rule(".popover").add("font-family", family));
        }
        if (tokens.getBaseFontSize() != null)
        {
            light.add("--bs-body-font-size", tokens.getBaseFontSize()
                                                   .toCss());
        }
    }

    private static void addShape(ThemeTokens tokens, Rule light, List<Rule> rules)
    {
        CssLength radius = tokens.getBorderRadius();
        if (radius != null)
        {
            String small = radius.scaled(RADIUS_SMALL_RATIO)
                                 .toCss();
            light.add("--bs-border-radius", radius.toCss());
            light.add("--bs-border-radius-sm", small);
            light.add("--bs-border-radius-lg", radius.scaled(RADIUS_LARGE_RATIO)
                                                     .toCss());
            light.add("--bs-border-radius-xl", radius.scaled(RADIUS_EXTRA_LARGE_RATIO)
                                                     .toCss());
            // the two places where Bootstrap compiled the radius as a literal instead of a variable reference
            rules.add(new Rule("kbd").add("border-radius", small));
            rules.add(new Rule(".btn-close").add("border-radius", radius.toCss()));
        }
    }

    private static void addShadow(ShadowStrength strength, Rule light, List<Rule> rules)
    {
        if (strength == null)
        {
            return;
        }
        String small;
        String regular;
        String large;
        switch (strength)
        {
            case NONE :
                small = "none";
                regular = "none";
                large = "none";
                break;
            case SUBTLE :
                small = shadow("0 1px 2px", "0.03");
                regular = shadow("0 4px 12px", "0.04");
                large = shadow("0 12px 32px", "0.07");
                break;
            case STRONG :
                small = shadow("0 1px 2px", "0.12");
                regular = shadow("0 4px 12px", "0.16");
                large = shadow("0 12px 32px", "0.28");
                break;
            case DEFAULT :
            default :
                small = shadow("0 1px 2px", "0.06");
                regular = shadow("0 4px 12px", "0.08");
                large = shadow("0 12px 32px", "0.14");
                break;
        }
        light.add("--bs-box-shadow", regular);
        light.add("--bs-box-shadow-sm", small);
        light.add("--bs-box-shadow-lg", large);
        // the theme sets $card-box-shadow to the small shadow, which Bootstrap compiles as a literal into .card
        rules.add(new Rule(".card").add("--bs-card-box-shadow", small));
    }

    private static String shadow(String geometry, String alpha)
    {
        return geometry + " rgba(16, 24, 40, " + alpha + ")";
    }

    // ---- one CSS rule ------------------------------------------------------------------------------------------------

    private static final class Rule
    {
        private final String        selector;
        private final StringBuilder declarations = new StringBuilder();

        Rule(String selector)
        {
            this.selector = selector;
        }

        Rule add(String property, String value)
        {
            return this.append(property, value, "");
        }

        Rule important(String property, String value)
        {
            return this.append(property, value, " !important");
        }

        private Rule append(String property, String value, String suffix)
        {
            if (this.declarations.length() > 0)
            {
                this.declarations.append(';');
            }
            this.declarations.append(property)
                             .append(':')
                             .append(value)
                             .append(suffix);
            return this;
        }

        boolean isEmpty()
        {
            return this.declarations.length() == 0;
        }

        String toCss()
        {
            return this.selector + "{" + this.declarations + "}";
        }
    }
}
