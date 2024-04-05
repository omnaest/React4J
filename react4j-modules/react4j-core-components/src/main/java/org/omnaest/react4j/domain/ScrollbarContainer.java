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

import org.omnaest.react4j.domain.support.UIComponentWithContent;

public interface ScrollbarContainer extends UIComponentWithContent<ScrollbarContainer>
{
    public ScrollbarContainer withVerticalBox(VerticalBoxMode verticalBoxMode);

    public ScrollbarContainer withHorizontalBox(HorizontalBoxMode horizontalBoxMode);

    public static enum VerticalBoxMode
    {
        FULL_VIEWPORT_HEIGHT, HALF_VIEWPORT_HEIGHT, FULL_VIEWPORT_HEIGHT_WITHOUT_HEADER, FULL_PARENT_HEIGHT, DEFAULT_HEIGHT
    }

    public static enum HorizontalBoxMode
    {
        FULL_VIEWPORT_WIDTH, FULL_PARENT_WIDTH, DEFAULT_WIDTH, FULL_CONTENT_WIDTH
    }
}
