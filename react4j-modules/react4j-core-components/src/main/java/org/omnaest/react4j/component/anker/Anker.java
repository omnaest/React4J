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
package org.omnaest.react4j.component.anker;

import java.util.List;

import org.omnaest.react4j.domain.UIComponent;

public interface Anker extends UIComponent<Anker>
{
    public Anker withText(String text);

    public Anker withNonTranslatedText(String text);

    /**
     * Opens a link in a new blank page
     * 
     * @see #whichOpensOnSamePage()
     * @param link
     * @return
     */
    public Anker withLink(String link);

    /**
     * Enables the target of the link to be on the same page. Default is a new blank page.
     * 
     * @return
     */
    public Anker whichOpensOnSamePage();

    /**
     * Creates a link to an in page anker like
     * 
     * <pre>
     * &lta href="#element"/&gt
     * </pre>
     * 
     * @param locator
     * @return
     */
    public Anker withLocator(String locator);

    /**
     * Title attribute of an {@link Anker} which creates the tooltip
     * 
     * @param title
     * @return
     */
    public Anker withTitle(String title);

    /**
     * @see #withTitle(String)
     * @param title
     * @return
     */
    public Anker withNonTranslatedTitle(String title);

    /**
     * Adds a component to the label of the anchor, rendered after the text (if one was set) and after the components added before. It carries what a plain
     * text label cannot, like emphasis, inline code or an image. An anker without any component renders exactly as before this method existed.
     * <p>
     * Only phrasing content belongs here (text, emphasis, inline code, an image, a line break). Do not nest interactive components, like another link or a
     * button, inside an anchor: that is invalid HTML and browsers handle it inconsistently. React4J does not check this.
     *
     * @param component
     *            the component to add, {@code null} is ignored
     * @return this
     */
    public Anker addComponent(UIComponent<?> component);

    /**
     * Adds every given component in order, see {@link #addComponent(UIComponent)} (which also tells what may be added). A {@code null} list and a
     * {@code null} member are ignored.
     *
     * @param components
     * @return this
     */
    public Anker addComponents(List<? extends UIComponent<?>> components);
}
