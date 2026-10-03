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
     * Selects the built-in modern Bootstrap stylesheet. This is the default, so the call is only needed to enable the theme again after a
     * {@link #disable()}. The {@link #colorMode(ColorMode)} and the stylesheets added by {@link #addStylesheet(String)} are preserved.
     *
     * @return this
     */
    public ThemeConfiguration useDefault();

    /**
     * Disables the theming: only the stock Bootstrap stylesheet is linked, exactly like before the theme existed. In this state the
     * {@link #colorMode(ColorMode)} is ignored (no {@code data-bs-theme} attribute is written) and stylesheets added by
     * {@link #addStylesheet(String)} are not emitted. {@link #useDefault()} reverts this.
     *
     * @return this
     */
    public ThemeConfiguration disable();

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
     * Sets the font family of the page text (Bootstrap's {@code --bs-body-font-family}).
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
     * Sets the font size of the page text (Bootstrap's {@code --bs-body-font-size}). Heading sizes are compiled by Bootstrap and are not rescaled.
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
