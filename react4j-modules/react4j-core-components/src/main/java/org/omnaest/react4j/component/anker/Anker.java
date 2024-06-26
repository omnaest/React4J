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
}
