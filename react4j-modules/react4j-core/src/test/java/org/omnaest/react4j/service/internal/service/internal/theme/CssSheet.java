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
import java.util.Optional;

import lombok.Value;

/**
 * A small, tolerant CSS reader for the tests of the design token stylesheet: it flattens a stylesheet (the compiled theme, or the block emitted by the
 * renderer) into declarations with their selector and enclosing at-rules. It understands comments, quoted strings, parentheses (data URIs), nested
 * blocks and {@code !important}; it does not interpret anything.
 *
 * @author omnaest
 */
final class CssSheet
{
    @Value
    static class Declaration
    {
        /**
         * The enclosing at-rules, e.g. {@code @media (min-width:1200px)}, empty for a top level rule
         */
        String  atRules;
        /**
         * The selector with whitespace normalised and no space around commas
         */
        String  selector;
        String  property;
        String  value;
        boolean important;

        String key()
        {
            return this.atRules + "|" + this.selector + "|" + this.property;
        }
    }

    private final String            css;
    private int                     position;
    private final List<Declaration> declarations = new ArrayList<>();

    private CssSheet(String css)
    {
        this.css = css;
    }

    static List<Declaration> parse(String css)
    {
        // the compiled stylesheet starts with a byte order mark, which is not whitespace
        CssSheet sheet = new CssSheet(css.isEmpty() || css.charAt(0) != (char) 0xFEFF ? css : css.substring(1));
        sheet.parseBlock("", "", true);
        return sheet.declarations;
    }

    /**
     * Reads items up to the closing brace of the current block (or the end of the text at top level)
     */
    private void parseBlock(String atRules, String selector, boolean topLevel)
    {
        while (this.position < this.css.length())
        {
            this.skipWhitespaceAndComments();
            if (this.position >= this.css.length())
            {
                return;
            }
            if (this.css.charAt(this.position) == '}')
            {
                this.position++;
                if (!topLevel)
                {
                    return;
                }
                continue;
            }
            int start = this.position;
            char terminator = this.scanToTerminator();
            String text = this.css.substring(start, this.position)
                                  .trim();
            if (terminator == '{')
            {
                this.position++;
                String nestedAtRules = text.startsWith("@") ? atRules + normalise(text) + " :: " : atRules;
                String nestedSelector = text.startsWith("@") ? selector : normaliseSelector(text);
                this.parseBlock(nestedAtRules, nestedSelector, false);
            }
            else
            {
                if (terminator == ';')
                {
                    this.position++;
                }
                if (!topLevel)
                {
                    this.addDeclaration(atRules, selector, text);
                }
            }
        }
    }

    /**
     * Advances to the next {@code {}, {@code ;} or {@code }} outside quotes and parentheses and returns it (0 at the end of the text)
     */
    private char scanToTerminator()
    {
        int depth = 0;
        char quote = 0;
        while (this.position < this.css.length())
        {
            char c = this.css.charAt(this.position);
            if (quote != 0)
            {
                if (c == '\\')
                {
                    this.position++;
                }
                else if (c == quote)
                {
                    quote = 0;
                }
            }
            else if (c == '"' || c == '\'')
            {
                quote = c;
            }
            else if (c == '(')
            {
                depth++;
            }
            else if (c == ')')
            {
                depth--;
            }
            else if (depth <= 0 && (c == '{' || c == ';' || c == '}'))
            {
                return c;
            }
            this.position++;
        }
        return 0;
    }

    private void skipWhitespaceAndComments()
    {
        while (this.position < this.css.length())
        {
            char c = this.css.charAt(this.position);
            if (Character.isWhitespace(c))
            {
                this.position++;
            }
            else if (this.css.startsWith("/*", this.position))
            {
                int end = this.css.indexOf("*/", this.position + 2);
                this.position = end < 0 ? this.css.length() : end + 2;
            }
            else
            {
                return;
            }
        }
    }

    private void addDeclaration(String atRules, String selector, String text)
    {
        int colon = text.indexOf(':');
        if (colon <= 0)
        {
            return;
        }
        String property = text.substring(0, colon)
                              .trim();
        String value = text.substring(colon + 1)
                           .trim();
        boolean important = false;
        Optional<String> withoutImportant = stripImportant(value);
        if (withoutImportant.isPresent())
        {
            important = true;
            value = withoutImportant.get();
        }
        this.declarations.add(new Declaration(atRules, selector, property, normalise(value), important));
    }

    private static Optional<String> stripImportant(String value)
    {
        String lower = value.toLowerCase();
        int index = lower.lastIndexOf("!important");
        if (index >= 0 && lower.substring(index + "!important".length())
                               .isBlank())
        {
            return Optional.of(value.substring(0, index)
                                    .trim());
        }
        return Optional.empty();
    }

    static String normalise(String text)
    {
        return text.replaceAll("\\s+", " ")
                   .trim();
    }

    static String normaliseSelector(String selector)
    {
        return normalise(selector).replaceAll("\\s*,\\s*", ",");
    }
}
