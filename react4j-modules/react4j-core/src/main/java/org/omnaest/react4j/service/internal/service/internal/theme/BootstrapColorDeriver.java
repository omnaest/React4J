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

import static org.omnaest.react4j.service.internal.service.internal.theme.BootstrapColorFunctions.CONTRAST_LIGHT;
import static org.omnaest.react4j.service.internal.service.internal.theme.BootstrapColorFunctions.colorContrast;
import static org.omnaest.react4j.service.internal.service.internal.theme.BootstrapColorFunctions.mix;
import static org.omnaest.react4j.service.internal.service.internal.theme.BootstrapColorFunctions.shadeColor;
import static org.omnaest.react4j.service.internal.service.internal.theme.BootstrapColorFunctions.shiftColor;
import static org.omnaest.react4j.service.internal.service.internal.theme.BootstrapColorFunctions.tintColor;

import org.omnaest.react4j.service.internal.service.RgbColor;

import lombok.Value;

/**
 * Derives every colour Bootstrap 5.3.2 computes from one theme colour, with the formulas and amounts of Bootstrap's own Sass: the variants of
 * {@code _variables.scss} and {@code _variables-dark.scss} (text emphasis, subtle background, subtle border, the link colours), the button mixins of
 * {@code mixins/_buttons.scss}, the table variant mixin and the form helper colours. A pure algorithmic class without state.
 * <br>
 * <br>
 * <b>Settings of the react4j-modern theme that this class mirrors.</b> The theme ({@code react4j-modern.scss}) leaves all of these at Bootstrap's
 * defaults on purpose, so the amounts below are Bootstrap's: {@code $btn-hover-bg-shade-amount} 15%, {@code $btn-hover-bg-tint-amount} 15%,
 * {@code $btn-hover-border-shade-amount} 20%, {@code $btn-hover-border-tint-amount} 10%, {@code $btn-active-bg-shade-amount} 20%,
 * {@code $btn-active-bg-tint-amount} 20%, {@code $btn-active-border-shade-amount} 25%, {@code $btn-active-border-tint-amount} 10%,
 * {@code $link-shade-percentage} 20%, {@code $table-bg-scale} -80%, the table factors, and the contrast pair and minimum ratio of
 * {@link BootstrapColorFunctions}. Drift between the scss and these constants is guarded by the compiled-sheet equality test.
 *
 * @author omnaest
 */
final class BootstrapColorDeriver
{
    private static final double BTN_HOVER_BG_SHADE      = 15;
    private static final double BTN_HOVER_BG_TINT       = 15;
    private static final double BTN_HOVER_BORDER_SHADE  = 20;
    private static final double BTN_HOVER_BORDER_TINT   = 10;
    private static final double BTN_ACTIVE_BG_SHADE     = 20;
    private static final double BTN_ACTIVE_BG_TINT      = 20;
    private static final double BTN_ACTIVE_BORDER_SHADE = 25;
    private static final double BTN_ACTIVE_BORDER_TINT  = 10;
    private static final double BTN_FOCUS_SHADOW_MIX    = 15;
    private static final double LINK_SHADE_PERCENTAGE   = 20;
    private static final double TABLE_BG_SCALE          = -80;
    private static final double TABLE_STRIPED_FACTOR    = 5;
    private static final double TABLE_ACTIVE_FACTOR     = 10;
    private static final double TABLE_HOVER_FACTOR      = 7.5;
    private static final double TABLE_BORDER_FACTOR     = 20;

    private BootstrapColorDeriver()
    {
    }

    /**
     * The custom properties set by Bootstrap's {@code button-variant} mixin for a solid button whose background and border are the theme colour
     */
    @Value
    static class ButtonVariant
    {
        RgbColor color;
        RgbColor background;
        RgbColor border;
        RgbColor hoverColor;
        RgbColor hoverBackground;
        RgbColor hoverBorder;
        int[]    focusShadowRgb;
        RgbColor activeColor;
        RgbColor activeBackground;
        RgbColor activeBorder;
        RgbColor disabledColor;
        RgbColor disabledBackground;
        RgbColor disabledBorder;
    }

    /**
     * The custom properties set by Bootstrap's {@code button-outline-variant} mixin
     */
    @Value
    static class OutlineButtonVariant
    {
        RgbColor color;
        RgbColor border;
        RgbColor hoverColor;
        RgbColor hoverBackground;
        RgbColor hoverBorder;
        int[]    focusShadowRgb;
        RgbColor activeColor;
        RgbColor activeBackground;
        RgbColor activeBorder;
        RgbColor disabledColor;
        RgbColor disabledBorder;
    }

    /**
     * The custom properties set by Bootstrap's {@code table-variant} mixin
     */
    @Value
    static class TableVariant
    {
        RgbColor color;
        RgbColor background;
        RgbColor borderColor;
        RgbColor stripedBackground;
        RgbColor stripedColor;
        RgbColor activeBackground;
        RgbColor activeColor;
        RgbColor hoverBackground;
        RgbColor hoverColor;
    }

    /**
     * {@code button-variant($value, $value)} as {@code _buttons.scss} invokes it for every theme colour except light and dark
     */
    static ButtonVariant buttonVariant(RgbColor themeColor)
    {
        RgbColor background = themeColor;
        RgbColor border = themeColor;
        RgbColor color = colorContrast(background);
        boolean light = color.equals(CONTRAST_LIGHT);
        RgbColor hoverBackground = light ? shadeColor(background, BTN_HOVER_BG_SHADE) : tintColor(background, BTN_HOVER_BG_TINT);
        RgbColor hoverBorder = light ? shadeColor(border, BTN_HOVER_BORDER_SHADE) : tintColor(border, BTN_HOVER_BORDER_TINT);
        RgbColor activeBackground = light ? shadeColor(background, BTN_ACTIVE_BG_SHADE) : tintColor(background, BTN_ACTIVE_BG_TINT);
        RgbColor activeBorder = light ? shadeColor(border, BTN_ACTIVE_BORDER_SHADE) : tintColor(border, BTN_ACTIVE_BORDER_TINT);
        return new ButtonVariant(color, background, border, colorContrast(hoverBackground), hoverBackground, hoverBorder,
                                 BootstrapColorFunctions.toRgb(mix(color, border, BTN_FOCUS_SHADOW_MIX)), colorContrast(activeBackground), activeBackground,
                                 activeBorder, colorContrast(background), background, border);
    }

    /**
     * {@code button-outline-variant($value)}
     */
    static OutlineButtonVariant buttonOutlineVariant(RgbColor themeColor)
    {
        RgbColor contrast = colorContrast(themeColor);
        return new OutlineButtonVariant(themeColor, themeColor, contrast, themeColor, themeColor, BootstrapColorFunctions.toRgb(themeColor), contrast,
                                        themeColor, themeColor, themeColor, themeColor);
    }

    /**
     * {@code table-variant($state, shift-color($value, $table-bg-scale))}
     */
    static TableVariant tableVariant(RgbColor themeColor)
    {
        RgbColor background = shiftColor(themeColor, TABLE_BG_SCALE);
        RgbColor color = colorContrast(background);
        RgbColor hover = mix(color, background, TABLE_HOVER_FACTOR);
        RgbColor striped = mix(color, background, TABLE_STRIPED_FACTOR);
        RgbColor active = mix(color, background, TABLE_ACTIVE_FACTOR);
        RgbColor border = mix(color, background, TABLE_BORDER_FACTOR);
        return new TableVariant(color, background, border, striped, colorContrast(striped), active, colorContrast(active), hover, colorContrast(hover));
    }

    /**
     * {@code $<name>-text-emphasis}: {@code shade-color($color, 60%)} in light mode, {@code tint-color($color, 40%)} in dark mode
     */
    static RgbColor textEmphasis(RgbColor themeColor, boolean dark)
    {
        return dark ? tintColor(themeColor, 40) : shadeColor(themeColor, 60);
    }

    /**
     * {@code $<name>-bg-subtle}: {@code tint-color($color, 80%)} in light mode, {@code shade-color($color, 80%)} in dark mode
     */
    static RgbColor backgroundSubtle(RgbColor themeColor, boolean dark)
    {
        return dark ? shadeColor(themeColor, 80) : tintColor(themeColor, 80);
    }

    /**
     * {@code $<name>-border-subtle}: {@code tint-color($color, 60%)} in light mode, {@code shade-color($color, 40%)} in dark mode
     */
    static RgbColor borderSubtle(RgbColor themeColor, boolean dark)
    {
        return dark ? shadeColor(themeColor, 40) : tintColor(themeColor, 60);
    }

    /**
     * {@code $link-color}: the theme colour itself in light mode, {@code tint-color($primary, 40%)} in dark mode
     */
    static RgbColor linkColor(RgbColor primary, boolean dark)
    {
        return dark ? tintColor(primary, 40) : primary;
    }

    /**
     * {@code $link-hover-color}: {@code shift-color($link-color, $link-shade-percentage)}, a shade in light mode, and in dark mode
     * ({@code $link-hover-color-dark}) {@code shift-color($link-color-dark, -$link-shade-percentage)}, a tint
     */
    static RgbColor linkHoverColor(RgbColor linkColor, boolean dark)
    {
        return shiftColor(linkColor, dark ? -LINK_SHADE_PERCENTAGE : LINK_SHADE_PERCENTAGE);
    }

    /**
     * The hover colour of Bootstrap's {@code .link-<name>} helper: shaded on a colour that takes white text, tinted otherwise
     */
    static RgbColor linkHelperHoverColor(RgbColor themeColor)
    {
        return colorContrast(themeColor).equals(CONTRAST_LIGHT) ? shadeColor(themeColor, LINK_SHADE_PERCENTAGE) : tintColor(themeColor, LINK_SHADE_PERCENTAGE);
    }

    /**
     * {@code $input-focus-border-color}: {@code tint-color($component-active-bg, 50%)}, also the colour of the focused switch knob and the focused
     * accordion button border
     */
    static RgbColor focusBorder(RgbColor primary)
    {
        return tintColor(primary, 50);
    }

    /**
     * {@code $form-range-thumb-active-bg}: {@code tint-color($component-active-bg, 70%)}
     */
    static RgbColor rangeThumbActive(RgbColor primary)
    {
        return tintColor(primary, 70);
    }

    /**
     * {@code $btn-link-focus-shadow-rgb}: {@code to-rgb(mix(color-contrast($link-color), $link-color, 15%))}
     */
    static int[] linkButtonFocusShadowRgb(RgbColor primary)
    {
        return BootstrapColorFunctions.toRgb(mix(colorContrast(primary), primary, BTN_FOCUS_SHADOW_MIX));
    }

    /**
     * {@code color-contrast($value)}: the text colour on top of the theme colour ({@code .text-bg-<name>})
     */
    static RgbColor textOn(RgbColor themeColor)
    {
        return colorContrast(themeColor);
    }
}
