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
package org.omnaest.react4j.domain;

import org.omnaest.react4j.domain.i18n.I18nText;

public interface Text extends UIComponent<Text>
{
    public Text addText(String text);

    public Text addNonTranslatedText(String text);

    public Text addText(I18nText text);

    /**
     * Declares a style for the texts. Without a style (the default, also after {@code withStyle(null)}) the text renders no element of its own, exactly as
     * before this method existed. With a style the client and the static HTML renderer wrap the texts in one {@code span} that carries the theme class of the
     * {@link Style}.
     *
     * @param style
     * @return this
     */
    public Text withStyle(Style style);

    /**
     * Declares inline emphasis for the texts, e.g. {@code withEmphasis(Emphasis.BOLD, Emphasis.ITALIC)}. The call is cumulative (a second call adds to the
     * first, none removes) and idempotent per member. Without a call (the default) the text carries no emphasis and renders exactly as before this method
     * existed. With emphasis the client and the static HTML renderer wrap the texts in the semantic elements of the {@link Emphasis} members, see
     * {@link Emphasis#toElementName()}, nested in the declaration order of the enum with the first member outermost, and inside the span of a {@link Style}
     * if one is set. A {@code null} array or member is ignored.
     *
     * @param emphasis
     * @return this
     */
    public Text withEmphasis(Emphasis... emphasis);

    /**
     * The inline emphasis a {@link Text} can be given. Every member maps to a semantic HTML element every stylesheet React4J serves styles by itself, so
     * unlike a theme class it cannot be missing from one of them, see {@link #toElementName()}.
     */
    public static enum Emphasis
    {
        BOLD, ITALIC, STRIKETHROUGH;

        /**
         * The HTML element the emphasised texts are wrapped with. The one place the {@link Emphasis} to element mapping lives, and total: every member answers.
         * The client renderer uses the same elements for the same node values.
         *
         * @return the element name without brackets, never {@code null}
         */
        public String toElementName()
        {
            switch (this)
            {
                case BOLD :
                    return "strong";
                case ITALIC :
                    return "em";
                case STRIKETHROUGH :
                    return "del";
                default :
                    throw new IllegalStateException("No element mapped for the text emphasis " + this);
            }
        }
    }

    /**
     * The styles a {@link Text} can be given. Every member maps to a theme utility class that is defined in each stylesheet React4J serves (modern, stock
     * Bootstrap and Tabler), see {@link #toCssClass()}.
     */
    public static enum Style
    {
        /**
         * De-emphasised text, e.g. a line of metadata beside the main content
         */
        MUTED;

        /**
         * The theme utility class a styled text is wrapped with. The one place the {@link Style} to class mapping lives, and total: every member answers.
         * The client renderer uses the same classes for the same node values.
         *
         * @return the CSS class name, never {@code null}
         */
        public String toCssClass()
        {
            switch (this)
            {
                case MUTED :
                    return "text-body-secondary";
                default :
                    throw new IllegalStateException("No theme class mapped for the text style " + this);
            }
        }
    }
}
