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

import java.util.List;

public interface BlockQuote extends UIComponent<BlockQuote>
{
    public BlockQuote addText(String text);

    /**
     * Adds a component to the quote, rendered after the texts and before the footer. It carries what plain texts cannot, like paragraphs with links and
     * emphasis, lists or a nested quote. A quote without any component renders exactly as before this method existed.
     *
     * @param component
     * @return this
     */
    public BlockQuote addComponent(UIComponent<?> component);

    /**
     * Adds every given component in order, see {@link #addComponent(UIComponent)}. A {@code null} list is ignored.
     *
     * @param components
     * @return this
     */
    public BlockQuote addComponents(List<? extends UIComponent<?>> components);

    /**
     * Declares the footer, e.g. the source of the quote. The footer element is only rendered if a footer was set, since an empty one makes Bootstrap print a
     * lone dash.
     *
     * @param footer
     * @return this
     */
    public BlockQuote withFooter(String footer);
}
