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

import lombok.Getter;
import lombok.RequiredArgsConstructor;

public interface SVGContainer extends UIComponent<SVGContainer>
{
    public SVGContainer withCSS(Consumer<CSSBuilder> builder);

    public SVGContainer withAlignment(AlignmentProvider alignment);

    public SVGContainer withSvg(String svg);

    public static interface AlignmentProvider
    {
        String getHeight();

        String getWidth();
    }

    @Getter
    @RequiredArgsConstructor
    public static enum Alignment implements AlignmentProvider
    {
        HALF_SCREEN_HEIGHT_FULL_WIDTH("50%", "100%"), FULL_SCREEN_HEIGHT_FULL_WIDTH("50%", "100%"), HALF_SCREEN_HEIGHT_HALF_WIDTH("50%", "100%");

        private final String height;
        private final String width;
    }

    public static interface CSSBuilder
    {
        CSSBuilder width(String width);

        CSSBuilder height(String height);
    }
}
