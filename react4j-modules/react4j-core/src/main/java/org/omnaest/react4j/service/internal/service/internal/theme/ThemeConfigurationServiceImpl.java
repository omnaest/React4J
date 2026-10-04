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

import java.util.function.UnaryOperator;

import org.apache.commons.lang3.StringUtils;
import org.omnaest.react4j.domain.configuration.ThemeConfiguration;
import org.omnaest.react4j.service.internal.service.CssLength;
import org.omnaest.react4j.service.internal.service.FontFamily;
import org.omnaest.react4j.service.internal.service.RgbColor;
import org.omnaest.react4j.service.internal.service.ThemeColorRole;
import org.omnaest.react4j.service.internal.service.ThemeConfigurationService;
import org.omnaest.react4j.service.internal.service.ThemeSettings;
import org.omnaest.react4j.service.internal.service.ThemeTokens;
import org.springframework.stereotype.Service;

/**
 * The mutable, framework-boundary side of the theme configuration: every change replaces the held, immutable {@link ThemeSettings} snapshot, so
 * readers never see a half-applied change.
 *
 * @author omnaest
 */
@Service
public class ThemeConfigurationServiceImpl implements ThemeConfigurationService
{
    private volatile ThemeSettings settings = ThemeSettings.defaults();

    @Override
    public ThemeConfiguration useDefault()
    {
        return this.update(current -> current.toBuilder()
                                             .enabled(true)
                                             .preset(ThemePreset.MODERN)
                                             .build());
    }

    @Override
    public ThemeConfiguration disable()
    {
        return this.update(current -> current.toBuilder()
                                             .enabled(false)
                                             .build());
    }

    @Override
    public ThemeConfiguration preset(ThemePreset preset)
    {
        if (preset == null)
        {
            throw new IllegalArgumentException("The theme preset must not be null");
        }
        return this.update(current -> current.toBuilder()
                                             .preset(preset)
                                             .build());
    }

    @Override
    public ThemeConfiguration colorMode(ColorMode colorMode)
    {
        if (colorMode == null)
        {
            throw new IllegalArgumentException("The color mode must not be null");
        }
        return this.update(current -> current.toBuilder()
                                             .colorMode(colorMode)
                                             .build());
    }

    @Override
    public ThemeConfiguration addStylesheet(String url)
    {
        if (StringUtils.isBlank(url))
        {
            throw new IllegalArgumentException("The stylesheet url must neither be null nor blank");
        }
        return this.update(current -> current.toBuilder()
                                             .stylesheet(url)
                                             .build());
    }

    @Override
    public ThemeConfiguration primaryColor(String color)
    {
        return this.color(ThemeColorRole.PRIMARY, color);
    }

    @Override
    public ThemeConfiguration secondaryColor(String color)
    {
        return this.color(ThemeColorRole.SECONDARY, color);
    }

    @Override
    public ThemeConfiguration successColor(String color)
    {
        return this.color(ThemeColorRole.SUCCESS, color);
    }

    @Override
    public ThemeConfiguration infoColor(String color)
    {
        return this.color(ThemeColorRole.INFO, color);
    }

    @Override
    public ThemeConfiguration warningColor(String color)
    {
        return this.color(ThemeColorRole.WARNING, color);
    }

    @Override
    public ThemeConfiguration dangerColor(String color)
    {
        return this.color(ThemeColorRole.DANGER, color);
    }

    @Override
    public ThemeConfiguration fontFamily(String fontFamily)
    {
        FontFamily validated = FontFamily.parse(fontFamily);
        return this.updateTokens(tokens -> tokens.withFontFamily(validated));
    }

    @Override
    public ThemeConfiguration baseFontSize(String fontSize)
    {
        CssLength validated = CssLength.parse(fontSize);
        return this.updateTokens(tokens -> tokens.withBaseFontSize(validated));
    }

    @Override
    public ThemeConfiguration borderRadius(String radius)
    {
        CssLength validated = CssLength.parse(radius);
        return this.updateTokens(tokens -> tokens.withBorderRadius(validated));
    }

    @Override
    public ThemeConfiguration shadowStrength(ShadowStrength shadowStrength)
    {
        if (shadowStrength == null)
        {
            throw new IllegalArgumentException("The shadow strength must not be null");
        }
        return this.updateTokens(tokens -> tokens.withShadowStrength(shadowStrength));
    }

    @Override
    public ThemeSettings getSettings()
    {
        return this.settings;
    }

    /**
     * The value is parsed BEFORE the state is touched, so an invalid value leaves the configuration unchanged
     */
    private ThemeConfiguration color(ThemeColorRole role, String color)
    {
        RgbColor validated = RgbColor.parseHex(color);
        return this.updateTokens(tokens -> tokens.withColor(role, validated));
    }

    private ThemeConfiguration updateTokens(UnaryOperator<ThemeTokens> change)
    {
        return this.update(current -> current.toBuilder()
                                             .tokens(change.apply(current.getTokens()))
                                             .build());
    }

    private synchronized ThemeConfiguration update(UnaryOperator<ThemeSettings> change)
    {
        this.settings = change.apply(this.settings);
        return this;
    }
}
