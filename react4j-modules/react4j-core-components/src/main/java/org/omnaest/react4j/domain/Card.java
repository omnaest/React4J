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

import java.util.function.Consumer;

import org.omnaest.react4j.domain.context.document.Document.Field;
import org.omnaest.react4j.domain.support.UIComponentWithContent;

public interface Card extends UIComponentWithContent<Card>
{
    public Card withTitle(String title);

    public Card withTitle(Field field);

    public Card withLinkLocator(String locator);

    public Card withAdjustment(boolean value);

    public Card withImage(Consumer<Image> imageConsumer);

    public Card withSubTitle(String subTitle);

}
