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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Value;

/**
 * A non-negative length with the unit {@code px} or {@code rem}. The number is a {@link BigDecimal}, so scaling a radius by a ratio is exact and the
 * text written to the stylesheet carries no floating point noise. The only way in is {@link #parse(String)}.
 *
 * @author omnaest
 */
@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CssLength
{
    private static final Pattern LENGTH = Pattern.compile("(\\d{1,4}(?:\\.\\d{1,4})?|\\.\\d{1,4})(px|rem)");
    private static final int     SCALE  = 4;

    BigDecimal                   value;
    Unit                         unit;

    @AllArgsConstructor
    public static enum Unit
    {
        PX("px"), REM("rem");

        @Getter
        private final String suffix;
    }

    /**
     * @param text
     *            a number (up to 4 integer and 4 fraction digits) directly followed by {@code px} or {@code rem}
     * @return the length
     * @throws IllegalArgumentException
     *             for anything else, including null, a sign, a space, another unit or any trailing character
     */
    public static CssLength parse(String text)
    {
        Matcher matcher = text != null ? LENGTH.matcher(text) : null;
        if (matcher == null || !matcher.matches())
        {
            throw new IllegalArgumentException("A length must be a number followed by px or rem, e.g. 12px or 0.75rem");
        }
        return new CssLength(new BigDecimal(matcher.group(1)), "px".equals(matcher.group(2)) ? Unit.PX : Unit.REM);
    }

    /**
     * @param factor
     *            the ratio, e.g. {@code 0.75}
     * @return this length multiplied by the factor, rounded to 4 fraction digits
     */
    public CssLength scaled(String factor)
    {
        return new CssLength(this.value.multiply(new BigDecimal(factor))
                                       .setScale(SCALE, RoundingMode.HALF_UP),
                             this.unit);
    }

    /**
     * @return the CSS text, e.g. {@code 12px} or {@code 0.375rem}
     */
    public String toCss()
    {
        BigDecimal stripped = this.value.stripTrailingZeros();
        return (stripped.signum() == 0 ? "0" : stripped.toPlainString()) + this.unit.getSuffix();
    }
}
