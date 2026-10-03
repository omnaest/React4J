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
import java.math.RoundingMode;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.omnaest.react4j.service.internal.service.RgbColor;

/**
 * Writes colours and numbers the way Sass does, so that the stylesheet this module emits reads like the compiled one: integer colours as (short) hex,
 * fractional colours as {@code rgb()} percentages with up to 10 fraction digits, {@code to-rgb} triplets as three rounded integers. Every method
 * produces only digits, letters and the punctuation of the CSS color syntax, never anything a caller controls.
 *
 * @author omnaest
 */
final class CssText
{
    private static final int FRACTION_DIGITS = 10;
    private static final int MAX_CHANNEL     = 255;
    private static final int PERCENT         = 100;

    private CssText()
    {
    }

    /**
     * @return {@code #4f46e5} (or {@code #fff}) for a colour with integer channels, {@code rgb(26.3333333333%, 23.3333333333%, 76.3333333333%)} for
     *         a mixed one
     */
    static String color(RgbColor color)
    {
        if (color.hasIntegerChannels())
        {
            String hex = hex(color);
            boolean shortenable = hex.charAt(0) == hex.charAt(1) && hex.charAt(2) == hex.charAt(3) && hex.charAt(4) == hex.charAt(5);
            return shortenable ? "#" + hex.charAt(0) + hex.charAt(2) + hex.charAt(4) : "#" + hex;
        }
        return "rgb(" + percentages(color) + ")";
    }

    /**
     * @return {@code 79, 70, 229}, the value of Bootstrap's {@code --bs-<name>-rgb} variables
     */
    static String triplet(int[] rgb)
    {
        return Stream.of(rgb[0], rgb[1], rgb[2])
                     .map(String::valueOf)
                     .collect(Collectors.joining(", "));
    }

    /**
     * @return {@code rgba(79, 70, 229, 0.4)}
     */
    static String rgba(RgbColor color, String alpha)
    {
        return "rgba(" + triplet(BootstrapColorFunctions.toRgb(color)) + ", " + alpha + ")";
    }

    /**
     * @return the colour as Bootstrap's {@code escape-svg} leaves it inside a data URI: {@code %23198754}, or for a mixed colour
     *         {@code rgb%2812.39%, 10.98%, 35.92%%29}
     */
    static String svgPaint(RgbColor color)
    {
        if (color.hasIntegerChannels())
        {
            return "%23" + hex(color);
        }
        return "rgb%28" + percentages(color) + "%29";
    }

    /**
     * @return the number with up to 10 fraction digits and without trailing zeros, like Sass prints it
     */
    static String number(double value)
    {
        return BigDecimal.valueOf(value)
                         .setScale(FRACTION_DIGITS, RoundingMode.HALF_UP)
                         .stripTrailingZeros()
                         .toPlainString();
    }

    private static String hex(RgbColor color)
    {
        return String.format("%02x%02x%02x", color.roundedRed(), color.roundedGreen(), color.roundedBlue());
    }

    private static String percentages(RgbColor color)
    {
        return Stream.of(color.getRed(), color.getGreen(), color.getBlue())
                     .map(channel -> number(channel / MAX_CHANNEL * PERCENT) + "%")
                     .collect(Collectors.joining(", "));
    }
}
