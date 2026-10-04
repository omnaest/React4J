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

import org.omnaest.react4j.domain.configuration.ThemeConfiguration.ColorMode;
import org.omnaest.react4j.domain.configuration.ThemeConfiguration.ThemePreset;
import org.omnaest.react4j.service.internal.service.ThemeSettings;

/**
 * Renders a {@link ThemeSettings} snapshot into the index.html markup: the head slot (stylesheet links, colour mode script) and the
 * {@code data-bs-theme} attribute of the {@code html} element. Pure function, no state, no Spring.
 * <br>
 * <br>
 * Head order of the modern theme: (colour mode script for {@link ColorMode#AUTO}) - modern stylesheet link - (design token style block, only if a token is set)
 * - added stylesheets. The {@link ThemePreset#TABLER} preset has the same order with the Tabler stylesheet link and, instead of the {@link ThemeTokenCss}
 * block, the one of {@link TablerTokenCss} (base custom properties only). A disabled theme renders the stock Bootstrap link and nothing else, whatever
 * the preset.
 *
 * @author omnaest
 */
public final class ThemeHeadRenderer
{
    public static final String  MODERN_STYLESHEET_PATH = "/css/theme/react4j-modern.css";
    public static final String  TABLER_STYLESHEET_PATH = "/css/theme/react4j-tabler.css";
    public static final String  STOCK_STYLESHEET_PATH  = "/css/theme/bootstrap.min.css";

    private static final String COLOR_MODE_ATTRIBUTE   = "data-bs-theme";
    private static final String AUTO_COLOR_MODE_SCRIPT = "<script>(function(){var m=window.matchMedia&&window.matchMedia(\"(prefers-color-scheme: dark)\");"
                                                         + "var s=function(){document.documentElement.setAttribute(\"" + COLOR_MODE_ATTRIBUTE + "\",m&&m.matches?\"dark\":\"light\");};"
                                                         + "s();if(m&&m.addEventListener){m.addEventListener(\"change\",s);}})();</script>";

    private ThemeHeadRenderer()
    {
    }

    /**
     * @param settings
     * @param cacheBuster
     *            the value appended as query string to the theme stylesheet links of the theme itself, equal to the one the application stylesheet links
     *            carry. The stylesheets added by the application are emitted as given.
     * @return
     */
    public static ThemeHead render(ThemeSettings settings, String cacheBuster)
    {
        if (!settings.isEnabled())
        {
            return new ThemeHead(stylesheetLink(STOCK_STYLESHEET_PATH + "?" + cacheBuster), "");
        }

        List<String> markup = new ArrayList<>();
        if (settings.getColorMode() == ColorMode.AUTO)
        {
            markup.add(AUTO_COLOR_MODE_SCRIPT);
        }
        markup.add(stylesheetLink(presetStylesheetPath(settings.getPreset()) + "?" + cacheBuster));
        String tokenCss = presetTokenCss(settings);
        if (!tokenCss.isEmpty())
        {
            markup.add(styleBlock(tokenCss));
        }
        settings.getStylesheets()
                .stream()
                .map(ThemeHeadRenderer::stylesheetLink)
                .forEach(markup::add);

        return new ThemeHead(String.join("\n", markup), colorModeAttribute(settings.getColorMode()));
    }

    private static String presetStylesheetPath(ThemePreset preset)
    {
        switch (preset)
        {
            case TABLER :
                return TABLER_STYLESHEET_PATH;
            default :
                return MODERN_STYLESHEET_PATH;
        }
    }

    /**
     * The design token CSS of the preset: {@link TablerTokenCss} (base custom properties only) for {@link ThemePreset#TABLER}, {@link ThemeTokenCss}
     * (Bootstrap component overrides, which must never apply to Tabler) for the others. Empty if the preset's writer has nothing to write.
     */
    private static String presetTokenCss(ThemeSettings settings)
    {
        switch (settings.getPreset())
        {
            case TABLER :
                return TablerTokenCss.render(settings.getTokens());
            default :
                return ThemeTokenCss.render(settings.getTokens());
        }
    }

    /**
     * Defence in depth: the CSS text is built from validated value types and constants only, so it can never contain an angle bracket. If it ever
     * did, the page must not be served rather than allow the text to end the style element.
     */
    static String styleBlock(String css)
    {
        if (css.indexOf('<') >= 0)
        {
            throw new IllegalStateException("The design token stylesheet must never contain an angle bracket");
        }
        return "<style>" + css + "</style>";
    }

    private static String colorModeAttribute(ColorMode colorMode)
    {
        switch (colorMode)
        {
            case LIGHT :
                return COLOR_MODE_ATTRIBUTE + "=\"light\"";
            case DARK :
                return COLOR_MODE_ATTRIBUTE + "=\"dark\"";
            default :
                return "";
        }
    }

    private static String stylesheetLink(String url)
    {
        return "<link rel=\"stylesheet\" href=\"" + escapeAttribute(url) + "\"/>";
    }

    private static String escapeAttribute(String value)
    {
        return value.replace("&", "&amp;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#39;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;");
    }
}
