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

public interface IFrame extends UIComponent<IFrame>
{
    /**
     * Opens a link in a new blank page
     * 
     * @see #whichOpensOnSamePage()
     * @param link
     * @return
     */
    public IFrame withSourceLink(String link);

    /**
     * Title attribute of an {@link IFrame} which creates the tooltip
     * 
     * @param title
     * @return
     */
    public IFrame withTitle(String title);

    /**
     * @see #withTitle(String)
     * @param title
     * @return
     */
    public IFrame withNonTranslatedTitle(String title);

    /**
     * Allows embedded pages to go into full screen mode
     * 
     * @return
     */
    public IFrame allowFullScreen();

    /**
     * @see #allowFullScreen()
     * @param value
     * @return
     */
    public IFrame allowFullScreen(boolean value);
}
