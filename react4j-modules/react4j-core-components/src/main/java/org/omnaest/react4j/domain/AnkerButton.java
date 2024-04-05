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

import org.omnaest.react4j.domain.Button.Style;

public interface AnkerButton extends UIComponent<AnkerButton>
{
    public AnkerButton withText(String text);

    public AnkerButton withLink(String link);

    public AnkerButton withStyle(Style style);

    /**
     * Enables the target of the link to be on the same page. Default is a new blank page.
     * 
     * @return
     */
    public AnkerButton whichOpensOnSamePage();

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
    public AnkerButton withLocator(String locator);
}
