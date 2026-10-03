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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Compares two CSS values the way a browser would see them: every colour (hex, {@code rgb()} with numbers or percentages, {@code rgba()}, also inside a
 * percent-encoded data URI) is read as three numeric channels and compared with a tolerance, and everything else must match as text. This is what lets
 * {@code #4f46e5}, {@code rgb(31%, 27%, 90%)} and Sass' ten-digit percentages compare as the same colour or as different ones.
 *
 * @author omnaest
 */
final class CssValues
{
    /**
     * Sass prints ten fraction digits of a percentage, which is about 2.5e-10 of a channel; anything beyond this is a different colour
     */
    static final double          TOLERANCE = 1e-6;

    private static final Pattern COLOR     = Pattern.compile("#[0-9a-fA-F]{6}(?![0-9a-fA-F])|#[0-9a-fA-F]{3}(?![0-9a-fA-F])|rgba?\\(([^)]*)\\)");
    private static final Pattern HEX_DIGIT = Pattern.compile("[0-9a-fA-F]+");

    private CssValues()
    {
    }

    /**
     * A value reduced to its text skeleton (colours replaced by {@code <C>}) and the channels of its colours in order of appearance
     */
    static final class Parsed
    {
        final String         skeleton;
        final List<double[]> colors;

        private Parsed(String skeleton, List<double[]> colors)
        {
            this.skeleton = skeleton;
            this.colors = colors;
        }
    }

    static boolean equivalent(String expected, String actual)
    {
        return describeDifference(expected, actual) == null;
    }

    /**
     * @return null if equivalent, else a one line description of the difference
     */
    static String describeDifference(String expected, String actual)
    {
        Parsed a = parse(expected);
        Parsed b = parse(actual);
        if (!a.skeleton.equals(b.skeleton))
        {
            return "text differs: expected '" + expected + "' but was '" + actual + "'";
        }
        for (int i = 0; i < a.colors.size(); i++)
        {
            for (int channel = 0; channel < 3; channel++)
            {
                if (Math.abs(a.colors.get(i)[channel] - b.colors.get(i)[channel]) > TOLERANCE)
                {
                    return "color " + i + " channel " + channel + " differs: expected '" + expected + "' but was '" + actual + "'";
                }
            }
        }
        return null;
    }

    static Parsed parse(String value)
    {
        String decoded = value.replace("%23", "#")
                              .replace("%28", "(")
                              .replace("%29", ")")
                              .replaceAll("(?<![-\\w])white(?![-\\w])", "#ffffff")
                              .replaceAll("(?<![-\\w])black(?![-\\w])", "#000000")
                              .replaceAll(",\\s+", ",")
                              .replaceAll("(?<![\\d.])0\\.", ".");
        Matcher matcher = COLOR.matcher(decoded);
        StringBuilder skeleton = new StringBuilder();
        List<double[]> colors = new ArrayList<>();
        int last = 0;
        while (matcher.find())
        {
            skeleton.append(decoded, last, matcher.start());
            String token = matcher.group();
            if (token.startsWith("#"))
            {
                colors.add(hex(token.substring(1)));
                skeleton.append("<C>");
            }
            else
            {
                String[] parts = matcher.group(1)
                                        .split(",");
                if (parts.length < 3 || !isNumeric(parts[0]) || !isNumeric(parts[1]) || !isNumeric(parts[2]))
                {
                    // e.g. rgba(var(--x), .5): not a literal colour, compared as text
                    skeleton.append(token);
                }
                else
                {
                    colors.add(new double[] {channel(parts[0]), channel(parts[1]), channel(parts[2])});
                    // the alpha, if any, is part of the text skeleton: it must match exactly
                    skeleton.append(token.toLowerCase()
                                         .startsWith("rgba")
                                    && parts.length > 3 ? "<C+A:" + parts[3].trim() + ">" : "<C>");
                }
            }
            last = matcher.end();
        }
        skeleton.append(decoded.substring(last));
        return new Parsed(skeleton.toString(), colors);
    }

    private static double[] hex(String digits)
    {
        if (!HEX_DIGIT.matcher(digits)
                      .matches())
        {
            throw new IllegalArgumentException("not hex: " + digits);
        }
        String full = digits.length() == 3 ? "" + digits.charAt(0) + digits.charAt(0) + digits.charAt(1) + digits.charAt(1) + digits.charAt(2) + digits.charAt(2)
                : digits;
        return new double[] {Integer.parseInt(full.substring(0, 2), 16), Integer.parseInt(full.substring(2, 4), 16), Integer.parseInt(full.substring(4, 6), 16)};
    }

    private static boolean isNumeric(String text)
    {
        return text.trim()
                   .matches("[0-9.]+%?");
    }

    private static double channel(String text)
    {
        String trimmed = text.trim();
        if (trimmed.endsWith("%"))
        {
            return Double.parseDouble(trimmed.substring(0, trimmed.length() - 1)) * 255 / 100;
        }
        return Double.parseDouble(trimmed);
    }
}
