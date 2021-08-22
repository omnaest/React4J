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

public interface SizedContainer extends UIComponentWithContent<SizedContainer>
{
    public SizedContainer withFullWidth();

    public SizedContainer withFullHeight();

    public SizedContainer withWidthRatio(double ratio);

    public SizedContainer withHeightRatio(double ratio);

    public SizedContainer withWidthInPixel(int pixels);

    public SizedContainer withHeightInPixel(int pixels);

    SizedContainer withHeightInViewPortRatio(double ratio);

    SizedContainer withWidthInViewPortRatio(double ratio);
}
