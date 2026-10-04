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
package org.omnaest.react4j.service.internal.service;

import java.util.List;

import org.omnaest.react4j.domain.configuration.ThemeConfiguration.ColorMode;
import org.omnaest.react4j.domain.configuration.ThemeConfiguration.ThemePreset;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

/**
 * An immutable snapshot of the {@link org.omnaest.react4j.domain.configuration.ThemeConfiguration} state, as read by the index.html rendering.
 * The default instance is: theme enabled, {@link ThemePreset#MODERN}, {@link ColorMode#LIGHT}, no added stylesheets.
 *
 * @see ThemeConfigurationService#getSettings()
 * @author omnaest
 */
@Value
@Builder(toBuilder = true)
public class ThemeSettings
{
    /**
     * false means stock Bootstrap only
     */
    @Builder.Default
    boolean      enabled   = true;

    /**
     * Which built-in stylesheet is linked. Ignored while not {@link #isEnabled()}.
     */
    @Builder.Default
    ThemePreset  preset    = ThemePreset.MODERN;

    @Builder.Default
    ColorMode    colorMode = ColorMode.LIGHT;

    /**
     * In insertion order
     */
    @Singular
    List<String> stylesheets;

    /**
     * The design tokens, none set by default. Only applied while {@link #isEnabled()}.
     */
    @Builder.Default
    ThemeTokens  tokens    = ThemeTokens.none();

    public static ThemeSettings defaults()
    {
        return ThemeSettings.builder()
                            .build();
    }
}
