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

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

import org.omnaest.react4j.domain.configuration.ThemeConfiguration.ShadowStrength;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.With;

/**
 * The immutable design tokens of a {@link ThemeSettings} snapshot. Every field is a typed, already validated value, so nothing that reaches the
 * stylesheet renderer is free text. A field that is null (or a role absent from {@link #getColors()}) is not set, and the compiled theme stylesheet
 * applies unchanged for it.
 *
 * @author omnaest
 */
@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ThemeTokens
{
    private static final ThemeTokens NONE = new ThemeTokens(Collections.emptyMap(), null, null, null, null);

    Map<ThemeColorRole, RgbColor>    colors;

    @With
    FontFamily                       fontFamily;

    @With
    CssLength                        baseFontSize;

    @With
    CssLength                        borderRadius;

    @With
    ShadowStrength                   shadowStrength;

    public static ThemeTokens none()
    {
        return NONE;
    }

    /**
     * @return true if no token is set at all, in which case nothing is emitted
     */
    public boolean isEmpty()
    {
        return this.colors.isEmpty() && this.fontFamily == null && this.baseFontSize == null && this.borderRadius == null && this.shadowStrength == null;
    }

    public ThemeTokens withColor(ThemeColorRole role, RgbColor color)
    {
        Map<ThemeColorRole, RgbColor> copy = new EnumMap<>(ThemeColorRole.class);
        copy.putAll(this.colors);
        copy.put(role, color);
        return new ThemeTokens(Collections.unmodifiableMap(copy), this.fontFamily, this.baseFontSize, this.borderRadius, this.shadowStrength);
    }
}
