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
