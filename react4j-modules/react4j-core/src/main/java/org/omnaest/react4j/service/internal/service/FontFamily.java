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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;

/**
 * A CSS font-family list from a deliberately small grammar: comma separated entries, each either a bare name (words of letters, digits and hyphens
 * separated by single spaces) or a name in matching single or double quotes (letters, digits, hyphens and spaces inside). Anything else, in particular
 * a backslash, a semicolon, a brace, an angle bracket, a slash, a parenthesis or an unbalanced quote, is rejected, so the text can neither end the
 * declaration nor the rule nor the style element it is written into.
 *
 * @author omnaest
 */
@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class FontFamily
{
    private static final int     MAX_LENGTH  = 300;
    private static final int     MAX_ENTRIES = 20;

    private static final Pattern CHARACTERS  = Pattern.compile("[A-Za-z0-9 ,'\"-]*");
    private static final Pattern ENTRY       = Pattern.compile("[A-Za-z0-9-]+(?: [A-Za-z0-9-]+)*|\"[A-Za-z0-9 -]+\"|'[A-Za-z0-9 -]+'");

    String                       css;

    /**
     * @param text
     *            e.g. {@code Inter, "Segoe UI", system-ui, sans-serif}
     * @return the normalised list, entries separated by a comma and a space
     * @throws IllegalArgumentException
     *             for anything outside the grammar, including null, blank, and lists beyond 300 characters or 20 entries
     */
    public static FontFamily parse(String text)
    {
        if (text == null || text.isBlank() || text.length() > MAX_LENGTH)
        {
            throw new IllegalArgumentException("A font family must be a non blank list of at most " + MAX_LENGTH + " characters");
        }
        // checked on the whole text first: String.trim() would silently drop control characters such as a NUL at the edge of an entry
        if (!CHARACTERS.matcher(text)
                       .matches())
        {
            throw new IllegalArgumentException("A font family may only contain letters, digits, spaces, commas, hyphens and quotes");
        }
        String[] entries = text.split(",", -1);
        if (entries.length > MAX_ENTRIES)
        {
            throw new IllegalArgumentException("A font family list holds at most " + MAX_ENTRIES + " entries");
        }
        List<String> normalised = new ArrayList<>();
        for (String entry : entries)
        {
            String trimmed = entry.trim();
            if (!ENTRY.matcher(trimmed)
                      .matches())
            {
                throw new IllegalArgumentException("A font family entry must be a name of letters, digits, hyphens and single spaces, optionally in quotes");
            }
            normalised.add(trimmed);
        }
        return new FontFamily(String.join(", ", normalised));
    }
}
