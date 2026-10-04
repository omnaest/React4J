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
package org.omnaest.react4j.domain.configuration;

/**
 * Configures the visual theme of the rendered page, i.e. which Bootstrap stylesheet the server links into the index.html.
 * <br>
 * <br>
 * The theme stylesheet is always linked <b>before</b> the application stylesheets ({@code /css/color.css}, {@code /css/print.css},
 * {@code /css/custom.css}), so equal-specificity rules of the application win against the theme.
 * <br>
 * <br>
 * Without any configuration the built-in modern theme is active, in {@link ColorMode#LIGHT}, without added stylesheets.
 *
 * @see org.omnaest.react4j.domain.ReactUI#configureTheme(java.util.function.Consumer)
 * @see org.omnaest.react4j.service.ReactUIService#configureTheme(java.util.function.Consumer)
 * @author omnaest
 */
public interface ThemeConfiguration
{
    /**
     * Enables the theming again and selects the built-in modern theme, i.e. {@link ThemePreset#MODERN}. This is the default, so the call is only
     * needed to enable the theme again after a {@link #disable()}, or to leave another {@link #preset(ThemePreset) preset}. The
     * {@link #colorMode(ColorMode)}, the stylesheets added by {@link #addStylesheet(String)} and the design tokens are preserved.
     * <br>
     * <br>
     * Because this selects {@link ThemePreset#MODERN}, a previously chosen {@link ThemePreset#TABLER} is dropped: to get Tabler back after a
     * {@link #disable()}, call {@code useDefault().preset(ThemePreset.TABLER)}.
     *
     * @return this
     */
    public ThemeConfiguration useDefault();

    /**
     * Disables the theming: only the stock Bootstrap stylesheet is linked, exactly like before the theme existed. In this state the
     * {@link #preset(ThemePreset) preset} and the {@link #colorMode(ColorMode)} are ignored (no {@code data-bs-theme} attribute is written) and
     * stylesheets added by {@link #addStylesheet(String)} are not emitted.
     * <br>
     * <br>
     * Disabling only switches the theming off: the selected preset, the colour mode, the added stylesheets and the design tokens are all kept, but
     * {@link #useDefault()}, which reverts this, selects {@link ThemePreset#MODERN} again.
     *
     * @return this
     */
    public ThemeConfiguration disable();

    /**
     * Selects the built-in theme preset whose stylesheet is linked. {@link ThemePreset#MODERN} is the default.
     * <br>
     * <br>
     * Choosing a preset never enables or disables the theming: while the theme is {@link #disable() disabled} the preset is ignored and only the stock
     * Bootstrap stylesheet is linked. {@link #disable()} keeps the preset, but {@link #useDefault()} selects {@link ThemePreset#MODERN}, so to get
     * {@link ThemePreset#TABLER} back after a {@link #disable()} call {@code useDefault().preset(ThemePreset.TABLER)}.
     * <br>
     * <br>
     * The {@link #colorMode(ColorMode)} and the stylesheets added by {@link #addStylesheet(String)} work the same for every preset.
     *
     * @param preset
     *            must not be null
     * @return this
     * @throws IllegalArgumentException
     *             if the {@link ThemePreset} is null; the configuration is left unchanged
     */
    public ThemeConfiguration preset(ThemePreset preset);

    /**
     * Sets the colour mode of the modern theme. {@link ColorMode#LIGHT} is the default.
     *
     * @param colorMode
     *            must not be null
     * @return this
     * @throws IllegalArgumentException
     *             if the {@link ColorMode} is null
     */
    public ThemeConfiguration colorMode(ColorMode colorMode);

    /**
     * Adds a stylesheet that is linked right after the theme stylesheet, in insertion order. The url is emitted as given (HTML attribute
     * escaped) and, in contrast to the theme stylesheets, without any cache busting parameter. Added stylesheets are only emitted while the theme is not
     * {@link #disable() disabled}.
     *
     * @param url
     *            must neither be null nor blank
     * @return this
     * @throws IllegalArgumentException
     *             if the url is null or blank
     */
    public ThemeConfiguration addStylesheet(String url);

    /**
     * Sets the primary colour of the modern theme, which drives buttons, links, form focus and checked states, pagination, progress bars, active list
     * items and the other places Bootstrap paints with the accent. All shades Bootstrap derives from it (hover and active states, the text on top of
     * it, the subtle variants for light and dark mode) are derived from it in Java with the exact formulas of Bootstrap's Sass.
     * <br>
     * <br>
     * <b>Design tokens</b> (this and the other token methods below) only take effect while the theme is enabled, are preserved by {@link #disable()}
     * and {@link #useDefault()}, and are emitted as one {@code <style>} block directly after the theme stylesheet link. Without any token the page is
     * exactly as without this feature. Every value is validated here; an invalid value throws an {@link IllegalArgumentException} and leaves the
     * configuration unchanged, so no value can ever break out of the stylesheet.
     * <br>
     * <br>
     * <b>The tokens work differently per {@link #preset(ThemePreset) preset}.</b> The description of each token method is the one of
     * {@link ThemePreset#MODERN}, whose compiled stylesheet bakes colours into component rules, so the block re-emits those rules. On
     * {@link ThemePreset#TABLER} the block holds <b>base custom properties only</b>, in one rule that is placed after the Tabler stylesheet and wins in
     * light and dark mode alike, and never a component rule such as {@code .btn-primary}: Tabler derives its hover and active shades, its subtle
     * variants and its focus rings from the base variables in the browser. Per token, on {@link ThemePreset#TABLER}:
     * <ul>
     * <li>A colour sets {@code --bs-<role>} and its {@code --bs-<role>-rgb} triplet and nothing else. The text colour on top of a role colour
     * ({@code --bs-<role>-fg}) is <b>not</b> derived and stays Tabler's light text, so choose role colours dark enough for light text.</li>
     * <li>{@link #fontFamily(String)} sets {@code --bs-font-sans-serif}, which the body font family follows, and {@link #baseFontSize(String)} sets
     * {@code --bs-body-font-size}.</li>
     * <li>{@link #borderRadius(String)} sets Tabler's base radius to the value and scales the other sizes with Tabler's own ratios.</li>
     * <li>{@link #shadowStrength(ShadowStrength)} scales the alpha of Tabler's shadow colour, see there.</li>
     * </ul>
     *
     * @param color
     *            {@code #rgb} or {@code #rrggbb}, nothing else (no names, no functions, no alpha)
     * @return this
     * @throws IllegalArgumentException
     *             if the value is null or not a hex colour
     */
    public ThemeConfiguration primaryColor(String color);

    /**
     * Sets the secondary colour, see {@link #primaryColor(String)} for the rules.
     *
     * @param color
     *            {@code #rgb} or {@code #rrggbb}
     * @return this
     * @throws IllegalArgumentException
     *             if the value is null or not a hex colour
     */
    public ThemeConfiguration secondaryColor(String color);

    /**
     * Sets the success colour, see {@link #primaryColor(String)} for the rules.
     *
     * @param color
     *            {@code #rgb} or {@code #rrggbb}
     * @return this
     * @throws IllegalArgumentException
     *             if the value is null or not a hex colour
     */
    public ThemeConfiguration successColor(String color);

    /**
     * Sets the info colour, see {@link #primaryColor(String)} for the rules.
     *
     * @param color
     *            {@code #rgb} or {@code #rrggbb}
     * @return this
     * @throws IllegalArgumentException
     *             if the value is null or not a hex colour
     */
    public ThemeConfiguration infoColor(String color);

    /**
     * Sets the warning colour, see {@link #primaryColor(String)} for the rules.
     *
     * @param color
     *            {@code #rgb} or {@code #rrggbb}
     * @return this
     * @throws IllegalArgumentException
     *             if the value is null or not a hex colour
     */
    public ThemeConfiguration warningColor(String color);

    /**
     * Sets the danger colour, see {@link #primaryColor(String)} for the rules.
     *
     * @param color
     *            {@code #rgb} or {@code #rrggbb}
     * @return this
     * @throws IllegalArgumentException
     *             if the value is null or not a hex colour
     */
    public ThemeConfiguration dangerColor(String color);

    /**
     * Sets the font family of the page text (Bootstrap's {@code --bs-body-font-family}). On {@link ThemePreset#TABLER} it sets Tabler's
     * {@code --bs-font-sans-serif}, which {@code --bs-body-font-family} follows.
     *
     * @param fontFamily
     *            a comma separated list of font names, each a bare name of letters, digits, hyphens and single spaces, or such a name in matching
     *            quotes, e.g. {@code Inter, "Segoe UI", system-ui, sans-serif}. Nothing else is accepted: no backslash, semicolon, brace, angle
     *            bracket, slash or parenthesis.
     * @return this
     * @throws IllegalArgumentException
     *             if the value is null, blank or outside that grammar
     * @see #primaryColor(String) for the rules all tokens share
     */
    public ThemeConfiguration fontFamily(String fontFamily);

    /**
     * Sets the font size of the page text (Bootstrap's {@code --bs-body-font-size}, also on {@link ThemePreset#TABLER}). Heading sizes are compiled by
     * Bootstrap and are not rescaled.
     *
     * @param fontSize
     *            a number directly followed by {@code px} or {@code rem}, e.g. {@code 16px} or {@code 1.125rem}
     * @return this
     * @throws IllegalArgumentException
     *             if the value is null or not such a length
     * @see #primaryColor(String) for the rules all tokens share
     */
    public ThemeConfiguration baseFontSize(String fontSize);

    /**
     * Sets the base corner radius. The small, large and extra large radii of the theme are scaled with the ratios of the modern theme (0.75, 1.5 and
     * 2 times the base).
     * <br>
     * <br>
     * On {@link ThemePreset#TABLER} the value becomes Tabler's base radius (the one {@code --bs-border-radius} follows) and the other sizes Tabler
     * reads are scaled with Tabler's own ratios to it: 2/3 (small), 4/3 (large), 8/3 (extra large) and 16/3 (extra extra large) of the base, in the
     * unit given, so {@code 12px} gives 8px, 16px, 32px and 64px.
     *
     * @param radius
     *            a number directly followed by {@code px} or {@code rem}, e.g. {@code 12px} or {@code 0.5rem}
     * @return this
     * @throws IllegalArgumentException
     *             if the value is null or not such a length
     * @see #primaryColor(String) for the rules all tokens share
     */
    public ThemeConfiguration borderRadius(String radius);

    /**
     * Sets how strong the shadows of cards, dropdowns, modals and similar components are.
     * <br>
     * <br>
     * On {@link ThemePreset#TABLER} the strength scales the alpha of Tabler's one shadow colour, from which every elevation shadow derives:
     * {@link ShadowStrength#NONE} removes the shadows, {@link ShadowStrength#SUBTLE} halves and {@link ShadowStrength#STRONG} doubles their strength,
     * {@link ShadowStrength#DEFAULT} is Tabler as compiled and writes nothing. Borders and focus rings are never touched, including the outline of
     * mentions and avatars, which Tabler draws with a shadow and which is therefore kept as it is. In dark mode Tabler's shadow colour is already opaque
     * black, so {@link ShadowStrength#STRONG} cannot be stronger than the default there.
     *
     * @param shadowStrength
     *            must not be null
     * @return this
     * @throws IllegalArgumentException
     *             if the value is null
     * @see #primaryColor(String) for the rules all tokens share
     */
    public ThemeConfiguration shadowStrength(ShadowStrength shadowStrength);

    /**
     * The strength of the component shadows, see {@link #shadowStrength(ShadowStrength)}
     */
    public static enum ShadowStrength
    {
        /**
         * No shadows at all
         */
        NONE,
        /**
         * Half the strength of the modern theme's default
         */
        SUBTLE,
        /**
         * The shadows of the modern theme as compiled
         */
        DEFAULT,
        /**
         * Twice the strength of the modern theme's default
         */
        STRONG
    }

    /**
     * The built-in theme presets, see {@link #preset(ThemePreset)}
     */
    public static enum ThemePreset
    {
        /**
         * The modern Bootstrap 5.3 theme (accent {@code #4f46e5}, radius 8px) compiled from Sass. This is the default.
         */
        MODERN,
        /**
         * The Tabler theme: Tabler (https://tabler.io, {@code @tabler/core}, MIT licence, Copyright (c) 2018-2026 The Tabler Authors), compiled from
         * its Sass.
         * <br>
         * <br>
         * This is a replacement stylesheet, not a skin on top of the modern theme: the modern stylesheet is not linked while this preset is
         * selected. Only the CSS of Tabler is shipped, no Tabler JavaScript and none of its bundled third-party plugins.
         * <br>
         * <br>
         * Browser floor: Chrome 123+, Firefox 128+, Safari 17.5+, because Tabler derives its colour shades in the browser with {@code color-mix()},
         * {@code light-dark()} and {@code oklch()}, and its hover shade with relative colour syntax, which Firefox supports from 128.
         * <br>
         * <br>
         * Design tokens work on this preset by setting Tabler's base custom properties only, see
         * {@link ThemeConfiguration#primaryColor(String) the design tokens}.
         * <br>
         * <br>
         * The stylesheet is about 635 KB (about 80 KB gzipped) and is loaded only when this preset is chosen.
         */
        TABLER
    }

    /**
     * The colour mode of the modern theme, written as the {@code data-bs-theme} attribute of the {@code html} element
     */
    public static enum ColorMode
    {
        /**
         * Always light
         */
        LIGHT,
        /**
         * Always dark
         */
        DARK,
        /**
         * Follows the {@code prefers-color-scheme} media query of the browser: a small inline script sets the attribute in the browser, no
         * attribute is written by the server.
         */
        AUTO
    }
}
