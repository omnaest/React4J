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

import org.omnaest.react4j.service.internal.service.RgbColor;

/**
 * A Java mirror of the colour functions of Bootstrap 5.3.2's {@code scss/_functions.scss}: {@code mix}, {@code tint-color}, {@code shade-color},
 * {@code shift-color}, {@code luminance}, {@code contrast-ratio}, {@code color-contrast} and {@code to-rgb}. Pure and without state.
 * <br>
 * <br>
 * Exactness is the point of this class. Sass (1.105, the compiler of the theme) does not round the channels of a mixed colour, so neither does
 * {@link #mix}; the places where Bootstrap does round (the channel read by {@code luminance}, {@code to-rgb}) round with Sass' own rule. The golden
 * tests compare every function against values produced by Bootstrap's Sass itself.
 *
 * @author omnaest
 */
final class BootstrapColorFunctions
{
    /**
     * {@code $min-contrast-ratio}: Bootstrap's default, deliberately not overridden by the react4j-modern theme
     */
    static final double           MIN_CONTRAST_RATIO = 4.5;

    /**
     * {@code $color-contrast-light} (white) and {@code $color-contrast-dark} (black): Bootstrap's defaults, deliberately not overridden by the theme
     */
    static final RgbColor         CONTRAST_LIGHT     = RgbColor.WHITE;
    static final RgbColor         CONTRAST_DARK      = RgbColor.BLACK;

    private static final int      DIVIDE_PRECISION   = 10;
    private static final double   LOW_THRESHOLD      = 0.04045;
    private static final double   LOW_DIVISOR        = 12.92;
    private static final int      MAX_CHANNEL        = 255;

    /**
     * {@code $_luminance-list}: the tabulated linear-light value of every channel value 0..255, copied from _functions.scss
     */
    private static final double[] LUMINANCE_LIST     = {
            0.0008, 0.001, 0.0011, 0.0013, 0.0015, 0.0017, 0.002, 0.0022,
            0.0025, 0.0027, 0.003, 0.0033, 0.0037, 0.004, 0.0044, 0.0048,
            0.0052, 0.0056, 0.006, 0.0065, 0.007, 0.0075, 0.008, 0.0086,
            0.0091, 0.0097, 0.0103, 0.011, 0.0116, 0.0123, 0.013, 0.0137,
            0.0144, 0.0152, 0.016, 0.0168, 0.0176, 0.0185, 0.0194, 0.0203,
            0.0212, 0.0222, 0.0232, 0.0242, 0.0252, 0.0262, 0.0273, 0.0284,
            0.0296, 0.0307, 0.0319, 0.0331, 0.0343, 0.0356, 0.0369, 0.0382,
            0.0395, 0.0409, 0.0423, 0.0437, 0.0452, 0.0467, 0.0482, 0.0497,
            0.0513, 0.0529, 0.0545, 0.0561, 0.0578, 0.0595, 0.0612, 0.063,
            0.0648, 0.0666, 0.0685, 0.0704, 0.0723, 0.0742, 0.0762, 0.0782,
            0.0802, 0.0823, 0.0844, 0.0865, 0.0887, 0.0908, 0.0931, 0.0953,
            0.0976, 0.0999, 0.1022, 0.1046, 0.107, 0.1095, 0.1119, 0.1144,
            0.117, 0.1195, 0.1221, 0.1248, 0.1274, 0.1301, 0.1329, 0.1356,
            0.1384, 0.1413, 0.1441, 0.147, 0.15, 0.1529, 0.1559, 0.159,
            0.162, 0.1651, 0.1683, 0.1714, 0.1746, 0.1779, 0.1812, 0.1845,
            0.1878, 0.1912, 0.1946, 0.1981, 0.2016, 0.2051, 0.2086, 0.2122,
            0.2159, 0.2195, 0.2232, 0.227, 0.2307, 0.2346, 0.2384, 0.2423,
            0.2462, 0.2502, 0.2542, 0.2582, 0.2623, 0.2664, 0.2705, 0.2747,
            0.2789, 0.2831, 0.2874, 0.2918, 0.2961, 0.3005, 0.305, 0.3095,
            0.314, 0.3185, 0.3231, 0.3278, 0.3325, 0.3372, 0.3419, 0.3467,
            0.3515, 0.3564, 0.3613, 0.3663, 0.3712, 0.3763, 0.3813, 0.3864,
            0.3916, 0.3968, 0.402, 0.4072, 0.4125, 0.4179, 0.4233, 0.4287,
            0.4342, 0.4397, 0.4452, 0.4508, 0.4564, 0.4621, 0.4678, 0.4735,
            0.4793, 0.4851, 0.491, 0.4969, 0.5029, 0.5089, 0.5149, 0.521,
            0.5271, 0.5333, 0.5395, 0.5457, 0.552, 0.5583, 0.5647, 0.5711,
            0.5776, 0.5841, 0.5906, 0.5972, 0.6038, 0.6105, 0.6172, 0.624,
            0.6308, 0.6376, 0.6445, 0.6514, 0.6584, 0.6654, 0.6724, 0.6795,
            0.6867, 0.6939, 0.7011, 0.7084, 0.7157, 0.7231, 0.7305, 0.7379,
            0.7454, 0.7529, 0.7605, 0.7682, 0.7758, 0.7835, 0.7913, 0.7991,
            0.807, 0.8148, 0.8228, 0.8308, 0.8388, 0.8469, 0.855, 0.8632,
            0.8714, 0.8796, 0.8879, 0.8963, 0.9047, 0.9131, 0.9216, 0.9301,
            0.9387, 0.9473, 0.956, 0.9647, 0.9734, 0.9823, 0.9911, 1};

    private BootstrapColorFunctions()
    {
    }

    /**
     * Sass' {@code mix($color1, $color2, $weight)} for two opaque colours: {@code weightPercent} percent of the first colour. Channels stay fractional.
     */
    static RgbColor mix(RgbColor first, RgbColor second, double weightPercent)
    {
        double weightScale = weightPercent / 100;
        double normalizedWeight = weightScale * 2 - 1;
        double alphaDistance = 0; // both colours are opaque
        double combinedWeight1 = (normalizedWeight + alphaDistance) / (1 + normalizedWeight * alphaDistance);
        double weight1 = (combinedWeight1 + 1) / 2;
        double weight2 = 1 - weight1;
        return RgbColor.of(first.getRed() * weight1 + second.getRed() * weight2, first.getGreen() * weight1 + second.getGreen() * weight2,
                           first.getBlue() * weight1 + second.getBlue() * weight2);
    }

    /**
     * {@code tint-color($color, $weight)}: mixes white into the colour
     */
    static RgbColor tintColor(RgbColor color, double weightPercent)
    {
        return mix(RgbColor.WHITE, color, weightPercent);
    }

    /**
     * {@code shade-color($color, $weight)}: mixes black into the colour
     */
    static RgbColor shadeColor(RgbColor color, double weightPercent)
    {
        return mix(RgbColor.BLACK, color, weightPercent);
    }

    /**
     * {@code shift-color($color, $weight)}: a positive weight shades, a negative one tints
     */
    static RgbColor shiftColor(RgbColor color, double weightPercent)
    {
        return weightPercent > 0 ? shadeColor(color, weightPercent) : tintColor(color, -weightPercent);
    }

    /**
     * {@code luminance($color)}: relative luminance with Bootstrap's tabulated linearisation. The channels are read as integers like Sass'
     * {@code red()}, {@code green()} and {@code blue()} do.
     */
    static double luminance(RgbColor color)
    {
        return linear(color.roundedRed()) * 0.2126 + linear(color.roundedGreen()) * 0.7152 + linear(color.roundedBlue()) * 0.0722;
    }

    /**
     * {@code contrast-ratio($background, $foreground)} for an opaque foreground, including Bootstrap's own 10 digit {@code divide()}
     */
    static double contrastRatio(RgbColor background, RgbColor foreground)
    {
        double backgroundLuminance = luminance(background);
        double foregroundLuminance = luminance(foreground);
        return backgroundLuminance > foregroundLuminance ? divide(backgroundLuminance + .05, foregroundLuminance + .05)
                : divide(foregroundLuminance + .05, backgroundLuminance + .05);
    }

    /**
     * {@code color-contrast($background)}: the first of white, black that reaches the minimum contrast ratio, otherwise the one with the highest ratio
     */
    static RgbColor colorContrast(RgbColor background)
    {
        double maxRatio = 0;
        RgbColor maxRatioColor = null;
        for (RgbColor foreground : new RgbColor[] {CONTRAST_LIGHT, CONTRAST_DARK, RgbColor.WHITE, RgbColor.BLACK})
        {
            double ratio = contrastRatio(background, foreground);
            if (ratio > MIN_CONTRAST_RATIO)
            {
                return foreground;
            }
            else if (ratio > maxRatio)
            {
                maxRatio = ratio;
                maxRatioColor = foreground;
            }
        }
        return maxRatioColor;
    }

    /**
     * {@code to-rgb($value)}: the three channels rounded to integers
     */
    static int[] toRgb(RgbColor color)
    {
        return new int[] {color.roundedRed(), color.roundedGreen(), color.roundedBlue()};
    }

    private static double linear(int channel)
    {
        double scaled = divide(channel, MAX_CHANNEL);
        return scaled < LOW_THRESHOLD ? divide(scaled, LOW_DIVISOR) : LUMINANCE_LIST[channel];
    }

    /**
     * Bootstrap's {@code divide()} computes to 10 fraction digits and rounds half up
     */
    private static double divide(double dividend, double divisor)
    {
        return BigDecimal.valueOf(dividend)
                         .divide(BigDecimal.valueOf(divisor), DIVIDE_PRECISION, RoundingMode.HALF_UP)
                         .doubleValue();
    }
}
