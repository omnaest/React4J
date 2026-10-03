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

import java.util.regex.Pattern;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;

/**
 * An opaque sRGB colour with channels in the range 0 to 255. The channels are doubles because Bootstrap's colour functions (mix, tint, shade) produce
 * fractional channels that are NOT rounded; the only way in is {@link #parseHex(String)}, which accepts {@code #rgb} and {@code #rrggbb} and nothing
 * else, so a colour can never carry anything but three numbers into the stylesheet.
 *
 * @author omnaest
 */
@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class RgbColor
{
    public static final RgbColor WHITE   = new RgbColor(255, 255, 255);
    public static final RgbColor BLACK   = new RgbColor(0, 0, 0);

    private static final Pattern HEX     = Pattern.compile("#([0-9a-fA-F]{3}|[0-9a-fA-F]{6})");
    private static final int     HEX_RGB = 3;
    private static final double  EPSILON = 1e-11;

    double                       red;
    double                       green;
    double                       blue;

    /**
     * @param hex
     *            {@code #rgb} or {@code #rrggbb}, case insensitive
     * @return the colour
     * @throws IllegalArgumentException
     *             for anything else, including null
     */
    public static RgbColor parseHex(String hex)
    {
        if (hex == null || !HEX.matcher(hex)
                               .matches())
        {
            throw new IllegalArgumentException("A color must be written as #rgb or #rrggbb");
        }
        String digits = hex.substring(1);
        if (digits.length() == HEX_RGB)
        {
            digits = "" + digits.charAt(0) + digits.charAt(0) + digits.charAt(1) + digits.charAt(1) + digits.charAt(2) + digits.charAt(2);
        }
        return new RgbColor(Integer.parseInt(digits.substring(0, 2), 16), Integer.parseInt(digits.substring(2, 4), 16), Integer.parseInt(digits.substring(4, 6), 16));
    }

    /**
     * @return a colour with exactly these (possibly fractional) channels
     */
    public static RgbColor of(double red, double green, double blue)
    {
        return new RgbColor(red, green, blue);
    }

    /**
     * Sass' {@code round()}: half away from zero, with Sass' fuzzy comparison at the .5 boundary
     */
    public static int sassRound(double value)
    {
        double floor = Math.floor(value);
        return (int) (value - floor < 0.5 - EPSILON ? floor : floor + 1);
    }

    public int roundedRed()
    {
        return sassRound(this.red);
    }

    public int roundedGreen()
    {
        return sassRound(this.green);
    }

    public int roundedBlue()
    {
        return sassRound(this.blue);
    }

    /**
     * @return true if all three channels are (fuzzy) integers, which is the case for every colour a user can configure
     */
    public boolean hasIntegerChannels()
    {
        return isInteger(this.red) && isInteger(this.green) && isInteger(this.blue);
    }

    private static boolean isInteger(double channel)
    {
        return Math.abs(channel - Math.rint(channel)) < EPSILON;
    }
}
